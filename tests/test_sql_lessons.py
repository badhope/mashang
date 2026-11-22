"""SQL 课程包真执行门禁。

每一讲的参考答案都送进 `app/src/main/python/sql_runner.py` 的判题函数，
用真实 sqlite3 执行并逐条跑判题用例；任何一讲失败即视为内容与引擎漂移。
"""

import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "app" / "src" / "main" / "python"))

import sql_runner  # noqa: E402


class SqlPackTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pack_paths = sorted((ROOT / "content_packs").glob("sql-*.json"))
        cls.lessons = []
        for path in cls.pack_paths:
            cls.lessons.extend(json.loads(path.read_text(encoding="utf-8")))

    def test_pack_exists_and_has_lessons(self):
        self.assertEqual(
            ["sql-advanced.json", "sql-basics.json", "sql-core.json", "sql-edge.json", "sql-expert.json"],
            [p.name for p in self.pack_paths],
            "SQL 课程应为 基础/核心/进阶/专业/边界 五包",
        )
        self.assertEqual(43, len(self.lessons), "SQL 课程合计 43 讲（38 主线 + 5 边界）")
        self.assertTrue(all(l["lang"] == "sql" for l in self.lessons))

    def test_every_solution_passes_its_tests(self):
        for lesson in self.lessons:
            ex = lesson["exercise"]
            with self.subTest(lesson=lesson["id"]):
                result = sql_runner.check_sql(ex["solution"], ex["tests"])
                self.assertTrue(
                    result["passed"],
                    f'{lesson["id"]} 未通过：{result["error"]}',
                )

    def test_every_runnable_block_executes(self):
        for lesson in self.lessons:
            for index, block in enumerate(lesson["blocks"], start=1):
                if block.get("type") != "code" or not block.get("runnable"):
                    continue
                with self.subTest(lesson=lesson["id"], block=index):
                    result = sql_runner.run_sql(block["code"])
                    self.assertTrue(result["ok"], f'{lesson["id"]}#{index}: {result["error"]}')

    def test_sandbox_denies_attach(self):
        result = sql_runner.run_sql("ATTACH DATABASE '/tmp/evil.db' AS evil;")
        self.assertFalse(result["ok"])
        self.assertEqual("DatabaseError", result["error"]["type"])

    def test_each_run_starts_from_a_clean_database(self):
        first = sql_runner.run_sql("CREATE TABLE t(x INTEGER); INSERT INTO t VALUES (1);")
        self.assertTrue(first["ok"])
        second = sql_runner.run_sql("SELECT * FROM t;")
        self.assertFalse(second["ok"], "第二次运行应看到全新的空库")

    def test_null_and_grouping_semantics(self):
        sql = (
            "CREATE TABLE r(h TEXT, p INTEGER);"
            "INSERT INTO r VALUES ('a',1),('a',NULL),('b',3);"
        )
        result = sql_runner.check_sql(
            sql + "SELECT h, COALESCE(SUM(p),0) AS total FROM r GROUP BY h ORDER BY h;",
            [
                "assert columns == ['h','total'], columns",
                "assert rows == [['a',1],['b',3]], rows",
            ],
        )
        self.assertTrue(result["passed"], result["error"])

    def test_test_failure_reports_first_reason(self):
        result = sql_runner.check_sql("SELECT 1 AS a;", ["assert rows == [[2]], '应为 2'"])
        self.assertFalse(result["passed"])
        self.assertEqual("TestFailed", result["error"]["type"])
        self.assertIn("应为 2", result["error"]["message"])


if __name__ == "__main__":
    unittest.main(verbosity=2)


class SqlChallengeTests(unittest.TestCase):
    """挑战页 SQL 题也要真实判题，不能只验证课程 exercise。"""

    def test_all_sql_challenges_pass(self):
        challenges = json.loads(
            (ROOT / "app" / "src" / "main" / "assets" / "challenges.json").read_text(encoding="utf-8")
        )
        sql_challenges = [c for c in challenges if c.get("lang") == "sql"]
        self.assertGreaterEqual(len(sql_challenges), 2, "至少两道 SQL 挑战")
        for c in sql_challenges:
            with self.subTest(challenge=c["id"]):
                result = sql_runner.check_sql(c["solution"], c["tests"])
                self.assertTrue(result["passed"], f'{c["id"]} 未通过：{result["error"]}')
                starter = sql_runner.check_sql(c["starterCode"], c["tests"])
                self.assertFalse(starter["passed"], f'{c["id"]} 起始代码不应通过')


class SqlTimeoutTests(unittest.TestCase):
    """SQL 必须有执行超时：死循环 CTE 不能把线程永久占住（2026-11 发布前审计）。"""

    def test_infinite_cte_is_interrupted(self):
        import time
        sql = (
            "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq) "
            "SELECT COUNT(*) AS c FROM seq;"
        )
        started = time.time()
        result = sql_runner.run_sql(sql, timeout=0.5)
        elapsed = time.time() - started
        self.assertFalse(result["ok"], "死循环查询必须失败")
        self.assertEqual("Timeout", result["error"]["type"])
        self.assertLess(elapsed, 5.0, f"超时未生效，耗时 {elapsed:.1f}s")

    def test_check_sql_also_times_out(self):
        sql = (
            "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq) "
            "SELECT COUNT(*) AS c FROM seq;"
        )
        result = sql_runner.check_sql(sql, ["assert True"], timeout=0.5)
        self.assertFalse(result["passed"])
        self.assertEqual("Timeout", result["error"]["type"])

    def test_normal_query_is_unaffected(self):
        result = sql_runner.run_sql("SELECT 1 AS a;", timeout=2.0)
        self.assertTrue(result["ok"])

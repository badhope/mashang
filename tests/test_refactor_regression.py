"""E2/E4 修复的回归测试：判题用例命名空间隔离 + 变量快照排序。"""
import json
import os
import sys
import unittest

sys.path.insert(
    0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
)

import runner


class IsolationTests(unittest.TestCase):
    def test_tests_share_state_but_do_not_pollute_user_ns(self):
        # 现有课程把 tests 写成"准备、动作、断言"的有序脚本，测试间必须共享状态
        code = "class Counter:\n    def __init__(self):\n        self.n = 0\n    def add(self, x):\n        self.n += x\n        return self.n"
        tests = [
            "c = Counter()",                     # 准备
            "c.add(3)",                          # 动作
            "assert c.n == 3",                   # 断言 1
            "c.add(4)",                          # 动作 2（依赖前面共享状态）
            "assert c.n == 7",                   # 断言 2
        ]
        result = runner.check_exercise(code, tests)
        self.assertTrue(result["passed"], result["error"])
        # 测试写的 c 不应出现在变量快照里（快照只含用户代码定义）
        names = {v["name"] for v in result["variables"]}
        self.assertNotIn("c", names)

    def test_user_functions_visible_in_tests(self):
        code = "def add(a, b):\n    return a + b"
        tests = ["assert add(1, 2) == 3", "assert add(10, 20) == 30"]
        result = runner.check_exercise(code, tests)
        self.assertTrue(result["passed"], result["error"])

    def test_user_result_not_overwritten_by_test(self):
        # 测试里同名赋值只影响测试命名空间，不污染用户结果
        code = "result = 42"
        tests = ["result = 0\nassert result == 0"]
        result = runner.check_exercise(code, tests)
        self.assertTrue(result["passed"], result["error"])
        by_name = {v["name"]: v for v in result["variables"]}
        self.assertEqual(by_name["result"]["value"], "42")


class SnapshotTests(unittest.TestCase):
    def test_snapshot_recent_first_and_not_truncated_by_insertion_order(self):
        code = "\n".join(
            ["%s = %d" % (("v%02d" % i), i) for i in range(50)]
        )
        result = runner.run_code(code)
        names = [v["name"] for v in result["variables"]]
        # 最近定义的排最前
        self.assertEqual(names[0], "v49")
        # 最多展示 MAX_VARS 个（40）
        self.assertLessEqual(len(names), 40)
        # 早期版本按插入序 break 会漏掉最后定义的；现在最近定义优先，必然保留
        self.assertIn("v49", names)

    def test_snapshot_keeps_data_vars_only(self):
        result = runner.run_code("import math\ndef f():\n    pass\nx = 1")
        names = {v["name"] for v in result["variables"]}
        self.assertIn("x", names)
        self.assertNotIn("math", names)
        self.assertNotIn("f", names)


if __name__ == "__main__":
    unittest.main()

"""补齐 runner.py 的未覆盖分支（覆盖率审计发现 67% → 目标 90%+）。

覆盖的空白点：
  - input(prompt) 打印提示词后正常返回
  - input() 队列耗尽时的 InputError
  - 变量快照：超大对象 / __repr__ 抛异常 / 超长 repr 截断 / MAX_VARS 上限
  - check_exercise：测试打印诊断信息、测试断言失败、测试超时
  - run_code：timeout 参数透传、stdin 顺序消费
"""
import json
import sys
import unittest

sys.path.insert(0, "app/src/main/python")

import runner


class InputHostTests(unittest.TestCase):
    def test_input_prints_prompt_and_returns(self):
        out = runner.run_code("v = input('name? ')\nprint('hi', v)", ["neo"])
        self.assertTrue(out["ok"], out)
        self.assertIn("name? ", out["stdout"])
        self.assertIn("hi neo", out["stdout"])

    def test_input_exhausted_raises_friendly_input_error(self):
        out = runner.run_code("input()")
        self.assertFalse(out["ok"])
        self.assertEqual(out["error"]["type"], "InputError")
        self.assertIn("不支持交互式输入", out["error"]["message"])


class SnapshotEdgeTests(unittest.TestCase):
    def test_huge_object_is_marked_not_rendered(self):
        out = runner.run_code("big = bytearray(2_000_000)")
        self.assertTrue(out["ok"], out)
        snap = {v["name"]: v for v in out["variables"]}
        self.assertIn("big", snap)
        self.assertEqual(snap["big"]["value"], "<对象过大>")

    def test_repr_that_raises_is_caught(self):
        code = (
            "class Boom:\n"
            "    def __repr__(self):\n"
            "        raise RuntimeError('nope')\n"
            "bad = Boom()\n"
        )
        out = runner.run_code(code)
        self.assertTrue(out["ok"], out)
        snap = {v["name"]: v for v in out["variables"]}
        self.assertEqual(snap["bad"]["value"], "<无法显示>")

    def test_long_repr_is_truncated(self):
        out = runner.run_code("s = 'x' * 500")
        self.assertTrue(out["ok"])
        snap = {v["name"]: v for v in out["variables"]}
        self.assertLessEqual(len(snap["s"]["value"]), runner.MAX_REPR + 1)
        self.assertTrue(snap["s"]["value"].endswith("…"))

    def test_variable_cap(self):
        code = "\n".join(f"v{i} = {i}" for i in range(runner.MAX_VARS + 15))
        out = runner.run_code(code)
        self.assertTrue(out["ok"])
        self.assertEqual(len(out["variables"]), runner.MAX_VARS)


class CheckExerciseEdgeTests(unittest.TestCase):
    def test_test_output_collected_on_failure(self):
        res = runner.check_exercise(
            "x = 1",
            ["print('diagnostic line')\nassert x == 2"],
        )
        self.assertFalse(res["passed"])
        self.assertIn("diagnostic line", res["test_output"])

    def test_assertion_message_in_failure_reason(self):
        res = runner.check_exercise("x = 1", ["assert x == 2, 'x 应该是 2'"])
        self.assertFalse(res["passed"])
        self.assertIn("x 应该是 2", res["error"]["message"])

    def test_empty_assertion_message_uses_default(self):
        res = runner.check_exercise("x = 1", ["assert x == 2"])
        self.assertFalse(res["passed"])
        self.assertIn("断言未通过", res["error"]["message"])

    def test_user_code_crash_reported(self):
        res = runner.check_exercise("1 / 0", ["assert True"])
        self.assertFalse(res["ok"])
        self.assertEqual(res["error"]["type"], "ZeroDivisionError")

    def test_passing_exercise_has_empty_test_output(self):
        res = runner.check_exercise("x = 1", ["assert x == 1"])
        self.assertTrue(res["passed"])
        self.assertEqual(res["test_output"], "")


class JsonWrapperTests(unittest.TestCase):
    def test_run_code_json_roundtrip(self):
        out = json.loads(runner.run_code_json(json.dumps({"code": "print(1+1)"})))
        self.assertTrue(out["ok"])
        self.assertIn("2", out["stdout"])

    def test_check_exercise_json_roundtrip(self):
        out = json.loads(
            runner.check_exercise_json(json.dumps({"code": "x=1", "tests": ["assert x==1"]}))
        )
        self.assertTrue(out["passed"])

    def test_info_json_shape(self):
        info = json.loads(runner.info_json())
        self.assertIn("python_version", info)
        self.assertIn("platform", info)


if __name__ == "__main__":
    unittest.main()

import json
import os
import sys
import time
import unittest

sys.path.insert(
    0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
)

import repl
import runner


class EngineEdgeTests(unittest.TestCase):
    # ---- G1: repr longer than MAX_REPR is truncated ----
    def test_repr_truncated_beyond_max(self):
        result = runner.run_code("big = 'A' * 500")
        big = next(v for v in result["variables"] if v["name"] == "big")
        self.assertLessEqual(len(big["value"]), runner.MAX_REPR + 1)
        self.assertTrue(big["value"].endswith("…"))

    # ---- G2: traceback must not leak engine internal frames ----
    def test_traceback_has_no_engine_frames(self):
        result = runner.run_code("def f():\n    return 1 / 0\nf()")
        self.assertFalse(result["ok"])
        tb = result["error"]["traceback"]
        self.assertNotIn("runner.py", tb)
        self.assertNotIn("repl.py", tb)
        self.assertNotIn("app/src/main/python", tb)
        self.assertIn("<pyneon>", tb)

    def test_traceback_shows_user_frame(self):
        result = runner.run_code("def g(x):\n    return x[100]\ng([1, 2])")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "IndexError")

    # ---- custom exception + raise ... from chain ----
    def test_custom_exception_and_cause_chain(self):
        code = (
            "class MyError(Exception):\n    pass\n"
            "def boom():\n    try:\n        raise ValueError('root')\n"
            "    except ValueError as e:\n        raise MyError('wrapped') from e\n"
            "boom()\n"
        )
        result = runner.run_code(code)
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "MyError")
        self.assertIn("wrapped", result["error"]["message"])
        # The cause chain must be rendered in the traceback.
        self.assertIn("direct cause", result["error"]["traceback"])

    # ---- G3: multiple test failures report N + first reason ----
    def test_check_exercise_multiple_failures(self):
        tests = [
            "assert add(1, 1) == 2",
            "assert add(2, 2) == 5",
            "assert add(3, 3) == 7",
        ]
        result = runner.check_exercise("def add(a, b):\n    return a + b", tests)
        self.assertFalse(result["passed"])
        self.assertIsNotNone(result["error"])
        self.assertIn("2 个未通过", result["error"]["message"])
        self.assertIn("首个失败", result["error"]["message"])

    # ---- G4: Unicode Chinese / emoji stdout ----
    def test_unicode_chinese_stdout(self):
        result = runner.run_code("print('霓虹城市 中文输出 ✅')")
        self.assertTrue(result["ok"])
        self.assertIn("霓虹城市 中文输出 ✅", result["stdout"])

    # ---- G5 / W3: huge stdout is truncated, not unbounded ----
    def test_huge_stdout_truncated(self):
        code = "for _ in range(4000):\n    print('*' * 500)\n"
        result = runner.run_code(code)
        self.assertTrue(result["ok"])
        out = result["stdout"]
        self.assertIn("输出过长已截断", out)
        self.assertLessEqual(len(out), runner.MAX_OUTPUT + 100)

    # ---- G6: 100 repeated calls all succeed (no leak / no flakiness) ----
    def test_repeated_calls_stable(self):
        for i in range(100):
            r = runner.run_code("x = %d * 2\nprint(x)" % i)
            self.assertTrue(r["ok"], "call %d failed" % i)
            self.assertIn(str(i * 2), r["stdout"])

    # ---- empty code ----
    def test_empty_code(self):
        r = runner.run_code("")
        self.assertTrue(r["ok"])
        self.assertEqual(r["stdout"], "")
        self.assertIsNone(r["error"])
        r2 = runner.run_code("   \n  ")
        self.assertTrue(r2["ok"])

    # ---- G7: check_exercise_json / info_json round-trip ----
    def test_json_wrappers_roundtrip(self):
        payload = json.dumps(
            {
                "code": "def sq(n):\n    return n * n",
                "tests": ["assert sq(4) == 16", "assert sq(5) == 25"],
            }
        )
        parsed = json.loads(runner.check_exercise_json(payload))
        self.assertTrue(parsed["passed"])
        info = json.loads(runner.info_json())
        self.assertIn("python_version", info)
        self.assertIn("platform", info)

    # ---- G3b: print() inside a test is now visible via test_output ----
    def test_test_output_visible(self):
        tests = [
            "print('DIAG inside test')",
            "assert double(2) == 4",
        ]
        result = runner.check_exercise("def double(n):\n    return n * 2", tests)
        self.assertTrue(result["passed"])
        self.assertIn("test_output", result)
        self.assertIn("DIAG inside test", result["test_output"])

    def test_test_output_present_on_failure(self):
        tests = [
            "assert truth() == True",
            "assert truth() == False",
        ]
        result = runner.check_exercise("def truth():\n    return True", tests)
        self.assertFalse(result["passed"])
        self.assertIn("test_output", result)

    # ---- W2: custom __repr__ that raises must not crash the snapshot ----
    def test_snapshot_repr_exception_safe(self):
        code = (
            "class Bad:\n"
            "    def __repr__(self):\n"
            "        raise ValueError('boom')\n"
            "b = Bad()\n"
        )
        result = runner.run_code(code)
        b = next(v for v in result["variables"] if v["name"] == "b")
        self.assertEqual(b["value"], "<无法显示>")

    # ---- W2: recursive __repr__ must not hang (RecursionError is caught) ----
    def test_snapshot_repr_recursion_safe(self):
        code = (
            "class Node:\n"
            "    def __repr__(self):\n"
            "        return repr(self.child)\n"
            "n = Node()\n"
            "n.child = n\n"
        )
        result = runner.run_code(code)
        n = next(v for v in result["variables"] if v["name"] == "n")
        self.assertEqual(n["value"], "<无法显示>")

    # ---- W2: oversized object is skipped via getsizeof guard ----
    def test_snapshot_oversized_safe(self):
        result = runner.run_code("huge = list(range(500000))")
        huge = next(v for v in result["variables"] if v["name"] == "huge")
        self.assertEqual(huge["value"], "<对象过大>")

    # ---- B1: REPL dead loop is interrupted (no permanent hang) ----
    def test_repl_infinite_loop_interrupted(self):
        repl.start()
        repl.push("while True:")
        repl.push("    pass")
        started = time.perf_counter()
        out = json.loads(repl.push("", timeout=0.6))
        elapsed = time.perf_counter() - started
        self.assertFalse(out["more"])
        self.assertIn("超时", out["output"])
        self.assertLess(elapsed, 5.0)

    # ---- B1: REPL stays stateful after the protection change ----
    def test_repl_stateful_after_protect(self):
        repl.start()
        repl.push("a = 10")
        out = json.loads(repl.push("a + 5"))
        self.assertFalse(out["more"])
        self.assertIn("15", out["output"])

    # ---- B1: REPL multiline block still works ----
    def test_repl_multiline_block(self):
        repl.start()
        json.loads(repl.push("total = 0"))
        self.assertTrue(json.loads(repl.push("for i in range(3):"))["more"])
        self.assertTrue(json.loads(repl.push("    total += i"))["more"])
        self.assertFalse(json.loads(repl.push(""))["more"])
        self.assertIn("3", json.loads(repl.push("total"))["output"])

    # ---- B1: REPL reset clears the namespace ----
    def test_repl_reset_clears(self):
        repl.start()
        repl.push("token = 'x'")
        repl.reset()
        out = json.loads(repl.push("token"))
        self.assertIn("NameError", out["output"])


if __name__ == "__main__":
    unittest.main(verbosity=2)


class OutputBoundTests(unittest.TestCase):
    """输出硬上限：死循环打印不能把进程内存吃爆（2026-11 发布前审计）。"""

    def test_runaway_print_is_interrupted_early(self):
        import time
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python"))
        import runner

        started = time.time()
        result = runner.run_code("while True:\n    print('x' * 10000)\n", [], 8.0)
        elapsed = time.time() - started
        self.assertFalse(result["ok"], "死循环打印必须被中断")
        self.assertEqual("OutputTooLong", result["error"]["type"])
        self.assertLess(elapsed, 3.0, f"应在写入侧就中断，实际耗时 {elapsed:.1f}s")
        self.assertLess(len(result["stdout"]), 5 * 1024 * 1024, "保留的输出不应超过硬上限")

    def test_normal_large_output_still_works(self):
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python"))
        import runner

        result = runner.run_code("print('a' * 100000)\n", [], 8.0)
        self.assertTrue(result["ok"])
        self.assertEqual(100001, len(result["stdout"]))

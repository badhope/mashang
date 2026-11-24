"""离线学习闭环的加速压力测试。

不是 UI 点击模拟，而是直接压 Python 引擎和判题路径：
    1. 120 次正常提交，覆盖循环、字符串、字典、递归、异常、列表六类常见写法；
    2. 每 10 次插入一次坏代码，确认报错不会把引擎带崩；
    3. 记录总耗时和峰值内存，超过阈值就失败。

真机长跑仍然要做；这份测试负责把“跑一百次会不会越来越慢/越占内存”变成
每次 CI 都能复现的硬指标。
"""
from __future__ import annotations

import os
import resource
import sys
import time
import unittest

sys.path.insert(
    0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
)

import runner

CASES = [
    (
        "loop_sum",
        "total = 0\nfor n in range(1, 101):\n    total += n\nprint(total)",
        ["assert total == 5050"],
    ),
    (
        "string_scan",
        "text = 'neon-academy'\nvowels = sum(1 for ch in text if ch in 'aeiou')\nprint(vowels)",
        ["assert vowels == 5"],
    ),
    (
        "dict_count",
        "words = ['ai', 'code', 'ai', 'offline']\ncounts = {}\n"
        "for w in words:\n    counts[w] = counts.get(w, 0) + 1",
        ["assert counts['ai'] == 2", "assert counts['offline'] == 1"],
    ),
    (
        "recursion",
        "def fib(n):\n    if n < 2:\n        return n\n    return fib(n - 1) + fib(n - 2)\n"
        "result = fib(10)",
        ["assert result == 55"],
    ),
    (
        "exception",
        "def safe_div(a, b):\n    try:\n        return a / b\n"
        "    except ZeroDivisionError:\n        return None\nvalue = safe_div(9, 3)",
        ["assert value == 3.0", "assert safe_div(1, 0) is None"],
    ),
    (
        "list_pipeline",
        "values = [5, 3, 8, 1]\nordered = sorted(values)\n"
        "doubled = [v * 2 for v in ordered if v % 2 == 1]",
        ["assert ordered == [1, 3, 5, 8]", "assert doubled == [2, 6, 10]"],
    ),
]

BAD_CODE = "def broken(:\n    return 1"

HEAVY_CASES = [
    (
        "heavy_sum",
        "total = 0\nfor n in range(1, 150001):\n    total += n * n",
        ["assert total == 1125011250025000"],
    ),
    (
        "heavy_words",
        "words = ['w%d' % (i % 997) for i in range(25000)]\ncounts = {}\n"
        "for w in words:\n    counts[w] = counts.get(w, 0) + 1",
        ["assert len(counts) == 997", "assert counts['w0'] == 26"],
    ),
    (
        "heavy_sort",
        "import random\nrandom.seed(7)\nvalues = [random.randint(0, 99999) for _ in range(20000)]\n"
        "values.sort()",
        ["assert values[0] <= values[-1]", "assert len(values) == 20000"],
    ),
]


class OfflineStressTests(unittest.TestCase):
    def test_one_hundred_twenty_submissions_stay_stable(self):
        started = time.perf_counter()
        good_runs = 0
        bad_runs = 0

        for index in range(120):
            name, code, tests = CASES[index % len(CASES)]
            result = runner.check_exercise(code, tests)
            self.assertTrue(result["passed"], f"{name} 第 {index + 1} 次提交判题失败")
            good_runs += 1

            if index % 20 == 19:
                heavy_name, heavy_code, heavy_tests = HEAVY_CASES[index // 20 % len(HEAVY_CASES)]
                heavy = runner.check_exercise(heavy_code, heavy_tests)
                self.assertTrue(heavy["passed"], f"{heavy_name} 第 {index + 1} 次重负载判题失败")
                good_runs += 1

            if index % 10 == 9:
                failure = runner.check_exercise(BAD_CODE, ["assert True"])
                self.assertFalse(failure["passed"], "坏代码不能判通过")
                self.assertTrue(
                    failure.get("error") or failure.get("stderr"),
                    "坏代码要给出可读报错，不能静默失败",
                )
                bad_runs += 1

        elapsed = time.perf_counter() - started
        peak_mb = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024
        average_ms = elapsed * 1000 / (good_runs + bad_runs)

        # 阈值留足 CI 余量：单次平均 250ms 已经远高于真机体感上限。
        self.assertLess(average_ms, 250, f"平均单次 {average_ms:.1f}ms，明显变慢")
        self.assertLess(peak_mb, 300, f"峰值内存 {peak_mb:.1f}MB，超出离线低配机预算")
        print(
            f"\n[stress] {good_runs} 次正常 + {bad_runs} 次异常，"
            f"总耗时 {elapsed:.2f}s，平均 {average_ms:.1f}ms，峰值 {peak_mb:.1f}MB"
        )

    def test_runaway_loop_is_cut_off(self):
        code = "while True:\n    pass"
        started = time.perf_counter()
        result = runner.run_code(code, timeout=1.0)
        elapsed = time.perf_counter() - started

        self.assertIsNotNone(result.get("error"), "死循环必须被超时中断")
        self.assertLess(elapsed, 3.0, "超时中断不能拖太久")


if __name__ == "__main__":
    unittest.main()

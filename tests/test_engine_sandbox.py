"""Python 运行台离线沙箱回归。

这里测的是操作级拦截，不是「把 socket/subprocess 的 import 封掉」：
课程里的 asyncio 本身就会 import socket/subprocess，封 import 会误伤正常课。
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python"))

import runner


def run(code: str) -> dict:
    return runner.run_code(code)


class PythonSandboxTest(unittest.TestCase):
    def test_socket_is_blocked(self):
        result = run("import socket\nsocket.socket()")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("离线沙箱", result["error"]["message"])

    def test_asyncio_still_works(self):
        # asyncio 事件循环会创建 AF_UNIX socketpair，不能把 socket 一刀切封死。
        result = run(
            "import asyncio\n"
            "async def main():\n"
            "    await asyncio.sleep(0.01)\n"
            "    print('async-ok')\n"
            "asyncio.run(main())"
        )
        self.assertTrue(result["ok"], result)
        self.assertIn("async-ok", result["stdout"])

    def test_subprocess_is_blocked(self):
        result = run('import subprocess\nsubprocess.run(["echo", "hi"])')
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("subprocess.Popen", result["error"]["message"])

    def test_os_system_is_blocked(self):
        result = run('import os\nos.system("echo hi")')
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("os.system", result["error"]["message"])

    def test_java_bridge_is_blocked(self):
        result = run("import java")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("import java", result["error"]["message"])

    def test_ctypes_is_blocked(self):
        result = run("import ctypes")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("import ctypes", result["error"]["message"])

    def test_guard_survives_trace_disable(self):
        # 即使关掉 trace，审计钩子仍在，网络照样出不去。
        result = run("import sys\nsys.settrace(None)\nimport socket\nsocket.socket()")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "PermissionError")
        self.assertIn("socket family", result["error"]["message"])

    def test_child_thread_inherits_guard(self):
        result = run(
            "import threading\n"
            "box = {}\n"
            "def f():\n"
            "    try:\n"
            "        import socket\n"
            "        socket.socket()\n"
            "        box['r'] = 'BYPASS'\n"
            "    except Exception as exc:\n"
            "        box['r'] = type(exc).__name__\n"
            "t = threading.Thread(target=f)\n"
            "t.start(); t.join()\n"
            "print(box.get('r'))"
        )
        self.assertTrue(result["ok"], result)
        self.assertIn("PermissionError", result["stdout"])

    def test_normal_code_and_os_path_still_work(self):
        result = run("import os\nprint(os.path.join('a', 'b'))")
        self.assertTrue(result["ok"], result)
        self.assertIn("a/b", result["stdout"])

    def test_guard_is_released_after_run(self):
        result = run("print('ok')")
        self.assertTrue(result["ok"], result)
        self.assertEqual(runner._guard_depths, {})

    def test_timeout_still_works(self):
        result = runner.run_code("while True:\n    pass", timeout=0.2)
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["type"], "Timeout")


if __name__ == "__main__":
    unittest.main()

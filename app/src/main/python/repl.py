import code as code_module
import io
import json
import sys
import threading
import time

from runner import DEFAULT_TIMEOUT, EXEC_LOCK, SandboxTimeout, _Watchdog, _truncate, sandbox_guard

# 单条 REPL 语句最多跑多久，和 runner.DEFAULT_TIMEOUT 一样，
# 桌面和 Android 行为保持一致。
REPL_TIMEOUT = DEFAULT_TIMEOUT

_console = None
# REPL 会话是进程内单例，跨 push 要保住状态。start/reset/push 都加锁，
# 免得 UI 和后台线程抢同一个 console。
# 复用 runner.EXEC_LOCK：REPL 也会改 sys.stdout/stderr，
# 必须和 run_code、check_exercise 互斥，不然终端和课程页一起跑会串输出。
_console_lock = EXEC_LOCK


class NeonConsole(code_module.InteractiveConsole):
    def __init__(self):
        super().__init__(locals={"__name__": "__neon_console__", "__doc__": None})
        self.capture = io.StringIO()
        # 每条语句的预算，push 时更新。
        self._timeout = REPL_TIMEOUT

    def write(self, data):
        self.capture.write(data)

    def runcode(self, code):
        # 每条语句都丢进 daemon 线程，用和 runner 一样的 settrace 看门狗。
        # 死循环或慢语句会被掐掉，不会把 UI 线程卡死。
        capture = self.capture
        state = {"finished": False}
        old_stdout = sys.stdout
        old_stderr = sys.stderr
        deadline = time.perf_counter() + self._timeout

        def target():
            try:
                sys.stdout = capture
                sys.stderr = capture
                sys.settrace(_Watchdog(deadline))
                try:
                    with sandbox_guard():
                        try:
                            exec(code, self.locals)
                        finally:
                            sys.settrace(None)
                finally:
                    sys.settrace(None)
            except SystemExit:
                raise
            except SandboxTimeout:
                self.write("运行超时：代码疑似死循环，已被强制中断。\n")
            except BaseException:
                self.showtraceback()
            finally:
                sys.stdout = old_stdout
                sys.stderr = old_stderr
                state["finished"] = True

        worker = threading.Thread(target=target, daemon=True)
        worker.start()
        # 等预算再加一点余量，让看门狗先触发。
        worker.join(self._timeout + 1.5)
        if not state["finished"]:
            self.write("运行超时：代码疑似死循环，已被强制中断。\n")


def _active_console() -> NeonConsole:
    global _console
    with _console_lock:
        if _console is None:
            _console = NeonConsole()
        return _console


def start():
    global _console
    with _console_lock:
        _console = NeonConsole()
    return json.dumps({"ok": True})


def reset():
    # 重建 InteractiveConsole 会清掉 locals，变量和函数都没了。
    # import 过的模块和改过的 builtins 不会回滚，CPython 就这样，教学场景能接受。
    return start()


def push(line, timeout=None):
    console = _active_console()
    with _console_lock:
        # 可以传单次预算，测试用；默认 REPL_TIMEOUT。
        console._timeout = REPL_TIMEOUT if timeout is None else float(timeout)
        console.capture.seek(0)
        console.capture.truncate(0)
        more = console.push(line)
        return json.dumps(
            {"more": bool(more), "output": _truncate(console.capture.getvalue())},
            ensure_ascii=False,
        )

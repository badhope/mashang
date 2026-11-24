import io
import json
import sys
import threading
import time
import traceback
import types
from contextlib import contextmanager

DEFAULT_TIMEOUT = 8.0
EXERCISE_TEST_TIMEOUT = 2.0
MAX_VARS = 40
MAX_REPR = 200
# 输出最多留 1MiB，死循环 print 也不会把 JSON 和内存撑爆。
MAX_OUTPUT = 1_048_576
# 调 repr 之前先看对象多大，太大就跳过，免得 repr 本身把内存吃光。
MAX_VAR_BYTES = 1_000_000

# 运行台沙箱。只拦「执行用户代码」这一段，不拦 App 自己的课程包下载和启动。
# 用审计钩子而不是封 import：课程里的 asyncio 会 import socket / subprocess,
# 封 import 会把正常课程一起打断；这里允许 import，等真正建 socket、起进程时再拒绝。
_BLOCKED_IMPORT_ROOTS = {"java", "jnius", "ctypes", "_ctypes", "chaquopy"}
_BLOCKED_AUDIT_EVENTS = {
    "os.system", "os.popen", "os.exec", "os.spawn", "os.posix_spawn",
    "os.fork", "os.forkpty", "os.kill", "os.killpg",
    "socket.connect", "socket.bind", "socket.getaddrinfo", "socket.gethostbyname",
    "socket.gethostbyaddr", "socket.sendto", "socket.send_fds",
}
_BLOCKED_AUDIT_PREFIXES = ("subprocess.", "ctypes.")
# 每个线程单独计数。超时线程卡在 C 调用里时，不能让整个 App 的联网也一起被锁死。
_guard_depths = {}
_SANDBOX_DENY = "运行台处于离线沙箱：不开放网络、系统命令、Java/ctypes 桥。"


def _deny(what):
    raise PermissionError("%s（%s）" % (_SANDBOX_DENY, what))


def _audit(event, args):
    if _guard_depths.get(threading.get_ident(), 0) <= 0:
        return
    if event == "import":
        root = str(args[0]).split(".", 1)[0] if args else ""
        if root in _BLOCKED_IMPORT_ROOTS:
            _deny("import %s" % root)
        return
    if event == "socket.__new__":
        # 只封 INET/INET6。asyncio 的事件循环会用 AF_UNIX socketpair，
        # 连这个也封，所有异步课程都会挂。
        family = args[1] if len(args) > 1 else None
        if family in (2, 10):
            _deny("socket family=%s" % family)
        return
    if event in _BLOCKED_AUDIT_EVENTS or event.startswith(_BLOCKED_AUDIT_PREFIXES):
        _deny(event)


try:
    sys.addaudithook(_audit)
except Exception:
    # 老解释器没有审计钩子时宁可少一层防护，也不能让 App 起不来。
    pass


@contextmanager
def sandbox_guard():
    """执行用户代码期间打开操作级沙箱；可重入，按线程隔离。"""
    tid = threading.get_ident()
    _guard_depths[tid] = _guard_depths.get(tid, 0) + 1
    try:
        yield
    finally:
        depth = _guard_depths.get(tid, 1) - 1
        if depth <= 0:
            _guard_depths.pop(tid, None)
        else:
            _guard_depths[tid] = depth


# 用户代码 new 出来的线程默认不继承 guard。包一层 Thread.start，
# 让 threading / ThreadPoolExecutor 的子线程也留在沙箱里。
_original_thread_start = threading.Thread.start


def _guarded_thread_start(self):
    if _guard_depths.get(threading.get_ident(), 0) > 0:
        original_run = self.run

        def guarded_run():
            with sandbox_guard():
                original_run()

        self.run = guarded_run
    return _original_thread_start(self)


threading.Thread.start = _guarded_thread_start


# 执行锁。一次运行会改 sys.stdout、sys.stderr 和 builtins.input 这些进程级对象，
# 两个线程一起跑就会互相抢缓冲区，A 的输出跑进 B 里。
# run_code、check_exercise 和 repl.push 都用这一把，repl.py 也导它，不另起炉灶。
# 用 RLock 是允许同线程嵌套，比如 push 里再调 runcode。
EXEC_LOCK = threading.RLock()


class SandboxTimeout(Exception):
    pass


class InputExhausted(Exception):
    pass


def _make_input(stdin_lines):
    queue = list(stdin_lines)

    def _input(prompt=""):
        if prompt:
            sys.stdout.write(str(prompt))
            sys.stdout.flush()
        if not queue:
            raise InputExhausted(
                "运行台不支持交互式输入：请把测试数据直接写在代码里，或到「神经接口」终端中使用 input()"
            )
        return queue.pop(0)

    return _input


class _Watchdog:
    def __init__(self, deadline):
        self.deadline = deadline

    def __call__(self, frame, event, arg):
        if time.perf_counter() > self.deadline:
            raise SandboxTimeout()
        return self


def _error_payload(etype, evalue, tb):
    trimmed_tb = tb.tb_next if tb is not None else None
    text = "".join(traceback.format_exception(etype, evalue, trimmed_tb)).strip()
    return {
        "type": getattr(etype, "__name__", "Error"),
        "message": str(evalue),
        "traceback": text,
    }


def _truncate(text):
    """输出太长就丢前面留尾部，最近的输出和报错还能看到。"""
    if text and len(text) > MAX_OUTPUT:
        return "[输出过长已截断]\n" + text[-MAX_OUTPUT:]
    return text


class OutputTooLong(Exception):
    """输出超过硬上限，立即中断执行（防 OOM）。"""


class _BoundedBuffer(io.StringIO):
    """带上限的输出缓冲。只在结束后截断挡不住内存，
    死循环 print 在超时前就能吃几百 MB。这里边写边量，超过上限直接抛异常。"""

    HARD_LIMIT = MAX_OUTPUT * 4

    def __init__(self):
        super().__init__()
        self._written = 0

    def write(self, s):
        self._written += len(s)
        if self._written > self.HARD_LIMIT:
            raise OutputTooLong()
        return super().write(s)


def _run_protected(code, namespace, stdin_lines, timeout):
    out_buf = _BoundedBuffer()
    err_buf = _BoundedBuffer()
    state = {"finished": False, "error": None}
    import builtins

    original_input = builtins.input
    old_stdout = sys.stdout
    old_stderr = sys.stderr

    def target():
        try:
            sys.stdout = out_buf
            sys.stderr = err_buf
            builtins.input = _make_input(stdin_lines)
            sys.settrace(_Watchdog(time.perf_counter() + timeout))
            try:
                with sandbox_guard():
                    try:
                        exec(compile(code, "<pyneon>", "exec"), namespace)
                    finally:
                        # 必须在 guard 内先关 trace，否则看门狗可能在清理过程中再抛一次。
                        sys.settrace(None)
            finally:
                sys.settrace(None)
        except SandboxTimeout:
            state["error"] = {
                "type": "Timeout",
                "message": "代码运行超过 %.1f 秒，已被强制中断（可能是死循环）"
                % timeout,
                "traceback": "",
            }
        except OutputTooLong:
            state["error"] = {
                "type": "OutputTooLong",
                "message": "输出超过 %d KB 上限，已中断（疑似死循环打印）"
                % (_BoundedBuffer.HARD_LIMIT // 1024),
                "traceback": "",
            }
        except InputExhausted as exc:
            state["error"] = {
                "type": "InputError",
                "message": str(exc),
                "traceback": "",
            }
        except BaseException:
            etype, evalue, tb = sys.exc_info()
            state["error"] = _error_payload(etype, evalue, tb)
        finally:
            builtins.input = original_input
            sys.stdout = old_stdout
            sys.stderr = old_stderr
            state["finished"] = True

    started_at = time.perf_counter()
    worker = threading.Thread(target=target, daemon=True, name="pyneon-sandbox")
    thread_started = True
    try:
        worker.start()
    except RuntimeError as exc:
        # 无多线程能力的环境（Pyodide 未启用 SharedArrayBuffer 时 start() 抛
        # RuntimeError: can't start new thread）：降级为主线程直接执行。
        # settrace 看门狗仍能中断纯 Python 死循环；C 阻塞调用失去超时保护。
        if "can't start new thread" not in str(exc):
            raise
        thread_started = False
    if thread_started:
        worker.join(timeout + 1.5)
    else:
        # 在 except 之外调用，避免异常的 "During handling of the above"
        # 上下文污染用户代码的 traceback。
        target()
    duration_ms = int((time.perf_counter() - started_at) * 1000)

    # settrace 只能拦纯 Python 字节码。用户卡在 C 调用（sleep、C 写的循环）时，
    # join 超时后线程可能还在跑，CPython 又没法安全强杀线程。这里只能：
    # 不再读它的缓冲，免得脏数据上屏；每次用新命名空间，残留线程写不到状态；
    # 线程是 daemon，进程退出就结束。真要管 C 阻塞得上子进程，教学场景不值当。
    still_alive = not state["finished"]
    error = (
        None
        if state["finished"] and state["error"] is None
        else (
            state["error"]
            or {
                "type": "Timeout",
                "message": "代码运行超过 %.1f 秒，已被强制中断（可能是死循环）"
                % timeout,
                "traceback": "",
            }
        )
    )
    return {
        "stdout": _truncate(out_buf.getvalue()),
        "stderr": _truncate(err_buf.getvalue()),
        "error": error,
        "duration_ms": duration_ms,
        # 供调用方判断是否需要重建会话（超时线程可能仍在写命名空间）
        "worker_still_alive": still_alive,
    }


def _snapshot(namespace):
    # 先收数据变量，再按定义顺序倒着显示，最近算出来的放最上面。
    # 以前数到 40 个就 break，按插入顺序截断，最该看的最后几个反而没了。
    items = []
    for key, value in reversed(namespace.items()):
        if key.startswith("__"):
            continue
        # 只展示“数据变量”：过滤内置函数/自定义函数/方法/类/模块等噪音，
        # 避免快照面板出现 random: module 这类与学习无关的条目。
        if isinstance(
            value,
            (
                type,
                types.ModuleType,
                types.FunctionType,
                types.BuiltinFunctionType,
                types.BuiltinMethodType,
                types.MethodType,
                types.LambdaType,
                types.GeneratorType,
            ),
        ):
            continue
        try:
            if sys.getsizeof(value, 0) > MAX_VAR_BYTES:
                rendered = "<对象过大>"
            else:
                rendered = repr(value)
        except BaseException:
            # 自定义 __repr__ 抛异常或递归太深，也不能把整个快照带崩。
            rendered = "<无法显示>"
        if len(rendered) > MAX_REPR:
            rendered = rendered[:MAX_REPR] + "…"
        items.append({"name": key, "type": type(value).__name__, "value": rendered})
        if len(items) >= MAX_VARS:
            break
    return items


def run_code(code, stdin_lines=None, timeout=DEFAULT_TIMEOUT):
    namespace = {"__name__": "__main__", "__doc__": None}
    with EXEC_LOCK:
        core = _run_protected(code, namespace, stdin_lines or [], float(timeout))
    result = dict(core)
    result["ok"] = core["error"] is None
    result["variables"] = _snapshot(namespace)
    return result


def check_exercise(code, tests, stdin_lines=None):
    # 用户代码与后续每条断言都会改写进程级 sys.stdout，必须与 run_code 互斥，
    # 且整段（跑用户代码 + 跑用例）保持同一把锁，否则中途可能被另一次运行插入。
    namespace = {"__name__": "__main__", "__doc__": None}
    with EXEC_LOCK:
        return _check_exercise_locked(code, tests, stdin_lines, namespace)


def _check_exercise_locked(code, tests, stdin_lines, namespace):
    core = _run_protected(code, namespace, stdin_lines or [], DEFAULT_TIMEOUT)
    if core["error"] is not None:
        return {
            "ok": False,
            "passed": False,
            "stdout": core["stdout"],
            "stderr": core["stderr"],
            "test_output": "",
            "error": core["error"],
            "duration_ms": core["duration_ms"],
            "variables": [],
        }

    failures = []
    test_outputs = []
    # 用户代码跑完后，测试放在 namespace 的浅拷贝里跑：
    # 测试之间共享状态，课程里的 tests 本来就是准备、动作、断言三段；
    # 但测试的赋值不会污染用户命名空间，快照只显示用户自己定义的东西。
    # 函数和类通过浅拷贝对测试可见。
    test_ns = namespace.copy()
    # 判题把用户 stdout 交给用例，允许 assert 输出内容。
    # 不然只赋值不打印也能过，l01 就出过这个漏洞。stderr 同样处理。
    test_ns["__stdout__"] = core["stdout"]
    test_ns["__stderr__"] = core["stderr"]
    for index, test in enumerate(tests, start=1):
        test_state = {"finished": False, "error": None}
        test_out = io.StringIO()

        def test_target(ns=test_ns, source=test, st=test_state, buf=test_out):
            old_stdout = sys.stdout
            try:
                sys.stdout = buf
                sys.settrace(_Watchdog(time.perf_counter() + EXERCISE_TEST_TIMEOUT))
                try:
                    exec(compile(source, "<neon-test-%d>" % index, "exec"), ns)
                finally:
                    sys.settrace(None)
            except AssertionError as exc:
                st["error"] = str(exc) or "断言未通过"
            except SandboxTimeout:
                st["error"] = "测试执行超时"
            except BaseException:
                etype, evalue, _tb = sys.exc_info()
                st["error"] = "%s: %s" % (getattr(etype, "__name__", "Error"), evalue)
            finally:
                sys.stdout = old_stdout
                st["finished"] = True

        worker = threading.Thread(target=test_target, daemon=True)
        test_thread_started = True
        try:
            worker.start()
        except RuntimeError as exc:
            if "can't start new thread" not in str(exc):
                raise
            test_thread_started = False
        if test_thread_started:
            worker.join(EXERCISE_TEST_TIMEOUT + 1.0)
        else:
            test_target()  # 无多线程环境降级为主线程执行（同 run_code）
        if not test_state["finished"]:
            test_state["error"] = "测试执行超时"
        # 测试不管过没过，print 出来的内容都收着，用户加的排查输出才看得到。
        test_outputs.append(test_out.getvalue())
        if test_state["error"] is not None:
            failures.append(
                {"index": index, "test": test.strip(), "reason": test_state["error"]}
            )

    passed = not failures
    message = (
        ""
        if passed
        else "共 %d 个用例，%d 个未通过；首个失败：%s"
        % (len(tests), len(failures), failures[0]["reason"])
    )
    return {
        "ok": True,
        "passed": passed,
        "stdout": core["stdout"],
        "stderr": core["stderr"],
        "test_output": _truncate("".join(test_outputs)),
        "error": None
        if passed
        else {"type": "TestFailed", "message": message, "traceback": ""},
        "duration_ms": core["duration_ms"],
        "variables": _snapshot(namespace),
    }


def info_json():
    return json.dumps(
        {
            "python_version": sys.version.split()[0],
            "platform": sys.platform,
        }
    )


def run_code_json(payload_json):
    payload = json.loads(payload_json)
    result = run_code(
        payload.get("code", ""),
        payload.get("stdin") or [],
        payload.get("timeout", DEFAULT_TIMEOUT),
    )
    return json.dumps(result, ensure_ascii=False)


def check_exercise_json(payload_json):
    payload = json.loads(payload_json)
    result = check_exercise(
        payload.get("code", ""),
        payload.get("tests") or [],
        payload.get("stdin") or [],
    )
    return json.dumps(result, ensure_ascii=False)

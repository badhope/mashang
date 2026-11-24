"""CI 产物探针（tests/ci_apk_probe.py）的回归测试。

背景：GitHub runner 与本机构建出的 Chaquopy app.imy 布局不同
（runner.py / runner.pyc / __pycache__/runner.cpython-3XX.pyc），
探针必须三种都接受，否则 CI 假红；反过来，缺看门狗、假 pyc、未签名
必须判红，否则 CI 假绿。两种方向都在这里锁住。
"""
import io
import json
import marshal
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROBE = ROOT / "tests" / "ci_apk_probe.py"

LAYOUTS = ("py", "pyc", "pycache")
NUL = bytes([0])


def _runner_src():
    return (ROOT / "app/src/main/python/runner.py").read_text(encoding="utf-8")


def _repl_src():
    return (ROOT / "app/src/main/python/repl.py").read_text(encoding="utf-8")


def _fake_pyc():
    """构造结构合法的 pyc：3.13 魔数 + CRLF + 16 字节头。"""
    header = bytes([0xF3, 0x0D]) + bytes([13, 10]) + bytes(12)
    return header + marshal.dumps(compile(_runner_src(), "runner.py", "exec"))




def _add_pack_assets(z):
    """给假 APK 补 24 个预装包与 catalog，匹配 R7。"""
    for i in range(24):
        z.writestr(f"assets/content_packs/pack{i:02d}.v2.json", "[]")
    z.writestr(
        "assets/catalog.json",
        json.dumps({"packs": [{"id": f"pack{i:02d}"} for i in range(24)]}),
    )

def _write_apk(path, layout="py", *, with_assets=True, runner=None, repl=None):
    runner = runner if runner is not None else _runner_src()
    repl = repl if repl is not None else _repl_src()

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as inner:
        if layout == "py":
            inner.writestr("runner.py", runner)
            inner.writestr("repl.py", repl)
        elif layout == "pyc":
            inner.writestr("runner.pyc", _fake_pyc())
            inner.writestr("repl.pyc", _fake_pyc())
        else:
            inner.writestr("__pycache__/runner.cpython-313.pyc", _fake_pyc())
            inner.writestr("__pycache__/repl.cpython-313.pyc", _fake_pyc())

    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("assets/chaquopy/app.imy", buf.getvalue())
        if with_assets:
            for name in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json",
                         "lessons_pro.json", "challenges.json"):
                z.writestr(f"assets/{name}", json.dumps([]))
            for _lang in ("python", "javascript", "typescript", "c", "lua", "sql"):
                z.writestr(f"assets/syntaxes/{_lang}.tmLanguage.json", "{}")
            _add_pack_assets(z)
        z.writestr("AndroidManifest.xml", "com.pyneon.academy".encode("utf-16-le"))
        z.writestr("classes.dex", b"dex-035")
        # R9 检查要求 arm64-v8a 原生库就位（国内商店强制 64 位）
        z.writestr("lib/arm64-v8a/libpython3.13.so", b"ELF-fake-lib")
        for i in range(520):
            z.writestr(f"res/dummy_{i}.txt", "x")


class ApkProbeTests(unittest.TestCase):
    def _run(self, apk, *flags):
        return subprocess.run(
            [sys.executable, str(PROBE), str(apk), *flags],
            capture_output=True, text=True, encoding="utf-8",
        )

    def test_accepts_all_chaquopy_layouts(self):
        for layout in LAYOUTS:
            with self.subTest(layout=layout), tempfile.TemporaryDirectory() as tmp:
                apk = Path(tmp) / "app.apk"
                _write_apk(apk, layout)
                result = self._run(apk)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertIn("ALL APK PROBES PASSED", result.stdout)

    def test_rejects_runner_without_watchdog(self):
        """runner 模块缺 threading/settrace 时必须失败。"""
        for layout in LAYOUTS:
            with self.subTest(layout=layout), tempfile.TemporaryDirectory() as tmp:
                apk = Path(tmp) / "app.apk"
                if layout == "py":
                    _write_apk(apk, "py", runner="print('no watchdog')\n")
                else:
                    buf = io.BytesIO()
                    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as inner:
                        name = "runner.pyc" if layout == "pyc" else "__pycache__/runner.cpython-313.pyc"
                        inner.writestr(name, _fake_pyc())
                        inner.writestr(name.replace("runner", "repl"), _fake_pyc())
                    with zipfile.ZipFile(apk, "w", zipfile.ZIP_DEFLATED) as z:
                        z.writestr("assets/chaquopy/app.imy", buf.getvalue())
                        for n in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json",
                                  "lessons_pro.json", "challenges.json"):
                            z.writestr(f"assets/{n}", "[]")
                        for _lang in ("python", "javascript", "typescript", "c", "lua", "sql"):
                            z.writestr(f"assets/syntaxes/{_lang}.tmLanguage.json", "{}")
                        _add_pack_assets(z)
                        z.writestr("AndroidManifest.xml", "com.pyneon.academy".encode("utf-16-le"))
                        z.writestr("classes.dex", b"dex-035")
                        for i in range(520):
                            z.writestr(f"res/d{i}.txt", "x")
                    continue
                result = self._run(apk)
                self.assertEqual(result.returncode, 1)
                self.assertIn("超时看门狗缺失", result.stdout)

    def test_rejects_corrupt_pyc_magic(self):
        """pyc 头两个魔数字节不对时必须失败（防假 pyc 混过 marshal）。"""
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "app.apk"
            buf = io.BytesIO()
            with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as inner:
                bad = NUL + NUL + bytes([13, 10]) + bytes(12) + marshal.dumps(
                    compile(_runner_src(), "runner.py", "exec"))
                inner.writestr("runner.pyc", bad)
                inner.writestr("repl.pyc", _fake_pyc())
            with zipfile.ZipFile(apk, "w", zipfile.ZIP_DEFLATED) as z:
                z.writestr("assets/chaquopy/app.imy", buf.getvalue())
                for n in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json",
                          "lessons_pro.json", "challenges.json"):
                    z.writestr(f"assets/{n}", "[]")
                for _lang in ("python", "javascript", "typescript", "c", "lua", "sql"):
                    z.writestr(f"assets/syntaxes/{_lang}.tmLanguage.json", "{}")
                _add_pack_assets(z)
                z.writestr("AndroidManifest.xml", "com.pyneon.academy".encode("utf-16-le"))
                z.writestr("classes.dex", b"dex-035")
                for i in range(520):
                    z.writestr(f"res/d{i}.txt", "x")
            result = self._run(apk)
            self.assertEqual(result.returncode, 1)
            self.assertIn("pyc", result.stdout)

    def test_require_release_rejects_unsigned(self):
        """--require-release 必须拒绝未签名包。"""
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "app.apk"
            _write_apk(apk, "py")
            result = self._run(apk, "--require-release")
            self.assertEqual(result.returncode, 1)
            self.assertIn("未签名", result.stdout)

    def test_fails_when_lessons_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "app.apk"
            _write_apk(apk, "py", with_assets=False)
            result = self._run(apk)
            self.assertEqual(result.returncode, 1)
            self.assertIn("缺少 assets/lessons_basic.json", result.stdout)


if __name__ == "__main__":
    unittest.main()

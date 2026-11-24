"""发布口径回归：文档里的数字与能力声明必须等于仓库事实。

背景（2026-10-24）：v0.4.0 发布后复核发现 FAQ 写 128 条测试、软著说明书写
119 条 / 200 用例 / 「仅内置 Python」，Release body 写 136 条，而实际是
140 条 / 201 用例 / Python+JavaScript 双运行时。本文件把「文档自述」变成
可执行门禁：数字从仓库现算，声明从代码读，任何漂移直接失败。

被锁定的文件：
  - FAQ.md
  - docs/softcopy/软件说明书.md
  - README.md / README.zh-CN.md / README.ja-JP.md
  - app/src/main/java/com/pyneon/academy/lang/LanguageRuntime.kt
  - app/src/main/java/com/pyneon/academy/screens/{AboutScreen,PrivacyPolicyScreen}.kt
  - web/app.js / web/index.html（版本串）
"""

import ast
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TESTS = ROOT / "tests"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PACKS = ROOT / "content_packs"
JAVA = ROOT / "app" / "src" / "main" / "java" / "com" / "pyneon" / "academy"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _test_method_count() -> int:
    total = 0
    for f in sorted(TESTS.glob("test_*.py")):
        tree = ast.parse(_read(f))
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef) and node.name.startswith("test_"):
                total += 1
    return total


def _judge_counts():
    """返回六门语言的判题项与用例数。

    注意：Lua / SQL 课程包的判题项不计入「Python 判题项」——它们由
    LuaJ 与 sqlite3 执行，Python 侧的 validate_content.py 不覆盖。
    C / TypeScript 由 Kotlin 侧的 CCoursePacksTest / TypeScriptCoursePacksTest
    执行；旧版这里漏计了这两门，导致软著「合计」少算 76 项 / 311 用例。
    总数这两个数字用于断言软著说明书的汇总行。
    """
    items = []
    for f in sorted(ASSETS.glob("lessons_*.json")) + sorted(PACKS.glob("*.json")):
        items.extend(json.loads(_read(f)))
    items.extend(json.loads(_read(ASSETS / "challenges.json")))
    counters = {
        name: {"items": 0, "cases": 0}
        for name in ("python", "javascript", "typescript", "c", "lua", "sql")
    }
    for item in items:
        tests = (item.get("exercise") or {}).get("tests") or item.get("tests") or []
        if not tests:
            continue
        lang = item.get("lang", "python")
        if lang in counters:
            counters[lang]["items"] += 1
            counters[lang]["cases"] += len(tests)
    return counters


def _source_scale():
    kt = list((ROOT / "app" / "src" / "main" / "java").rglob("*.kt"))
    py = list((ROOT / "app" / "src" / "main" / "python").rglob("*.py"))

    def lines(files):
        return sum(len(_read(f).splitlines()) for f in files)

    return len(kt), len(py), lines(kt), lines(py)


class TestCountClaims(unittest.TestCase):
    def test_faq_test_count_matches_suite(self):
        count = _test_method_count()
        text = _read(ROOT / "FAQ.md")
        self.assertIn(f"{count} 条测试", text, f"FAQ 的测试条数与实际 {count} 不一致")

    def test_softcopy_test_count_matches_suite(self):
        count = _test_method_count()
        text = _read(ROOT / "docs" / "softcopy" / "软件说明书.md")
        self.assertIn(
            f"| 合计 | **{count}** |", text, f"软著说明书合计应为 {count} 条"
        )

    def test_softcopy_judge_counts_match_assets(self):
        counts = _judge_counts()
        text = _read(ROOT / "docs" / "softcopy" / "软件说明书.md")
        labels = {
            "python": "Python",
            "javascript": "JavaScript",
            "typescript": "TypeScript",
            "c": "C",
            "lua": "Lua",
            "sql": "SQL",
        }
        for lang, label in labels.items():
            c = counts[lang]
            for snippet in (f"{c['items']} 个 {label}", f"{c['cases']} 条 {label}"):
                with self.subTest(snippet=snippet):
                    self.assertIn(snippet, text, f"软著说明书缺少或写错：{snippet}")
        total_items = sum(c["items"] for c in counts.values())
        total_cases = sum(c["cases"] for c in counts.values())
        for snippet in (f"{total_items} 个判题项", f"{total_cases} 条用例"):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, text, f"软著说明书缺少或写错：{snippet}")

    def test_softcopy_source_scale_matches_repo(self):
        kt_files, py_files, kt_lines, py_lines = _source_scale()
        text = _read(ROOT / "docs" / "softcopy" / "软件说明书.md")
        expect = f"{kt_files + py_files} 个源文件、{kt_lines + py_lines:,} 行（Kotlin {kt_files} 个 + Python {py_files} 个）"
        self.assertIn(expect, text, f"软著说明书的源程序规模应更新为：{expect}")


class TestCapabilityClaims(unittest.TestCase):
    def test_softcopy_limitation_is_current(self):
        text = _read(ROOT / "docs" / "softcopy" / "软件说明书.md")
        self.assertNotIn("JavaScript 等后端尚未实现", text, "JavaScript 运行时已内置，局限性表述过期")
        self.assertNotIn("当前仅内置 Python 运行时", text, "已内置 Python + JavaScript 两个运行时")

    def test_runtime_fallback_has_no_download_promise(self):
        text = _read(JAVA / "lang" / "LanguageRuntime.kt")
        self.assertNotIn("可在「语言包」中下载", text, "项目没有语言包下载功能，不得承诺下载")
        self.assertIn("未内置于当前版本", text, "回退提示应说明运行时未内置")

    def test_about_and_privacy_do_not_claim_first_language_pack(self):
        for name in ("AboutScreen.kt", "PrivacyPolicyScreen.kt"):
            with self.subTest(file=name):
                text = _read(JAVA / "screens" / name)
                self.assertNotIn("首发语言包", text, "「首发语言包」暗示后续语言包可下载，与实际不符")

    def test_readmes_name_shipped_runtimes(self):
        """README 不得声称「语言包可扩展运行时」，且必须列全六门语言。

        2026-11 调整：README 精简为「一句话 + 截图 + 安装」后，首屏不再逐门列语言。
        断言范围从「前 12 行」改为「全文」——语言表章节仍必须完整，
        但不该为了过测试往首屏堆废话。
        """
        forbidden = ("语言包扩展更多", "more via language packs", "言語パックで拡張")
        for name in ("README.md", "README.zh-CN.md", "README.ja-JP.md"):
            with self.subTest(file=name):
                text = _read(ROOT / name)
                for phrase in forbidden:
                    self.assertNotIn(phrase, text, f"{name} 声称语言包可扩展运行时，但当前无语言包下载")
                for lang in ("Python", "JavaScript", "TypeScript", "C", "Lua", "SQL"):
                    self.assertIn(lang, text, f"{name} 缺少语言：{lang}")

    def test_readmes_list_all_shipped_languages(self):
        """三语 README 的「学什么」表必须逐门列出六门运行时，且点明课程已预装。

        口径约定（2026-11）：对外讲「有哪些语言、各能学到什么」，
        不再用单一的判题总数当卖点——数字会随内容增删漂移，语言清单不会。
        """
        cases = {
            "README.zh-CN.md": ("Python", "JavaScript", "TypeScript", "C", "Lua", "SQL", "预装"),
            "README.md": ("Python", "JavaScript", "TypeScript", "C", "Lua", "SQL", "ship"),
            "README.ja-JP.md": ("Python", "JavaScript", "TypeScript", "C", "Lua", "SQL", "同梱"),
        }
        for name, needles in cases.items():
            with self.subTest(file=name):
                text = _read(ROOT / name)
                for needle in needles:
                    self.assertIn(needle, text, f"{name} 缺少语言清单要素：{needle}")

    def test_no_stale_judge_totals_in_public_docs(self):
        """公开文档不得再出现与仓库不符的判题总数（曾写死 63 项 / 230 用例）。

        正确口径来自 assets + content_packs 实时统计，禁止在文案里硬编码。
        """
        stale = ("63 项 assert 判题", "63 assert judge items", "63 項目",
                 "230 用例", "230 CASES")
        for name in ("README.md", "README.zh-CN.md", "README.ja-JP.md"):
            with self.subTest(file=name):
                text = _read(ROOT / name)
                for phrase in stale:
                    self.assertNotIn(phrase, text, f"{name} 仍含过期判题总数：{phrase}")
        index = _read(ROOT / "docs" / "index.html")
        for phrase in ("63 项 assert 判题", "63 项判题", "230 用例", "230 CASES"):
            self.assertNotIn(phrase, index, f"官网仍含过期判题总数：{phrase}")


class TestCiClaims(unittest.TestCase):
    """GitCode 的代码化流水线 push 不自动触发，文档不得再声称它承担门禁。"""

    FORBIDDEN = (
        "GitCode Actions 负责引擎",
        "GitCode Actions runs the engine",
        "GitCode Actions はエンジン",
    )

    def test_docs_do_not_claim_gitcode_ci_gate(self):
        for name in ("README.md", "README.zh-CN.md", "README.ja-JP.md", "FAQ.md"):
            with self.subTest(file=name):
                text = _read(ROOT / name)
                for phrase in self.FORBIDDEN:
                    self.assertNotIn(phrase, text, f"{name} 仍声称 GitCode Actions 承担门禁：{phrase}")
                self.assertIn("GitCode", text)


class TestVersionClaims(unittest.TestCase):
    def test_web_version_matches_gradle(self):
        gradle = _read(ROOT / "app" / "build.gradle.kts")
        version = re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)
        app_js = _read(ROOT / "web" / "app.js")
        self.assertIn(f'APP_VERSION = "{version}"', app_js, "web/app.js 版本串与 versionName 不一致")
        index = _read(ROOT / "web" / "index.html")
        self.assertIn(f'id="foot-version">v{version}<', index, "web/index.html 页脚版本与 versionName 不一致")
        for copy in ("docs/web/app.js", "docs/web/index.html"):
            with self.subTest(copy=copy):
                self.assertEqual(
                    _read(ROOT / "web" / Path(copy).name),
                    _read(ROOT / copy),
                    f"{copy} 与 web/ 源文件不一致，需运行 tools/sync_pages_assets.py",
                )


if __name__ == "__main__":
    unittest.main(verbosity=2)

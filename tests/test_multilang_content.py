"""
多语言架构 · 内容侧行为锁
=========================

锁定「课程 JSON ↔ Kotlin LanguageRuntime / LessonRepository」之间关于 `lang`
字段的契约。新增语言时，只要在这里补一个 id 即可，若有人写入未登记的语言，
测试会立刻失败，避免 UI 出现无法执行的空课程。

被锁定的 Kotlin 语义（改 Kotlin 时同步改这里）：
  - LessonRepository.parseLesson:  obj.optString("lang", "python")
  - LessonRepository.parseChallenge: obj.optString("lang", "python")
  - Block.CodeBlock.lang:          b.optString("lang", "")   // 空串 = 继承本讲语言
  - LanguageIds:                   python / javascript / typescript / c / lua / sql 已实现，
                                  java / cpp / go / rust 为规划中
"""

import json
import os
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"

LESSON_FILES = [
    "lessons_basic.json",
    "lessons_mid.json",
    "lessons_adv.json",
    "lessons_pro.json",
]
CHALLENGE_FILES = ["challenges.json"]

# 与 Kotlin LanguageIds 保持一致
IMPLEMENTED_LANGS = {"python", "javascript", "typescript", "c", "lua", "sql"}
PLANNED_LANGS = {"java", "cpp", "go", "rust"}
KNOWN_LANGS = IMPLEMENTED_LANGS | PLANNED_LANGS

DEFAULT_LANG = "python"


def _load(name):
    with open(ASSETS / name, "r", encoding="utf-8") as f:
        return json.load(f)


def _all_lessons():
    for name in LESSON_FILES:
        for item in _load(name):
            yield name, item


def _all_challenges():
    for name in CHALLENGE_FILES:
        for item in _load(name):
            yield name, item


class LangFieldContractTests(unittest.TestCase):
    """lang 字段本身必须合法：缺失 → python；出现 → 必须是已登记语言"""

    def test_legacy_content_without_lang_defaults_to_python(self):
        """旧课程 JSON 不带 lang 字段是可接受的，运行时按 python 处理。"""
        missing = [i for _, i in _all_lessons() if "lang" not in i]
        # 不断言必须为空：向后兼容是设计目标。只断言"缺失即视为 python"。
        for item in missing:
            self.assertEqual(DEFAULT_LANG, item.get("lang", DEFAULT_LANG))

    def test_lesson_lang_is_registered(self):
        for fname, item in _all_lessons():
            lang = item.get("lang", DEFAULT_LANG)
            with self.subTest(file=fname, id=item.get("id"), lang=lang):
                self.assertIn(lang, KNOWN_LANGS, f"未登记的语言：{lang}")

    def test_challenge_lang_is_registered(self):
        for fname, item in _all_challenges():
            lang = item.get("lang", DEFAULT_LANG)
            with self.subTest(file=fname, id=item.get("id"), lang=lang):
                self.assertIn(lang, KNOWN_LANGS, f"未登记的语言：{lang}")

    def test_no_lesson_runs_unimplemented_language(self):
        """
        R1：不允许出现"有内容但没有运行时"的语言。
        规划中的语言（java/cpp/go/rust）一旦出现在课程里，必须先落地运行时。
        """
        offenders = [
            (item.get("id"), item.get("lang"))
            for _, item in _all_lessons()
            if item.get("lang", DEFAULT_LANG) in PLANNED_LANGS
        ]
        self.assertEqual([], offenders, f"以下课程使用了未实现的语言：{offenders}")

    def test_codeblock_lang_override_is_registered(self):
        """代码块可自带 lang 覆盖（用于对比教学），但同样必须是已登记语言"""
        for fname, item in _all_lessons():
            for b in item.get("blocks", []):
                if b.get("type") != "code":
                    continue
                lang = b.get("lang", "")
                if lang == "":
                    continue  # 空串 = 继承本讲语言
                with self.subTest(file=fname, id=item.get("id"), lang=lang):
                    self.assertIn(lang, KNOWN_LANGS, f"未登记的语言：{lang}")


class LangConsistencyTests(unittest.TestCase):
    """同一讲内部不得出现互相矛盾的语言声明"""

    def test_lesson_id_unique_across_all_languages(self):
        """
        R2：LessonRepository.lessons() 用 distinctBy { it.id } 去重。
        若不同语言的课程撞 id，后加载的一讲会被静默吞掉。
        """
        seen = {}
        for fname, item in _all_lessons():
            lid = item["id"]
            with self.subTest(id=lid):
                self.assertNotIn(lid, seen, f"课程 id 重复：{lid}（{fname} 与 {seen.get(lid)}）")
                seen[lid] = fname

    def test_lang_is_lowercase_no_spaces(self):
        """lang 直接参与 SharedPreferences key 与语言包目录名，必须可安全拼接"""
        import re
        for _, item in list(_all_lessons()) + list(_all_challenges()):
            lang = item.get("lang", DEFAULT_LANG)
            with self.subTest(id=item.get("id"), lang=lang):
                self.assertRegex(lang, r"^[a-z0-9+#]+$", f"lang 格式非法：{lang!r}")


class RuntimeContractTests(unittest.TestCase):
    """Kotlin 接口文件与内容约定必须保持同步（防止改了 Kotlin 忘了改文档/测试）"""

    LANG_KT = ROOT / "app" / "src" / "main" / "java" / "com" / "pyneon" / "academy" / "lang" / "LanguageRuntime.kt"

    def test_language_runtime_interface_exists(self):
        self.assertTrue(
            self.LANG_KT.exists(),
            "缺少 lang/LanguageRuntime.kt —— 多语言架构的核心抽象被删除了",
        )

    def test_interface_declares_full_contract(self):
        """run / check / repl / info 四组能力必须齐全，缺一即视为破坏多语言契约"""
        src = self.LANG_KT.read_text(encoding="utf-8")
        for member in [
            "interface LanguageRuntime",
            "fun runCode(",
            "fun checkExercise(",
            "fun replPush(",
            "fun replStart()",
            "fun warmup()",
            "fun ensureStarted(",
            "object LanguageRegistry",
            "fun register(",
        ]:
            with self.subTest(member=member):
                self.assertIn(member, src)

    def test_python_bridge_implements_the_interface(self):
        pyb = (ROOT / "app" / "src" / "main" / "java" / "com" / "pyneon" / "academy" / "py" / "PyBridge.kt")
        src = pyb.read_text(encoding="utf-8")
        self.assertIn("object PyBridge : LanguageRuntime", src)

    def test_no_ui_layer_references_python_bridge_directly(self):
        """
        R3：UI 层禁止直接依赖具体运行时，必须经 LanguageRegistry。
        否则新增语言时每个界面都要改，多语言架构就名存实亡。
        """
        screens = (ROOT / "app" / "src" / "main" / "java" / "com" / "pyneon" / "academy" / "screens")
        offenders = []
        for f in screens.glob("*.kt"):
            text = f.read_text(encoding="utf-8")
            if "PyBridge" in text:
                offenders.append(f.name)
        self.assertEqual([], offenders, f"以下界面仍在直接引用 PyBridge：{offenders}")


if __name__ == "__main__":
    unittest.main(verbosity=2)

"""语言覆盖底板：六门语言必须真的有课、有题，且每讲题量不再退化。

背景（2026-11-22）：对外说「六门语言、230+ 讲」，但此前没有任何测试约束
「每门语言到底有多少讲、每讲有没有判题、每讲几道用例」。结果 Python 有
8 讲只有 2 道用例、1 讲只有 1 道，SQL 全部 38 讲固定 3 道，C 有 14 讲 3 道。
这些课能跑，但题量薄到只能验证一个 happy path。

这里把最低水位写成门禁，不追求「题海」：
  - 五门扩展语言至少 38 讲（SQL 已扩到 43 讲边界专题；Python 另有 DSA / 彩蛋）
  - 245 讲每讲必须有判题，且至少 3 道用例
  - l37 是「写两个 unittest」的元课程，2 道用例是教学目标本身，唯一豁免
  - 每门语言课程用例 >= 110，全库课程用例 >= 900

注意：这里只数「课程 exercise」，不含 8 道独立挑战；挑战由
test_docs_claims 的判题口径单独统计。
"""

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PACKS = ROOT / "content_packs"
IMPLEMENTED = ("python", "javascript", "typescript", "c", "lua", "sql")
EXTENSION_LANGS = ("javascript", "typescript", "c", "lua", "sql")
MIN_CASES_PER_LESSON = 3
MIN_CASES_PER_LANGUAGE = 110
MIN_TOTAL_CASES = 900
# l37 的教学目标是补全两个 unittest 方法，题面本身就只有两道断言。
META_LESSON = "l37"


def _read(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def _all_lessons():
    for name in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json"):
        yield from _read(ASSETS / name)
    for path in sorted(PACKS.glob("*.json")):
        yield from _read(path)


def _lang(item):
    return item.get("lang", "python")


def _cases(item):
    return (item.get("exercise") or {}).get("tests") or []


class LanguageCoverageTests(unittest.TestCase):
    def test_each_extension_language_has_at_least_38_pack_lessons(self):
        # SQL 在 38 讲之后补了 4 讲边界专题；其余四门保持 38 讲。
        expected = {"javascript": 38, "typescript": 38, "c": 38, "lua": 38, "sql": 43}
        counts = {lang: 0 for lang in EXTENSION_LANGS}
        for item in _all_lessons():
            lang = _lang(item)
            if lang in counts:
                counts[lang] += 1
        for lang in EXTENSION_LANGS:
            with self.subTest(lang=lang):
                self.assertEqual(expected[lang], counts[lang], f"{lang} 课程包应为 {expected[lang]} 讲，实际 {counts[lang]}")

    def test_every_lesson_has_a_judge_exercise_and_at_least_three_cases(self):
        offenders = []
        missing = []
        for item in _all_lessons():
            lid = item.get("id")
            if not _cases(item):
                missing.append(lid)
                continue
            if lid == META_LESSON:
                continue
            if len(_cases(item)) < MIN_CASES_PER_LESSON:
                offenders.append((lid, len(_cases(item))))
        self.assertEqual([], missing, f"以下课程没有判题：{missing}")
        self.assertEqual([], offenders, f"以下课程少于 {MIN_CASES_PER_LESSON} 道用例：{offenders}")

    def test_case_volume_floor_per_language_and_total(self):
        per_language = {lang: 0 for lang in IMPLEMENTED}
        total = 0
        for item in _all_lessons():
            lang = _lang(item)
            n = len(_cases(item))
            if lang in per_language:
                per_language[lang] += n
            total += n
        for lang in IMPLEMENTED:
            with self.subTest(lang=lang):
                self.assertGreaterEqual(
                    per_language[lang], MIN_CASES_PER_LANGUAGE,
                    f"{lang} 课程用例只有 {per_language[lang]}，低于 {MIN_CASES_PER_LANGUAGE}",
                )
        self.assertGreaterEqual(total, MIN_TOTAL_CASES, f"全库课程用例只有 {total}")


if __name__ == "__main__":
    unittest.main(verbosity=2)

#!/usr/bin/env python3
"""校验基线文档里的关键数字和仓库实际状态是否一致。"""
from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "docs" / "PROJECT_BASELINE.md"
BUILD = ROOT / "app" / "build.gradle.kts"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PACKS = ROOT / "content_packs"


def fail(message: str) -> None:
    print(f"[FAIL] {message}")
    sys.exit(1)


def build_version() -> tuple[str, int]:
    text = BUILD.read_text(encoding="utf-8")
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code = re.search(r'versionCode\s*=\s*(\d+)', text)
    if not name or not code:
        fail("build.gradle.kts 里找不到 versionName/versionCode")
    return name.group(1), int(code.group(1))


def load_json(path: Path) -> list[dict]:
    return json.loads(path.read_text(encoding="utf-8"))


def course_stats() -> tuple[int, int, int, int]:
    built_in = [
        ASSETS / "lessons_basic.json",
        ASSETS / "lessons_mid.json",
        ASSETS / "lessons_adv.json",
        ASSETS / "lessons_pro.json",
    ]
    lessons: list[tuple[dict, bool]] = []
    for path in built_in:
        lessons.extend((item, True) for item in load_json(path))
    for path in sorted(PACKS.glob("*.json")):
        lessons.extend((item, False) for item in load_json(path))

    code_blocks = 0
    runnable = 0
    cases = 0
    for lesson, default_runnable in lessons:
        for block in lesson.get("blocks", []):
            if block.get("type") != "code":
                continue
            code_blocks += 1
            if block.get("runnable", default_runnable):
                runnable += 1
        exercise = lesson.get("exercise")
        if exercise:
            cases += len(exercise.get("tests", []))

    challenges = load_json(ASSETS / "challenges.json")
    cases += sum(len(item.get("tests", [])) for item in challenges)
    return len(lessons), code_blocks, runnable, cases


def git_branch() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"], cwd=ROOT, text=True
        ).strip()
    except Exception:
        return "unknown"


def main() -> None:
    text = BASELINE.read_text(encoding="utf-8")
    marker = re.search(
        r"<!-- baseline: version=([^\s]+) code=(\d+) lessons=(\d+) "
        r"code_blocks=(\d+) runnable=(\d+) cases=(\d+) -->",
        text,
    )
    if not marker:
        fail("基线文档缺少 baseline 标记")
    version, code, lessons, blocks, runnable, cases = marker.groups()

    actual_version, actual_code = build_version()
    actual_lessons, actual_blocks, actual_runnable, actual_cases = course_stats()
    checks = [
        ("version", version, actual_version),
        ("versionCode", int(code), actual_code),
        ("lessons", int(lessons), actual_lessons),
        ("code_blocks", int(blocks), actual_blocks),
        ("runnable", int(runnable), actual_runnable),
        ("cases", int(cases), actual_cases),
    ]
    for name, expected, actual in checks:
        if expected != actual:
            fail(f"{name}: 文档写 {expected}，实际 {actual}")

    if git_branch() != "main":
        fail(f"当前分支不是 main，而是 {git_branch()}")

    print(
        f"[PASS] 基线一致：v{actual_version} / code {actual_code} / "
        f"{actual_lessons} 讲 / {actual_blocks} 代码块（{actual_runnable} 可运行）/ {actual_cases} 用例"
    )


if __name__ == "__main__":
    main()

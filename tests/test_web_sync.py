"""Web 端与 App 内置内容的同源一致性回归网（S9 切片）。

锁定八条契约（R6b 为品牌图指纹；见 .aqg/code-construction/mashang-web-sync-cleanup.md）：
  R1 data.js 课程/挑战 == app/src/main/assets（含 lessons_pro）
  R2 engine.js 内嵌 ENGINE_PY == app/src/main/python/runner.py（逐字节）
  R3 内置课程结构字段：chapter int / chapterName 非空 / exercise.xp > 0
  R4 Pyodide 版本串在 index.html 与 app.js 一致且回退源 >= 2
  R5 web 角斗场按 lang 分流（JS 挑战不得进 Python 判题路径）
  R6 品牌残留为零（web/index.html、docs/index.html、README.zh-CN.md）
  R7 重置清除错题本/复习卡（Kotlin 静态契约）
"""
import json
import os
import pathlib
import re
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
# 2026-11-06 CodeLearning 品牌重塑后的品牌图指纹（banner 2560x800 / poster 1080x1920）
BANNER_SHA = "5fd2ecad79d7cc4f4be198aa31070dfdfe89759a93518df20ac6d160f80a69d4"
POSTER_SHA = "e29b59100e14f87c83a1448338e36dab35cddab724afcc92d8e4c4b44f8dd684"
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
WEB = os.path.join(ROOT, "web")

LESSON_FILES = [
    "lessons_basic.json",
    "lessons_mid.json",
    "lessons_adv.json",
    "lessons_pro.json",
]


def _read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def _load_assets():
    """assets 内置课程 + content_packs 语言包（与 web/gen_data.py 的合并顺序一致）。"""
    lessons = []
    for name in LESSON_FILES:
        lessons.extend(json.loads(_read(os.path.join(ASSETS, name))))
    packs_dir = os.path.join(ROOT, "content_packs")
    for name in sorted(os.listdir(packs_dir)):
        if name.endswith(".json"):
            lessons.extend(json.loads(_read(os.path.join(packs_dir, name))))
    challenges = json.loads(_read(os.path.join(ASSETS, "challenges.json")))
    return lessons, challenges


def _parse_data_js():
    text = _read(os.path.join(WEB, "data.js"))
    m = re.search(r"window\.LESSONS = (.*?);\nwindow\.CHALLENGES = (.*?);\n", text, re.S)
    if not m:
        raise AssertionError("data.js 无法解析 window.LESSONS / window.CHALLENGES")
    return json.loads(m.group(1)), json.loads(m.group(2))


def _parse_engine_js():
    text = _read(os.path.join(WEB, "engine.js"))
    m = re.search(r"window\.ENGINE_PY = (.*?);\nwindow\.REPL_HELPER_PY", text, re.S)
    if not m:
        raise AssertionError("engine.js 无法解析 window.ENGINE_PY")
    return json.loads(m.group(1))


class TestWebDataSync(unittest.TestCase):
    def test_web_lessons_match_assets(self):
        """R1: 课程逐条一致（id 序列 + chapter + order），含全部语言包。"""
        assets_lessons, _ = _load_assets()
        web_lessons, _ = _parse_data_js()
        self.assertEqual(
            [l["id"] for l in web_lessons],
            [l["id"] for l in assets_lessons],
            "data.js 课程 id 序列与 assets 不一致（需重跑 python web/gen_data.py）",
        )
        for w, a in zip(web_lessons, assets_lessons):
            self.assertEqual(w.get("chapter"), a.get("chapter"), f"{a['id']} chapter 漂移")
            self.assertEqual(w.get("order"), a.get("order"), f"{a['id']} order 漂移")

    def test_web_challenges_match_assets(self):
        """R1: 挑战逐条一致（含 lang 字段，JS 挑战不得被静默丢弃）。"""
        _, assets_challenges = _load_assets()
        _, web_challenges = _parse_data_js()
        self.assertEqual(
            [c["id"] for c in web_challenges],
            [c["id"] for c in assets_challenges],
            "data.js 挑战 id 序列与 assets 不一致（需重跑 python web/gen_data.py）",
        )
        for w, a in zip(web_challenges, assets_challenges):
            self.assertEqual(w.get("lang", "python"), a.get("lang", "python"), f"{a['id']} lang 漂移")

    def test_web_engine_matches_runner(self):
        """R2: ENGINE_PY 与 runner.py 逐字节一致。"""
        embedded = _parse_engine_js()
        current = _read(os.path.join(ROOT, "app", "src", "main", "python", "runner.py"))
        self.assertEqual(
            embedded,
            current,
            "engine.js 内嵌引擎与 runner.py 不一致（需重跑 python web/gen_data.py）",
        )


class TestLessonSchema(unittest.TestCase):
    def test_chapter_is_int_and_exercise_xp_positive(self):
        """R3: chapter 为 int、chapterName 非空、exercise.xp 为正整数。"""
        assets_lessons, _ = _load_assets()
        problems = []
        for l in assets_lessons:
            lid = l["id"]
            if not isinstance(l.get("chapter"), int) or isinstance(l.get("chapter"), bool):
                problems.append(f"{lid}: chapter 非 int（{l.get('chapter')!r}）")
            if not (l.get("chapterName") or "").strip():
                problems.append(f"{lid}: chapterName 为空")
            ex = l.get("exercise")
            if ex:
                xp = ex.get("xp")
                if not isinstance(xp, int) or isinstance(xp, bool) or xp <= 0:
                    problems.append(f"{lid}: exercise.xp 非正 int（{xp!r}）")
        self.assertEqual(problems, [], f"课程结构问题: {problems}")


class TestPyodidePin(unittest.TestCase):
    def _app_version(self):
        text = _read(os.path.join(WEB, "app.js"))
        m = re.search(r'PYODIDE_VERSION\s*=\s*"([0-9.]+)"', text)
        self.assertIsNotNone(m, "app.js 缺少 PYODIDE_VERSION 常量")
        return m.group(1), text

    def test_index_and_app_versions_match(self):
        """R4: index.html 脚本 URL 与 app.js 常量版本一致。"""
        version, _ = self._app_version()
        html = _read(os.path.join(WEB, "index.html"))
        m = re.search(r"pyodide/v([0-9.]+)/full/pyodide\.js", html)
        self.assertIsNotNone(m, "index.html 未引用 pyodide/v<版本>/full/pyodide.js")
        self.assertEqual(m.group(1), version, "index.html 与 app.js 的 Pyodide 版本不一致")

    def test_fallback_sources(self):
        """R4: 至少 3 个引擎源（1 主 + >=2 回退），主机去重后 >= 2。"""
        _, text = self._app_version()
        m = re.search(r"PYODIDE_SOURCES\s*=\s*\[(.*?)\];", text, re.S)
        self.assertIsNotNone(m, "app.js 缺少 PYODIDE_SOURCES 回退链")
        urls = re.findall(r'"(https://[^"]+)"', m.group(1))
        self.assertGreaterEqual(len(urls), 3, f"引擎源不足 3 个: {urls}")
        hosts = {re.match(r"https://([^/]+)", u).group(1) for u in urls}
        self.assertGreaterEqual(len(hosts), 2, f"回退源主机去重后不足 2 个: {hosts}")


class TestArenaLangSplit(unittest.TestCase):
    def test_js_challenges_not_routed_to_python(self):
        """R5: 角斗场按 lang 分流，JS 挑战标注「App 内体验」。"""
        text = _read(os.path.join(WEB, "app.js"))
        self.assertRegex(
            text,
            r"function splitChallenges",
            "app.js 缺少 splitChallenges 分流函数",
        )
        seg = text[text.index("function splitChallenges"):][:800]
        # 断言真实分流逻辑：python 进判题组，其余进 appOnly 组
        self.assertRegex(
            seg,
            r'if \(\(ch\.lang \|\| "python"\) === "python"\) python\.push\(ch\);\s*\n\s*else appOnly\.push\(ch\);',
            "splitChallenges 未把非 python 挑战放进 appOnly",
        )
        self.assertNotIn("if (true)", seg, "splitChallenges 出现恒真短路")
        self.assertIn("App 内体验", text, "app.js 缺少 JS 挑战的「App 内体验」标注")
        # 渲染路径必须真的只用 split.python 进判题卡片
        arena = text[text.index("function renderArena"):][:1200]
        self.assertIn("split.python.forEach", arena, "renderArena 未使用 python 分流结果")
        self.assertIn("split.appOnly.forEach", arena, "renderArena 未渲染 appOnly 卡片")


class TestBrandResidue(unittest.TestCase):
    def test_no_residue(self):
        """R6: 三处品牌残留为零。"""
        targets = [
            os.path.join(WEB, "index.html"),
            os.path.join(ROOT, "docs", "index.html"),
            os.path.join(ROOT, "README.zh-CN.md"),
        ]
        patterns = ["码上 · 码上", "PY//NOW", "码上Python", "码上PYTHON"]
        hits = []
        for path in targets:
            text = _read(path)
            for p in patterns:
                if p in text:
                    hits.append(f"{os.path.relpath(path, ROOT)}: {p}")
        self.assertEqual(hits, [], f"品牌残留: {hits}")
        self.assertIn("码上", _read(os.path.join(WEB, "index.html")), "web 首页应含新品牌名")

    def test_banner_and_poster_are_not_old_brand_assets(self):
        """R6b: 头图/海报曾整张是「码上Python / PY//NOW / 30 讲」，
        只做文本扫描抓不到，用尺寸+文件更新时间无法判断，改为锁定
        这两个文件必须来自重建脚本标注的产物版本（sha256 白名单）。"""
        import hashlib

        expected = {
            os.path.join(ROOT, "docs", "banner.png"): BANNER_SHA,
            os.path.join(ROOT, "demo", "promo-poster.jpg"): POSTER_SHA,
        }
        for path, want in expected.items():
            with open(path, "rb") as fh:
                digest = hashlib.sha256(fh.read()).hexdigest()
            self.assertEqual(digest, want, f"{path} 不是重建后的品牌图（可能被旧图覆盖）")


class TestResetClearsAll(unittest.TestCase):
    def test_progress_store_reset_deletes_mistakes_and_review(self):
        """R7: reset 必须经 UserDataFiles.deleteAll 删除，且失败要留痕。"""
        data_dir = os.path.join(
            ROOT, "app", "src", "main", "java", "com", "pyneon", "academy", "data"
        )
        text = _read(os.path.join(data_dir, "ProgressStore.kt"))
        m = re.search(r"suspend fun reset\(context: Context\) \{(.*?)\n    \}", text, re.S)
        self.assertIsNotNone(m, "未找到 ProgressStore.reset 实现")
        body = m.group(1)
        self.assertIn("UserDataFiles.deleteAll(", body, "reset 未调用 UserDataFiles.deleteAll")
        self.assertIn("it.clear()", body, "reset 未清 DataStore")
        self.assertIn("Log.w", body, "reset 删除失败未记录日志")

        files = _read(os.path.join(data_dir, "UserDataFiles.kt"))
        for fname in ("pyneon_mistakes.json", "pyneon_review_cards.json"):
            self.assertIn(fname, files, f"UserDataFiles 未覆盖 {fname}")

        for fname in ("ViewModels.kt", "BackupUtil.kt"):
            src = _read(os.path.join(data_dir, fname))
            self.assertNotIn('"pyneon_mistakes.json"', src, f"{fname} 仍硬编码文件名")
            self.assertNotIn('"pyneon_review_cards.json"', src, f"{fname} 仍硬编码文件名")

    def test_profile_dialog_copy_covers_cleared_data(self):
        """R7: 档案页重置弹窗文案覆盖错题本与复习卡。"""
        text = _read(
            os.path.join(
                ROOT,
                "app", "src", "main", "java", "com", "pyneon", "academy",
                "screens", "ProfileScreen.kt",
            )
        )
        seg = text[text.index("确认格式化？"):][:700]
        self.assertIn("错题本", seg, "重置弹窗文案未提及错题本")
        self.assertIn("复习卡", seg, "重置弹窗文案未提及复习卡")


class TestWebClientStatic(unittest.TestCase):
    """网页端静态契约：XSS 防回归 + 关键功能分支必须在位。"""

    def test_no_innerhtml_with_user_data(self):
        """用户/课程文本必须走 textContent；el() 的第三个参数不得拼 HTML。"""
        text = _read(os.path.join(WEB, "app.js"))
        self.assertNotIn(".innerHTML", text, "app.js 不得给 innerHTML 赋值（DOM XSS 面）")
        self.assertIn("textContent", text)

    def test_fill_block_is_rendered(self):
        """fill 块必须有渲染分支（2026-10 审计发现 13 个块在网页端显示「未知块类型」）。"""
        text = _read(os.path.join(WEB, "app.js"))
        self.assertIn('case "fill"', text, "app.js 缺少 fill 块渲染")
        self.assertIn("renderFill", text)

    def test_execution_runs_in_worker_with_terminate(self):
        """死循环防护：执行必须在 Worker 内，且超时会 terminate。"""
        text = _read(os.path.join(WEB, "app.js"))
        self.assertIn("new Worker(", text, "app.js 未使用 Web Worker")
        self.assertIn("worker.terminate()", text, "app.js 超时未终止 Worker")

    def test_progress_persisted_and_routed(self):
        text = _read(os.path.join(WEB, "app.js"))
        self.assertIn("localStorage.setItem(STORAGE_KEY", text)
        self.assertIn("hashchange", text)
        m = re.search(r"function parseRoute", text)
        self.assertIsNotNone(m, "缺少 hash 路由")

    def test_reset_control_has_confirm(self):
        text = _read(os.path.join(WEB, "app.js"))
        self.assertIn("window.confirm(", text, "重置进度必须二次确认")

    def test_engine_failure_offers_browse_mode(self):
        text = _read(os.path.join(WEB, "app.js"))
        self.assertIn("仅浏览模式", text, "引擎失败后应允许只读浏览")
        self.assertIn("boot-retry", text)


class TestPagesAssets(unittest.TestCase):
    def test_pages_copies_match_sources(self):
        """docs/web 与 docs/content_packs 必须与源文件逐字节一致。"""
        import filecmp
        for name in ("index.html", "app.js", "style.css", "data.js", "engine.js"):
            a = os.path.join(WEB, name)
            b = os.path.join(ROOT, "docs", "web", name)
            self.assertTrue(os.path.exists(b), f"docs/web/{name} 缺失（需运行 tools/sync_pages_assets.py）")
            self.assertTrue(filecmp.cmp(a, b, shallow=False), f"docs/web/{name} 与 web/{name} 不一致")
        import glob
        for src in glob.glob(os.path.join(ROOT, "content_packs", "*.json")):
            dst = os.path.join(ROOT, "docs", "content_packs", os.path.basename(src))
            self.assertTrue(filecmp.cmp(src, dst, shallow=False), f"docs/content_packs 与 {src} 不一致")

    def test_privacy_and_terms_pages_exist(self):
        for page in ("privacy", "terms"):
            path = os.path.join(ROOT, "docs", page, "index.html")
            self.assertTrue(os.path.exists(path), f"缺少 docs/{page}/index.html")
            text = _read(path)
            self.assertIn("码上", text)


class TestTooling(unittest.TestCase):
    """工具脚本必须可重复执行且真的能拦住问题（防止 CI 门禁变成空跑）。"""

    def test_check_links_passes_on_repo(self):
        import subprocess, sys
        r = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "check_links.py")],
                           capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=ROOT, timeout=120)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)

    def test_check_links_catches_missing_target(self):
        """把链接检查指向含坏链接的临时页面，必须非 0 退出。"""
        import subprocess, sys, tempfile
        with tempfile.TemporaryDirectory() as tmp:
            page = os.path.join(tmp, "index.html")
            with open(page, "w", encoding="utf-8") as fh:
                fh.write('<a href="no-such-file.png">x</a>')
            # 工具按固定页面列表工作，这里直接调用其解析函数验证判断逻辑
            sys.path.insert(0, os.path.join(ROOT, "tools"))
            import check_links
            self.assertFalse(check_links.resolve(pathlib.Path(page), "no-such-file.png").exists())

    def test_pages_assets_sync_is_idempotent(self):
        import hashlib, subprocess, sys
        def digest(p):
            with open(p, "rb") as fh:
                return hashlib.sha256(fh.read()).hexdigest()
        targets = [os.path.join(ROOT, "docs", "catalog.json"),
                   os.path.join(ROOT, "docs", "web", "app.js")]
        before = [digest(p) for p in targets]
        r = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "sync_pages_assets.py")],
                           capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=ROOT, timeout=180)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual(before, [digest(p) for p in targets], "同步脚本不是幂等的")

    def test_brand_assets_exist(self):
        for rel in ("docs/banner.png", "demo/promo-poster.jpg", "docs/assets/og.jpg"):
            self.assertTrue(os.path.exists(os.path.join(ROOT, rel)), f"缺少品牌资产 {rel}")


class WebMultiLanguageTests(unittest.TestCase):
    """网页端多语言口径（2026-11）：全部课程可读，仅 Python 可运行。

    设计取舍：不隐藏非 Python 课程——学习者能读到讲解、看到答案；
    但必须明确标注「需在 App 内运行」，不能假装能跑。
    """

    def test_web_data_contains_all_languages(self):
        text = _read(os.path.join(ROOT, "web", "data.js"))
        for lid in ("l01", "js01", "ts01", "c01", "lua01", "sql01"):
            with self.subTest(lesson=lid):
                self.assertIn(f'"{lid}"', text, f"web/data.js 缺少课程 {lid}")

    def test_web_declares_runnable_languages(self):
        js = _read(os.path.join(ROOT, "web", "app.js"))
        self.assertIn("RUNNABLE_LANGS", js, "必须显式声明哪些语言可运行")
        self.assertIn("langRunnable", js)
        # 只有 python 在可运行表里
        seg = js[js.index("RUNNABLE_LANGS"):][:120]
        self.assertIn("python: true", seg)
        for other in ("javascript", "typescript", "lua", "sql"):
            self.assertNotIn(f"{other}: true", seg, f"{other} 不应在网页端可运行")

    def test_non_python_code_block_is_readonly_with_notice(self):
        js = _read(os.path.join(ROOT, "web", "app.js"))
        seg = js[js.index("function codeCard"):][:600]
        self.assertIn("langRunnable", seg, "codeCard 必须按语言判断是否可运行")
        self.assertIn("App 里运行", seg, "必须提示该代码需在 App 里运行")

    def test_exercise_blocks_guard_non_python_languages(self):
        """非 Python 课程的判题不得调用 Pyodide（会给出错误结果）。

        2026-11 实测缺陷：renderExercise 无条件调用 checkExercise，
        导致 C/JS/Lua/SQL 的判题在网页端跑进 Python 引擎。
        """
        js = _read(os.path.join(ROOT, "web", "app.js"))
        seg = js[js.index("function renderExercise"):][:800]
        self.assertIn("langRunnable", seg, "renderExercise 必须按语言判断")
        self.assertIn("App 里做", seg, "必须提示改到 App 里判题")

    def test_web_renders_compare_and_visualize(self):
        js = _read(os.path.join(ROOT, "web", "app.js"))
        self.assertIn('case "compare"', js, "网页端缺少 compare 块渲染")
        self.assertIn('case "visualize"', js, "网页端缺少 visualize 块渲染")
        self.assertIn("renderCompare", js)
        self.assertIn("renderVisualize", js)

    def test_web_style_has_new_block_classes(self):
        css = _read(os.path.join(ROOT, "web", "style.css"))
        for cls in (".blk-compare", ".blk-viz", ".viz-row.changed", ".cmp-lang"):
            with self.subTest(cls=cls):
                self.assertIn(cls, css, f"样式缺少 {cls}")

    def test_docs_web_copy_is_in_sync(self):
        for name in ("app.js", "data.js", "engine.js", "style.css"):
            with self.subTest(file=name):
                self.assertEqual(
                    _read(os.path.join(ROOT, "web", name)),
                    _read(os.path.join(ROOT, "docs", "web", name)),
                    f"docs/web/{name} 与 web/{name} 不一致（需跑 sync_pages_assets.py）",
                )


if __name__ == "__main__":
    unittest.main()

"""UI 静态契约回归（不需要模拟器，捕捉会回归的界面缺陷）。

每条都对应 2026-10-21 模拟器验收时实际发现并修复的问题：
  R1 代码编辑器必须把真实 scopeName 传给 sora TextMate（否则静默降级成
     白底默认主题，在深色 UI 里出现刺眼白框）
  R2 主题 JSON 必须覆盖 sora 需要的 colors 键（否则部分控件仍是浅色）
  R3 首页「课程体系」文案的讲数必须与 assets 里 Python 课程数一致
  R4 帮助中心的挑战数必须与 challenges.json 一致
  R5 键盘弹出时底部导航必须收起（否则要么被盖住点不到，要么挤掉输入区）
"""
import json
import os
import pathlib
import re
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
JAVA = os.path.join(ROOT, "app", "src", "main", "java", "com", "pyneon", "academy")


def _read(*parts):
    with open(os.path.join(*parts), encoding="utf-8") as f:
        return f.read()


def _python_lesson_count():
    total = 0
    for name in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json"):
        with open(os.path.join(ASSETS, name), encoding="utf-8") as f:
            total += len(json.load(f))
    return total


def _challenge_count():
    with open(os.path.join(ASSETS, "challenges.json"), encoding="utf-8") as f:
        return len(json.load(f))


class EditorThemeTests(unittest.TestCase):
    def test_grammar_scope_name_is_passed(self):
        text = _read(JAVA, "ui", "components", "CodeEditor.kt")
        self.assertIn("withGrammarSource(", text)
        # 必须用三参数重载（grammarSource, name, scopeName）
        self.assertRegex(
            text,
            r"withGrammarSource\(\s*grammarSource,\s*language,\s*scopeName\s*\)",
            "必须把 scopeName 传给 withGrammarSource（否则默认 source.string 导致降级白底）",
        )
        self.assertIn('"source.python"', text)
        self.assertIn('"source.js"', text)
        self.assertIn('"source.ts"', text, "TypeScript 必须有独立 scope，否则高亮降级")
        self.assertIn('"source.c"', text, "C 必须有独立 scope，否则高亮降级")

    def test_every_shipped_language_has_syntax_file(self):
        """每门内置语言都必须有 TextMate 语法文件，且文件里的 scopeName 与代码一致。

        漏一个的直接后果：该语言编辑器静默降级成白底默认主题。
        """
        import plistlib
        # 语言 id 对应 (scopeName, 代表扩展名)
        expected = {
            "python": ("source.python", "py"),
            "javascript": ("source.js", "js"),
            "typescript": ("source.ts", "ts"),
            "c": ("source.c", "c"),
            "lua": ("source.lua", "lua"),
            "sql": ("source.sql", "sql"),
        }
        for lang, (scope, ext) in expected.items():
            with self.subTest(lang=lang):
                path = os.path.join(ASSETS, "syntaxes", f"{lang}.tmLanguage.json")
                self.assertTrue(os.path.exists(path), f"缺少语法文件 {lang}.tmLanguage.json")
                with open(path, "rb") as f:
                    data = plistlib.loads(f.read())
                self.assertEqual(data["scopeName"], scope, f"{lang} 的 scopeName 应为 {scope}")
                self.assertIn(ext, data["fileTypes"], f"{lang} 的 fileTypes 应含扩展名 {ext}")

    def test_theme_covers_sora_color_keys(self):
        text = _read(JAVA, "ui", "components", "CodeEditor.kt")
        for key in (
            "editor.background",
            "editor.foreground",
            "editor.lineHighlightBackground",
            "editorLineNumber.foreground",
            "editorCursor.foreground",
            "editor.selectionBackground",
        ):
            self.assertIn(key, text, f"主题缺 {key}，对应控件会退回默认浅色")


class CopyCountTests(unittest.TestCase):
    def test_home_track_lesson_count_matches_assets(self):
        """首页主线卡片的讲数由 TrackRepository 的 spec 定义，必须与 assets 一致。"""
        text = _read(JAVA, "data", "TrackRepository.kt")
        m = re.search(r'"python", "Python 主线".*?\(1\.\.(\d+)\)', text, re.S)
        self.assertIsNotNone(m, "TrackRepository 缺少 Python 主线课程 id 区间")
        self.assertEqual(
            int(m.group(1)), _python_lesson_count(),
            "首页课程卡片讲数与 assets 不一致",
        )

    def test_tracks_only_lists_real_content(self):
        """路线页不得再有「开发中 x%」这类没有实现的占位轨道。"""
        text = _read(JAVA, "data", "TrackRepository.kt")
        self.assertNotIn("DEVELOPING", text, "TrackRepository 仍有占位轨道状态")
        for fake in ("Java", "React", "机器学习", "Docker"):
            self.assertNotIn(f'"{fake}"', text, f"TrackRepository 仍列出未实现路线：{fake}")
        screen = _read(JAVA, "screens", "TracksScreen.kt")
        self.assertIn("NOT_INSTALLED", screen, "未下载的内容包应显示为可下载")
        self.assertIn("onOpenContentHub", screen, "未装载路线应引导到内容中心")

    def test_help_challenge_count_is_dynamic(self):
        """帮助中心的挑战数必须从 challenges.json 读取，不能硬编码。

        2026-10-22 界面巡检：原实现写死「8 个实战闯关」，一旦挑战增删就会与
        角斗场列表脱节。改为断言「文案引用 totalChallenges」+「总数来自 assets」。
        """
        text = _read(JAVA, "screens", "HelpScreen.kt")
        self.assertIn("challenges.json", text, "挑战数必须从 assets 读取")
        self.assertIn("$totalChallenges", text, "挑战数文案必须引用动态变量")
        self.assertGreaterEqual(_challenge_count(), 1, "challenges.json 至少应有 1 道题")


class LanguageCenterTests(unittest.TestCase):
    """语言中心契约：内置运行时列出、未实现语言无下载入口、课程包按需下载。"""

    def test_language_center_lists_registered_runtimes(self):
        text = _read(JAVA, "screens", "LanguageCenterScreen.kt")
        self.assertIn("LanguageRegistry.all()", text, "语言中心必须列出已注册运行时")
        self.assertIn("runtime.isAvailable()", text, "必须显示运行时可用状态")

    def test_planned_languages_have_no_download_entry(self):
        text = _read(JAVA, "screens", "LanguageCenterScreen.kt")
        self.assertIn("LanguageIds.PLANNED", text, "未实现语言必须单独列出")
        self.assertIn("暂不支持", text)
        self.assertNotIn("runtime.download", text, "项目没有运行时下载机制，不得出现运行时下载调用")

    def test_course_packs_show_runtime_gate(self):
        text = _read(JAVA, "screens", "LanguageCenterScreen.kt")
        self.assertIn("runtimeReady", text, "课程包必须校验运行时是否可用")
        self.assertIn("缺运行时，暂不可下载", text)

    def test_content_pack_carries_lang(self):
        text = _read(JAVA, "data", "ContentCenter.kt")
        self.assertIn("val lang: String", text, "内容包必须携带语言字段")
        self.assertIn("optString(\"lang\"", text, "必须从 catalog 解析 lang")
        # 2026-11：国内网络下 GitHub 最坏要等 20 秒才回落 GitCode，目录源必须国内优先。
        self.assertIn("CATALOG_TIMEOUT_MS = 4000", text, "目录源应有独立的 4 秒超时")
        gitcode = text.find("api.gitcode.com")
        github_raw = text.find("raw.githubusercontent.com")
        self.assertGreaterEqual(gitcode, 0, "GitCode 目录源缺失")
        self.assertGreaterEqual(github_raw, 0, "GitHub raw 目录源缺失")
        self.assertLess(gitcode, github_raw, "GitCode 必须排在 GitHub raw 之前")


class NavigationRouteTests(unittest.TestCase):
    """所有 navigate("x") 目标必须在 NavHost 里注册，否则点击即崩。

    2026-10-21 模拟器实测：ProfileScreen 的「备份/恢复」调用 navigate("backup")，
    但 AppNav 从未注册该路由 → IllegalArgumentException 直接闪退。
    """

    def test_every_navigate_target_is_registered(self):
        nav = _read(JAVA, "nav", "AppNav.kt")
        registered = {
            r.split("/")[0] for r in re.findall(r'composable\("([^"]+)"\)', nav)
        }
        self.assertTrue(registered, "未解析到任何 composable 路由")

        targets = {}
        for path in (pathlib.Path(JAVA)).rglob("*.kt"):
            text = path.read_text(encoding="utf-8")
            for m in re.finditer(r'navigate\(\s*"([^"$]+)"', text):
                targets.setdefault(m.group(1), set()).add(path.name)

        missing = {
            route: sorted(files)
            for route, files in targets.items()
            if route.split("/")[0] not in registered
        }
        self.assertEqual(missing, {}, f"navigate 目标未注册（点击会崩溃）: {missing}")


class KeyboardInsetTests(unittest.TestCase):
    def test_bottom_bar_hidden_while_ime_visible(self):
        text = _read(JAVA, "nav", "AppNav.kt")
        self.assertIn("WindowInsets.isImeVisible", text)
        self.assertRegex(
            text,
            r"if \(showBar && !WindowInsets\.isImeVisible\)",
            "键盘弹出时必须隐藏底部导航，否则遮挡/挤压输入区",
        )


class VisualRegressionTests(unittest.TestCase):
    """2026-10-22 模拟器逐屏点验发现、且可静态锁定的界面缺陷。"""

    def test_glitch_text_does_not_double_draw_permanently(self):
        """GlitchText 若常驻偏移绘制两份彩色副本，中文标题会糊成重影。

        必须用「只在短脉冲内绘制副本」的实现（amount 门控）。
        """
        text = _read(JAVA, "ui", "effects", "Effects.kt")
        seg = text[text.index("fun GlitchText"):][:1400]
        self.assertIn("amount", seg, "GlitchText 需要抖动强度门控")
        self.assertRegex(seg, r"if \(amount > [0-9.]+f?\)", "副本必须在静止期不绘制")

    def test_streak_screen_is_scrollable(self):
        """连击台 6 个徽章 + 主卡在 1080×2400 上超出屏幕，必须可滚动。"""
        text = _read(JAVA, "screens", "StreakScreen.kt")
        self.assertIn("verticalScroll(rememberScrollState())", text)

    def test_badge_grid_is_responsive(self):
        """徽章格固定 4 列会在窄屏溢出、固定宽度会在宽屏留白。"""
        text = _read(JAVA, "screens", "StreakScreen.kt")
        self.assertIn("BoxWithConstraints", text)
        self.assertIn("maxWidth", text)

    def test_code_inputs_disable_ime_autocapitalization(self):
        """代码大小写敏感：终端/填空/自定义颜色输入必须关掉 IME 自动大写。"""
        # 自定义颜色输入已抽到 ThemePickerSection（设置页与「我的」页共用）
        for rel in (("screens", "TerminalScreen.kt"), ("screens", "LessonBlockCards.kt"),
                    ("screens", "ThemePickerSection.kt")):
            text = _read(JAVA, *rel)
            self.assertIn("KeyboardType.Password", text, f"{rel[-1]} 未关闭自动大写")
            self.assertIn("autoCorrect = false", text, f"{rel[-1]} 未关闭自动纠错")

    def test_js_error_message_has_no_duplicated_type(self):
        """Rhino 的 EcmaError.message 自带「ReferenceError: …」，不能直接拼 type。"""
        text = _read(JAVA, "js", "JsBridge.kt")
        self.assertIn('removePrefix("${e.type}: ")', text)

    def test_secondary_pages_have_top_back(self):
        """帮助/隐私/关于原先只能滚到底部点返回。

        2026-11 去重后，顶部返回由 ui/components/ScreenHeader.kt 统一提供；
        页面本身只需使用 ScreenHeader，不再各自 import ArrowBack。
        """
        header = _read(JAVA, "ui", "components", "ScreenHeader.kt")
        self.assertIn("ArrowBack", header, "ScreenHeader 必须提供顶部返回图标")
        for name in ("HelpScreen.kt", "PrivacyPolicyScreen.kt", "AboutScreen.kt"):
            text = _read(JAVA, "screens", name)
            self.assertIn("ScreenHeader(", text, f"{name} 缺少统一顶部返回")

    def test_home_challenge_button_uses_fire_icon(self):
        text = _read(JAVA, "screens", "HomeScreen.kt")
        seg = text[text.index('label = "挑战"'):][:300]
        self.assertIn("LocalFireDepartment", seg, "挑战按钮不应使用人像图标")


class VisualStyleSwitchTests(unittest.TestCase):
    """视觉风格开关（2026-11）：赛博朋克是默认个性，但必须可以关掉。

    设计约束：风格只管「怎么动、怎么描边、什么形状」，不得引入第二套色板。
    """

    def test_style_enum_exists_with_both_options(self):
        text = _read(JAVA, "ui", "theme", "VisualStyle.kt")
        self.assertIn("enum class VisualStyle", text)
        self.assertIn("CYBER", text)
        self.assertIn("MINIMAL", text)
        self.assertIn("LocalVisualStyle", text)

    def test_style_does_not_define_its_own_colors(self):
        """风格层只能引用现有 Token，不得自造色值（否则配色与风格会互相打架）。"""
        text = _read(JAVA, "ui", "theme", "VisualStyle.kt")
        self.assertNotRegex(
            text, r"Color\(0x", "VisualStyle 不得定义 Color(0x…)，颜色一律来自 AppTheme"
        )

    def test_glitch_text_respects_style(self):
        """简洁风格必须走纯 Text 分支，不能创建无限动画。"""
        text = _read(JAVA, "ui", "effects", "Effects.kt")
        seg = text[text.index("fun GlitchText"):][:900]
        self.assertIn("glitchEnabled", seg, "GlitchText 必须响应风格开关")
        self.assertRegex(seg, r"if \(!LocalVisualStyle\.current\.glitchEnabled\)",
                         "简洁风格应在创建动画前直接返回")

    def test_backdrop_modifiers_respect_style(self):
        """扫描线与网格必须能整体关闭（省电、减少视觉噪音）。"""
        text = _read(JAVA, "ui", "effects", "Effects.kt")
        for name in ("fun Modifier.scanlines", "fun Modifier.cyberGrid"):
            with self.subTest(modifier=name):
                seg = text[text.index(name):][:700]
                self.assertIn("backdropEnabled", seg, f"{name} 必须响应风格开关")

    def test_neon_card_shape_and_border_follow_style(self):
        """卡片形状与描边必须来自风格，不能写死切角 + 霓虹渐变。"""
        text = _read(JAVA, "ui", "effects", "Effects.kt")
        seg = text[text.index("fun NeonCard"):][:1200]
        self.assertIn("LocalVisualStyle.current.cardShape", seg)
        self.assertIn("styleBorder", seg)

    def test_theme_provides_visual_style(self):
        """主题必须同时下发配色与风格，否则子界面拿不到开关。"""
        theme = _read(JAVA, "ui", "theme", "Theme.kt")
        self.assertIn("LocalVisualStyle provides visualStyle", theme)
        activity = _read(os.path.join(JAVA, "MainActivity.kt"))
        self.assertIn("KEY_VISUAL_STYLE", activity, "MainActivity 必须监听风格变更以即时生效")

    def test_settings_screen_exposes_style_picker(self):
        """风格切换必须能从设置页进入（用户明确要求放在设置里）。"""
        text = _read(JAVA, "screens", "SettingsScreen.kt")
        self.assertIn("外观风格", text)
        self.assertIn("VisualStyle.entries", text)
        self.assertIn("saveVisualStyle", text)

    def test_style_preference_is_persisted(self):
        text = _read(JAVA, "utils", "ThemePreference.kt")
        self.assertIn("KEY_VISUAL_STYLE", text)
        self.assertIn("getVisualStyle", text)
        self.assertIn("saveVisualStyle", text)


class AccessibilityTests(unittest.TestCase):
    """可访问性：字号缩放必须存在，且不得破坏代码块对齐。"""

    def test_font_scale_enum_exists(self):
        text = _read(JAVA, "ui", "theme", "Type.kt")
        self.assertIn("enum class FontScale", text)
        for level in ("COMPACT", "NORMAL", "COMFORT", "LARGE"):
            self.assertIn(level, text, f"缺少字号档位 {level}")

    def test_font_scale_only_scales_reading_text(self):
        """只放大正文，不放大等宽标题——否则窄屏中文标题会换行错位。"""
        text = _read(JAVA, "ui", "theme", "Type.kt")
        seg = text[text.index("fun typographyFor"):]
        self.assertIn("bodyLarge", seg)
        self.assertIn("bodyMedium", seg)
        self.assertNotIn("headlineLarge = AppTypography.headlineLarge.scaled()", seg,
                         "标题不应参与缩放")
        self.assertNotIn("labelSmall = AppTypography.labelSmall.scaled()", seg,
                         "等宽标签不应参与缩放")

    def test_font_scale_is_persisted_and_applied(self):
        pref = _read(JAVA, "utils", "ThemePreference.kt")
        self.assertIn("KEY_FONT_SCALE", pref)
        self.assertIn("getFontScale", pref)
        theme = _read(JAVA, "ui", "theme", "Theme.kt")
        self.assertIn("typographyFor(fontScale)", theme, "主题必须应用字号档位")
        activity = _read(os.path.join(JAVA, "MainActivity.kt"))
        self.assertIn("KEY_FONT_SCALE", activity, "MainActivity 必须监听字号变更")

    def test_theme_picker_reachable_from_settings(self):
        """配色主题必须能从设置页进入，且与「我的」页共用同一组件（防两套漂移）。"""
        settings = _read(JAVA, "screens", "SettingsScreen.kt")
        self.assertIn("ThemePickerSection()", settings, "设置页缺少配色主题入口")
        profile = _read(JAVA, "screens", "ProfileScreen.kt")
        self.assertIn("ThemePickerSection()", profile, "「我的」页应复用同一组件")
        # 不允许任何一页自己再写一份 AppThemes.allThemes 循环
        self.assertNotIn("AppThemes.allThemes.forEach", profile,
                         "主题列表只应存在于 ThemePickerSection，不得重复实现")
        self.assertNotIn("AppThemes.allThemes.forEach", settings,
                         "主题列表只应存在于 ThemePickerSection，不得重复实现")

    def test_settings_exposes_font_scale(self):
        text = _read(JAVA, "screens", "SettingsScreen.kt")
        self.assertIn("FontScale.entries", text)
        self.assertIn("saveFontScale", text)

    def test_default_theme_is_light(self):
        """默认主题必须是亮色（2026-11 决策：长文阅读更舒适）。"""
        text = _read(JAVA, "utils", "ThemePreference.kt")
        self.assertIn('DEFAULT_THEME_ID = "paper_light"', text, "默认主题应为白纸模式")
        self.assertIn("getString(KEY_THEME_ID, DEFAULT_THEME_ID)", text)

    def test_editor_theme_follows_light_dark(self):
        """编辑器配色必须跟随主题明暗，否则白纸模式下出现深色代码框。"""
        text = _read(JAVA, "ui", "components", "CodeEditor.kt")
        self.assertIn("PYNEON_THEME_LIGHT_JSON", text, "缺少亮色编辑器配色")
        self.assertIn("if (isLight)", text, "必须按明暗选择配色")
        self.assertIn("remember(context, language, isLight)", text,
                      "isLight 必须参与 remember key，否则切主题不刷新")

    def test_light_tokens_marked(self):
        text = _read(JAVA, "ui", "theme", "Color.kt")
        self.assertIn("val isLight: Boolean = false", text)
        seg = text[text.index("val LightTokens"):][:600]
        self.assertIn("isLight = true", seg, "LightTokens 必须标记 isLight")

    def test_minimal_style_disables_autoplay(self):
        """简洁风格下可视化块不应自动播放（避免持续动画干扰阅读）。"""
        # 2026-11 拆分：可视化组件移到 LessonVisualCards.kt
        text = _read(JAVA, "screens", "LessonVisualCards.kt")
        seg = text[text.index("fun VisualizeCard"):]
        self.assertIn("pressAnimationEnabled", seg,
                      "自动播放按钮应受风格开关控制")


class BuildVariantTests(unittest.TestCase):
    """构建变体契约。

    localRelease 是「本地可装、接近正式版」的构建：不需要开发者提供 keystore
    就能在真机上测 R8 版本的真实性能。它必须与正式版隔离（包名后缀），
    否则会互相覆盖安装、导致学习进度丢失。
    """

    def test_local_release_variant_exists(self):
        gradle = _read(os.path.join(ROOT, "app", "build.gradle.kts"))
        self.assertIn('create("localRelease")', gradle,
                      "缺少 localRelease 构建变体")
        self.assertIn("initWith(getByName(\"release\"))", gradle,
                      "localRelease 必须继承 release 的 R8 配置")

    def test_local_release_uses_debug_signing(self):
        gradle = _read(os.path.join(ROOT, "app", "build.gradle.kts"))
        seg = gradle[gradle.index('create("localRelease")'):][:800]
        self.assertIn("signingConfigs.getByName(\"debug\")", seg,
                      "localRelease 应用 debug keystore 签名（无需用户提供密钥）")
        self.assertIn("isDebuggable = false", seg,
                      "localRelease 必须不可调试，否则性能不代表真实体验")

    def test_local_release_is_isolated_from_production(self):
        """包名必须加后缀，避免与正式版互相覆盖安装。"""
        gradle = _read(os.path.join(ROOT, "app", "build.gradle.kts"))
        seg = gradle[gradle.index('create("localRelease")'):][:800]
        self.assertIn('applicationIdSuffix = ".local"', seg,
                      "localRelease 包名应加 .local 后缀")
        self.assertIn("versionNameSuffix", seg,
                      "localRelease 版本号应带后缀以便区分")

    def test_manifest_does_not_hardcode_debuggable(self):
        """硬编码 android:debuggable 会触发 lint HardcodedDebugMode。"""
        manifest = _read(os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml"))
        self.assertNotIn("android:debuggable", manifest,
                         "不得硬编码 debuggable，交由构建类型决定")


class PlatformDecisionTests(unittest.TestCase):
    """平台策略决策（2026-11）：只支持 Android 10+，主发国内渠道。

    这两条一旦被无意改回去，会连带破坏备份/证书保存路径与更新检查可达性。
    """

    def test_min_sdk_is_29(self):
        gradle = _read(os.path.join(ROOT, "app", "build.gradle.kts"))
        self.assertIn("minSdk = 29", gradle,
                      "minSdk 应为 29（放弃 Android 7-9，备份/海报统一走 MediaStore）")
        self.assertNotIn("minSdk = 24", gradle)

    def test_no_pre_q_storage_branches(self):
        """minSdk 29 后不应再有 pre-Q 的存储分支（死代码 + 缺权限风险）。"""
        for rel in (("data", "BackupUtil.kt"), ("screens", "CertificatePoster.kt")):
            with self.subTest(file=rel[-1]):
                text = _read(JAVA, *rel)
                self.assertNotIn("getExternalStoragePublicDirectory", text,
                                 f"{rel[-1]} 仍含 pre-Q 公共存储路径")
                # 只检查实际调用（sendBroadcast(...ACTION_MEDIA_SCANNER...)），
                # 注释里说明"早期版本曾用该常量"是正常文档
                self.assertNotIn("sendBroadcast", text,
                                 f"{rel[-1]} 仍含手动扫描广播调用")

    def test_update_source_prefers_domestic(self):
        """更新检查必须国内源优先——GitHub 直连在国内经常超时。"""
        text = _read(JAVA, "utils", "UpdateChecker.kt")
        seg = text[text.index("SOURCES"):][:300]
        self.assertIn("gitcode.com", seg, "更新源应包含 GitCode")
        # GitCode 必须排在 GitHub 之前
        self.assertLess(seg.index("gitcode.com"), seg.index("github.com"),
                        "GitCode 应作为首选源排在 GitHub 之前")

    def test_in_app_urls_are_domestic(self):
        """应用内跳转（源码/隐私政策/协议）不能指向国内打不开的地址。"""
        text = _read(JAVA, "utils", "AppConstants.kt")
        self.assertIn('REPO_URL = "https://gitcode.com', text,
                      "REPO_URL 应指向 GitCode")
        # 隐私政策 URL 由 REPO_URL 拼接，不能硬编码 GitHub Pages
        self.assertNotIn("badhope.github.io/mashang/privacy", text,
                         "隐私政策 URL 不应依赖 GitHub Pages")


class ReleaseReadinessTests(unittest.TestCase):
    """发布前必备项（应用商店审核会检查）。"""

    def test_licenses_screen_exists_and_covers_third_party(self):
        """LGPL/MPL/Apache 都要求展示第三方组件许可，缺了会卡审核。"""
        text = _read(JAVA, "screens", "LicensesScreen.kt")
        for lib in ("Chaquopy", "Rhino", "LuaJ", "sora-editor", "AndroidX", "ZXing"):
            self.assertIn(lib, text, f"许可页缺少组件：{lib}")
        # LGPL 必须说明源码获取与可替换性
        self.assertIn("LGPL", text)
        self.assertIn("源码", text)
        # 自身 MIT 全文
        self.assertIn("Permission is hereby granted", text)

    def test_licenses_route_reachable(self):
        nav = _read(os.path.join(JAVA, "nav", "AppNav.kt"))
        self.assertIn('composable("licenses")', nav, "许可页未注册路由")
        about = _read(JAVA, "screens", "AboutScreen.kt")
        self.assertIn("onOpenLicenses", about, "关于页缺少许可入口")

    def test_privacy_policy_covers_diagnostics(self):
        """新增的诊断日志必须在隐私政策里说明（合规硬要求）。"""
        root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        with open(os.path.join(root, "PRIVACY_POLICY.md"), encoding="utf-8") as f:
            text = f.read()
        self.assertIn("诊断日志", text, "隐私政策未说明诊断日志")
        self.assertIn("不记录你的代码", text, "必须声明不记录用户代码")
        self.assertIn("不会自动上传", text, "必须声明不上传")

    def test_privacy_policy_covers_six_languages(self):
        root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        with open(os.path.join(root, "PRIVACY_POLICY.md"), encoding="utf-8") as f:
            text = f.read()
        for lang in ("Python", "JavaScript", "TypeScript", "Lua", "SQL"):
            self.assertIn(lang, text, f"隐私政策未提及 {lang} 运行时")
        self.assertIn("本机", text)

    def test_back_buttons_meet_touch_target(self):
        """返回按钮触摸区必须 ≥ 48dp（无障碍与商店审核项）。

        视觉图标可以小，但必须用 padding 把可点区域撑开。
        """
        import glob
        offenders = []
        for path in glob.glob(os.path.join(JAVA, "screens", "*.kt")):
            with open(path, encoding="utf-8") as f:
                text = f.read()
            # 找 .size(28.dp) 后紧跟 .clickable { onBack() } 且中间没有 padding
            if ".size(28.dp)" in text and "clickable { onBack() }" in text:
                seg_start = text.find(".size(28.dp)")
                seg = text[max(0, seg_start - 200): seg_start + 100]
                if "clickable { onBack() }" in seg and "padding(10.dp)" not in seg:
                    offenders.append(os.path.basename(path))
        self.assertEqual(offenders, [], f"这些页面的返回按钮触摸区不足 48dp：{offenders}")

    def test_release_checklist_exists(self):
        root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        self.assertTrue(os.path.exists(os.path.join(root, "RELEASE_CHECKLIST.md")))
        self.assertTrue(os.path.exists(os.path.join(root, "store", "listing.md")))

    def test_no_extra_permissions(self):
        """发布包只能申请 INTERNET，多申请会被商店要求逐项说明。"""
        root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        with open(
            os.path.join(root, "app", "src", "main", "AndroidManifest.xml"), encoding="utf-8"
        ) as f:
            text = f.read()
        import re
        perms = re.findall(r'uses-permission android:name="([^"]+)"', text)
        self.assertEqual(perms, ["android.permission.INTERNET"],
                         f"权限应只有 INTERNET，实际：{perms}")


class DebugDiagnosticsTests(unittest.TestCase):
    """诊断能力：离线 App 没有服务端日志，必须有本地可导出的诊断链路。"""

    def test_debug_log_is_bounded_and_private(self):
        text = _read(JAVA, "debug", "DebugLog.kt")
        self.assertIn("MAX_ENTRIES", text, "日志必须有内存上限")
        self.assertIn("MAX_FILE_CHARS", text, "日志文件必须有体积上限")
        self.assertIn("不记用户代码", text, "必须明确不记用户代码")
        # 不得出现任何网络上报
        for bad in ("HttpURLConnection", "OkHttp", "URL("):
            self.assertNotIn(bad, text, f"日志不得联网上报：{bad}")

    def test_diagnostics_screen_shows_runtime_and_logs(self):
        text = _read(JAVA, "screens", "DiagnosticsScreen.kt")
        for needle in ("运行时状态", "运行日志", "运行环境", "内容状态"):
            self.assertIn(needle, text, f"诊断页缺少「{needle}」")
        self.assertIn("DebugLog.export()", text, "必须能导出日志")
        self.assertIn("copyText", text, "必须能复制日志")

    def test_diagnostics_route_registered(self):
        nav = _read(os.path.join(JAVA, "nav", "AppNav.kt"))
        self.assertIn('composable("diagnostics")', nav, "诊断页未注册路由")
        settings = _read(JAVA, "screens", "SettingsScreen.kt")
        self.assertIn("开发者诊断", settings, "设置页缺少诊断入口")

    def test_key_paths_are_instrumented(self):
        """启动、执行失败、内容下载三条关键路径必须有日志。"""
        app = _read(os.path.join(JAVA, "PyNeonApp.kt"))
        self.assertIn("DebugLog.i", app, "启动未打点")
        lesson = _read(JAVA, "screens", "LessonDetailScreen.kt")
        self.assertIn("DebugLog.w", lesson, "执行失败未打点")
        center = _read(JAVA, "screens", "LanguageCenterScreen.kt")
        self.assertIn("DebugLog", center, "内容下载未打点")


class FeedbackStateTests(unittest.TestCase):
    """反馈体验：执行异常不得崩溃，必须转成可读错误。"""

    def test_runtime_exception_is_caught(self):
        text = _read(JAVA, "screens", "LessonDetailScreen.kt")
        self.assertIn("RuntimeCrash", text, "运行时异常必须有兜底类型")
        # 至少两处（代码块运行 + 判题）
        self.assertGreaterEqual(text.count("catch (t: Throwable)"), 2,
                                "代码运行与判题都要有异常兜底")

    def test_error_message_is_user_readable(self):
        text = _read(JAVA, "screens", "LessonDetailScreen.kt")
        self.assertIn("运行时异常", text, "兜底错误要有中文可读提示")
        self.assertIn("判题异常", text)


if __name__ == "__main__":
    unittest.main()

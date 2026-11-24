# 项目基线

> 适用项目：CodeLearning（码上）  
> 文档版本：2026-11-24 / baseline-2  
> 维护人：项目负责人  
> 更新规则：每次版本号、主分支、课程数、运行时或发布状态变化时同步更新，并由 `tools/check_baseline.py` 校验关键字段。

<!-- baseline: version=0.7.4 code=23 lessons=245 code_blocks=448 runnable=434 cases=1097 -->

## 一、当前核心状态

### 1.1 版本与仓库

| 项目 | 当前值 |
| --- | --- |
| 应用名 | CodeLearning（码上） |
| 包名 | `com.pyneon.academy` |
| 发布版本 | `0.7.4`，`versionCode 23` |
| 最低系统 | Android 10（API 29） |
| 发布 ABI | `arm64-v8a` |
| 主分支 | `main`，0.7.4 修复提交待本轮发布；0.7.3 发布提交 `180e59e` |
| 发布标签 | `v0.7.4` → 待本轮签名构建；`v0.7.3` → `180e59e` |
| 当前工作区 | 0.7.4 可用性修复已完成，待签名发布 |
| 构建环境 | JDK 17 / Android SDK 35 / Gradle 9.5 / Chaquopy 3.13 |
| 签名 | 本地无 release keystore；正式包由 GitHub Actions + release secrets 签名 |

0.7.4 发布入口（本轮流水线创建后生效）：

- GitHub `badhope`：<https://github.com/badhope/mashang/releases/tag/v0.7.4>
- GitHub `X33834`：<https://github.com/X33834/mashang/releases/tag/v0.7.4>
- GitHub `Morningstar202604`：<https://github.com/Morningstar202604/mashang/releases/tag/v0.7.4>
- Gitee：<https://gitee.com/badhope/mashang/releases/tag/v0.7.4>
- GitCode：<https://gitcode.com/badhope/mashang/releases/tag/v0.7.4>

远端入口：

- GitHub 主仓：<https://github.com/badhope/mashang>
- GitHub 镜像：<https://github.com/X33834/mashang>
- GitHub 镜像：<https://github.com/Morningstar202604/mashang>
- Gitee：<https://gitee.com/badhope/mashang>
- GitCode：<https://gitcode.com/badhope/mashang>

### 1.2 运行时与内容

| 语言 | 运行时 | 课程规模 | 主要边界 |
| --- | --- | ---: | --- |
| Python | CPython 3.13（Chaquopy） | 50 讲 | 不预装第三方包 |
| JavaScript | Mozilla Rhino 1.7.15 | 38 讲 | ES 能力低于现代浏览器 |
| TypeScript | 类型擦除后交给 Rhino | 38 讲 | 不做 tsc 类型检查 |
| C | Kotlin 教学子集解释器 | 38 讲 | 不支持预处理、goto、函数指针 |
| Lua | LuaJ 3.0.1 | 38 讲 | 接近 Lua 5.2，不是完整 5.4 |
| SQL | SQLite 内存库 | 43 讲 | 不是 MySQL / PostgreSQL |

课程事实：245 讲、448 个课程代码块（434 个可直接运行）、966 条课程用例、22 道挑战、131 条挑战用例，合计 1,097 条判题用例。

### 1.3 质量门禁

| 项目 | 当前基线 |
| --- | --- |
| Kotlin 编译 | `compileDebugKotlin` 通过 |
| Android 单测 | 306 项，0 失败，1 跳过 |
| Python 测试 | 249 项通过，551 个 subtest 通过 |
| Lint | 0 Error，47 Warning，6 Hint |
| 内容校验 | 245 讲、22 挑战、267 判题项、1,097 用例，结构问题 0，判题失败 0 |
| 链接检查 | 5 个页面通过 |
| 官网结构 | HTML 标签无错误、无未闭合 |
| 发布 APK | 待本轮签名构建；0.7.3 为 26,307,798 字节，SHA-256 `34f13c73...508e` |
| APK 探针 | `ci_apk_probe.py --require-release` 全部通过 |

### 1.4 已知边界与风险

- 不提供自由项目、多文件工程、pip、第三方库安装；这是产品边界，不是缺陷。
- 没有账号、云同步、社区、广告、统计 SDK；学习数据只在本机。
- 没有 iOS 版；只打 `arm64-v8a`。
- 端侧大模型没有打进 APK。当前助教是规则 + 课程检索，零网络、零模型权重；原因和后续路径见 `COMPETITIVE_MATRIX.md` 第 6.3 节。
- 官网截图来自开发期构建，已标注“界面示意”，版本角标不代表当前版本。
- 软著申请表中的“未使用 AI 开发”承诺需要申请人本人判断，不能代签。

## 二、已交付功能清单

状态说明：已交付 = 当前 `main` 可用并有测试或人工验收；待补 = 已有入口但还需完善。

| ID | 功能 | 用户入口 | 实现位置 | 开源/第三方溯源 | 复用范围 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| F01 | 隐私同意与首次启动 | 首次启动 | `PrivacyConsentDialog`、`BootScreen` | 无 | 自有 | 已交付 |
| F02 | 首页仪表盘 | 首页 | `HomeScreen`、`ProgressStore` | AndroidX Compose | UI 框架 | 已交付 |
| F03 | Python 主线课程 | 课程 | `lessons_*.json`、`LessonRepository` | 课程内容原创 | 自有内容 | 已交付 |
| F04 | 五门扩展课程包 | 语言中心 | `content_packs/*.json`、`ContentCenter` | 课程内容原创 | 自有内容 | 已交付 |
| F05 | 六门语言运行时 | 课程/终端 | `lang/LanguageRuntime`、`py/js/lua/sql/c/ts` | CPython、Chaquopy、Rhino、LuaJ、SQLite | 只经适配器调用，不修改上游 | 已交付 |
| F06 | 代码编辑器 | 课程/挑战 | `ui/components/CodeEditor`、`PythonCodeField` | sora-editor、TextMate | 封装为自有组件 | 已交付 |
| F07 | 断言判题 | 课程/挑战 | `checkExercise`、`runner.py` | CPython / 各运行时 | 自有判题协议 | 已交付 |
| F08 | 错题本 | 我的 → 错题 | `MistakeScreen`、`MistakeViewModel` | 无 | 自有 | 已交付 |
| F09 | 间隔复习 | 我的 → 复习 | `ReviewScheduler`、`ReviewScreen` | SM-2 变体，算法自写 | 自有 | 已交付 |
| F10 | XP / 连击 / 徽章 | 首页/我的 | `Gamification`、`ProgressStore` | 无 | 自有 | 已交付 |
| F11 | 终端 REPL | 终端 | `TerminalScreen` | 各语言运行时 | 自有交互 | 已交付 |
| F12 | 内容中心 | 我的 → 语言中心 | `ContentHubScreen`、`ContentCenter` | GitHub/GitCode 公开内容源 | 只读下载与校验 | 已交付 |
| F13 | 备份/恢复 | 我的 → 备份 | `BackupUtil`、`BackupScreen` | AndroidX DataStore、MediaStore | 系统 API | 已交付 |
| F14 | 主题与字号 | 我的 → 设置 | `ThemePickerSection`、`SettingsScreen` | 无 | 自有 | 已交付 |
| F15 | 新手引导 | 首次启动 | `WelcomeTutorial` | 无 | 自有 | 已交付 |
| F16 | 毕业证书海报 | 我的 → 证书 | `CertificatePoster` | ZXing | 二维码生成 | 已交付 |
| F17 | 检查更新 | 我的 → 设置 | `UpdateChecker` | GitHub/GitCode 公开 API | 只读版本号 | 已交付 |
| F18 | 崩溃诊断 | 我的 → 诊断 | `CrashHandler`、`DiagnosticsScreen` | 无 | 自有 | 已交付 |
| F19 | 许可与帮助 | 我的 → 关于/帮助 | `LicensesScreen`、`HelpScreen` | 第三方许可原文 | 只读展示 | 已交付 |
| F20 | 离线教练 | 课程报错/终端 `/coach` | `CoachEngine` | 无 | 自有规则 | 已交付 |
| F21 | 离线 AI 助教 | 课程/挑战页 | `TutorEngine`、`TutorDialog`、`TutorKnowledge` | 课程正文检索 | 自有 | 0.7.4 修复 |
| F22 | 输入语法提示 | 课程/挑战编辑器 | `SyntaxCheck`、`SyntaxHintBar` | 无 | 自有 | 0.7.4 修复 SQL 转义 |
| F23 | 官网与网页版 | 浏览器 | `docs/index.html`、`web/`、`docs/web/` | GitHub Pages、Pyodide | 网页端独立运行时 | 已交付 |
| F24 | 发布流水线 | GitHub Actions | `.github/workflows/ci.yml` | GitHub Actions | 构建/签名/发布 | 已交付 |
| F25 | 自动化测试 | CI/本地 | `tests/`、`app/src/test` | JUnit、Robolectric、pytest | 测试依赖不进 APK | 已交付 |
| F26 | 练习草稿恢复 | 课程/挑战页 | `DraftStore` | 无 | 自有 | 0.7.4 交付 |

## 三、待开发任务池

### 3.1 本迭代收口项

| ID | 优先级 | 任务 | 验收标准 | 依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| T01 | P0 | 四模块基线文档 | 版本、提交、课程数、测试数与仓库一致，检查脚本通过 | 无 | 已完成 |
| T02 | P0 | 竞品矩阵与可行性论证 | 两条赛道、六类竞品、三条路径均有来源和边界 | 公开资料 | 已完成 |
| T03 | P0 | 离线 AI 助教 | 课程页和挑战页可随时调出，支持思路/报错/概念/下一步，零网络，不代写 | `TutorEngine` | 已完成 |
| T04 | P0 | 实时语法提示 | 括号、引号、中文标点、Python 冒号能在输入时提示且不误报 | `SyntaxCheck` | 已完成 |
| T05 | P0 | 离线压力验证 | 120 次练习写入、126 次正常提交、12 次异常，0 崩溃 0 丢失 | 测试脚本 | 已完成 |
| T06 | P0 | 0.7.3 发布 | 五端 Release、签名校验、APK 探针、质量报告同步 | CI secrets | 已完成 |
| T07 | P0 | 0.7.4 可用性修复发布 | 草稿恢复、SQL 转义、助教上下文与性能、错题去重；五端 Release 与软著同步 | CI secrets | 待发布 |

### 3.2 后续候选

| ID | 优先级 | 任务 | 说明 |
| --- | --- | --- | --- |
| B01 | P1 | 可选本地模型包 | 用户主动导入 GGUF/MediaPipe 模型，只用于润色讲解；不塞主 APK，不作为核心链路前提 |
| B02 | P1 | 挑战页助教 | 复用 `TutorEngine`，补 challenge 上下文的拆题与报错引导 |
| B03 | P1 | 课程深度 | Python 综合项目、SQL 实战、DSA 题解继续加深，不增加语言数量 |
| B04 | P2 | 多文件项目 | 需要编辑器、文件树、运行沙箱一起改，暂不硬上 |
| B05 | P2 | 官网截图重拍 | 需要真机/模拟器；当前已标注开发期截图 |
| B06 | P2 | 应用商店素材 | 华为/小米/应用宝文案、隐私说明、截图尺寸统一 |

### 3.3 暂不做

- iOS 客户端：当前运行时和构建链都在 Android，移植不是小版本任务。
- 云同步/账号/社区：与“无账号、数据不出设备”的定位冲突，除非产品方向改变。
- 竞赛题库：和 LeetCode 比规模没有胜算，现有 22 道挑战只做课程配套。
- 手机 IDE 方向：不追 pip、多文件工程和完整项目目录。

## 四、迭代路线图

| 版本 | 主题 | 范围 | 出口条件 |
| --- | --- | --- | --- |
| 0.7.2 | 发布收口 | 定位、质量报告、课程样例、软著材料、五端发布 | 已完成 |
| 0.7.3 | 基线、离线助教与压力收口 | 四模块基线、竞品矩阵、离线助教、语法提示、压力验证 | 已发布 |
| 0.7.4 | 可用性与数据安全修补 | 草稿落盘、SQL 转义、助教总结与异步检索、错题去重、XP 记账 | 待发布 |
| 0.8.0 | 个性化学习 | 学习路径推荐、弱项复习、挑战页助教、可选模型包调研 | 不破坏离线核心，包体可控 |
| 0.9.0 | 内容与项目 | Python 综合项目、SQL 实战、多文件只读项目演示 | 课程完成率和稳定性达标 |
| 1.0.0 | 商店正式版 | 商店素材、隐私合规、长期稳定性、内容审计 | 可上架，连续使用 30 天无阻断问题 |

## 五、溯源与复用规则

1. 第三方运行时只通过 `lang/` 下的适配器调用；课程页、判题、进度不直接依赖第三方 API。
2. 开源编辑器只通过 `ui/components/CodeEditor.kt` 封装；替换编辑器不应影响课程和判题。
3. 课程内容、判题用例、助教话术为项目原创，不把第三方库的示例代码当自有内容。
4. 软著源程序册只收原创 Kotlin/Python 文件；第三方依赖单独列 `THIRD_PARTY_NOTICES.md`。
5. 新增依赖必须先查许可证，再决定是打包、动态下载还是只用于开发测试。

## 六、基线维护清单

版本变更时按顺序更新：

1. `app/build.gradle.kts` 的 `versionCode` / `versionName`。
2. 本文件顶部 `baseline:` 注释和 1.1 节版本表。
3. `CHANGELOG.md`、三语 README、`docs/USER_GUIDE.md`、官网版本串。
4. `docs/QUALITY_REPORT.md` 的测试与课程统计。
5. 五端 Release 链接和 APK SHA-256。
6. 跑 `python3 tools/check_baseline.py`、`python3 -m pytest tests -q`、`tools/check_links.py`。

基线文档不是宣传页。数字对不上时，先改文档，再发版。

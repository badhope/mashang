# 风格重构进度

按模块分批改，每批单独提交，改完跑编译和测试。行为不变，公开 API 不动。

## 第一批：核心运行时和工具

已完成：

- `lang/LanguageRuntime.kt`
- `lang/RunResultJson.kt`
- `data/JsonListFile.kt`
- `data/BackupUtil.kt`
- `utils/HttpText.kt`
- `utils/AppConstants.kt`
- `ui/components/ScreenHeader.kt`
- `ui/components/NeonTopBar.kt`

主要改动：

- 删掉变更历史、设计宣言和编号式 KDoc。
- 去掉装饰符号和模板词。
- 备份逻辑去掉多余的转发函数，输出流改用 `use`。
- 返回键触摸区域按 48dp 修正。

验证：`compileDebugKotlin` 通过，`testDebugUnitTest` 通过。

## 第二批：界面层

已完成：

- `screens/HelpScreen.kt`
- `screens/LanguageCenterScreen.kt`
- `screens/LessonBlockCards.kt`
- `screens/LessonDetailScreen.kt`
- `screens/HomeScreen.kt`
- `screens/AboutScreen.kt`
- `ui/components/CodeEditor.kt`
- `ui/theme/VisualStyle.kt`

主要改动：

- 帮助页整篇重写，FAQ 去掉营销腔和分节横幅。
- 语言中心、课程页、首页、关于页清理符号、英文标题和历史注释。
- 代码编辑器删掉只转一道手的取消包装，订阅处直接 `try/finally`。
- 视觉风格文件删掉设计论文式说明。

验证：`compileDebugKotlin` 通过，`testDebugUnitTest` 通过，提交 `2cbbc0a`。

## 第三批：运行时、数据层和剩余界面

已完成：

- Python 判题器 `runner.py`、REPL `repl.py`、SQL 执行器 `sql_runner.py`。
- Kotlin 运行时桥接、数据仓库、调试工具、主题和剩余界面文件。
- 全仓 Kotlin 生产代码的装饰符号扫描清零。
- `ViewModels` 的数据读写改为顶层函数，调用处显式传 context。
- `CoachEngine` 去掉全局可变行号，改成 `analyze` 内部捕获。
- `DebugLog` 的上限常量改名 `MAX_FILE_CHARS`，跟实际按字符截断对齐。

验证：`compileDebugKotlin` 通过，`testDebugUnitTest` 通过，提交 `5087d71`。

## 第四批：网页端

已完成：

- `web/app.js`：删掉英文横幅标题、装饰符号、营销式文案和无用图标映射。
- `web/index.html`：标题、描述、导航、页脚改成直白中文，课程数改成 245。
- `web/README.md`：按实际能力重写，补上 245 讲、22 道挑战和语言运行边界。
- `web/style.css`：头部注释改成普通说明。
- `docs/web/` 已同步。

验证：`node --check web/app.js` 通过，`node --check docs/web/app.js` 通过。

## 第五批：工具脚本和对外材料

已完成：

- `tools/softcopy_source.py` 的文档头、分页标题和打印提示去掉装饰符号。
- `tools/make_brand_assets.py` 的课程数、挑战数和 Android 版本对齐到 245 讲、22 题、Android 10。
- `tools/build_catalog.py` 的设计说明压缩成普通流程说明。
- `DebugLog` 导出头改成中文，隐私政策里的日志上限改成“约 25 万字符”，跟实际按字符截断一致。
- 测试里过期的字符串断言同步到新文案。

验证：`pytest` 247 项通过，软著源程序册实际生成 321 页。

## 第六批：官网和对外文档

已完成：

- `docs/index.html` 的品牌、课程数、挑战数和版本号对齐到 0.7.2，去掉装饰符号、英文小标题和营销腔。
- 修掉网页版区块多出来的一个 `</div>`，这个标签会让后面的 `section` 错位。
- 三语 README、`FAQ.md`、`docs/USER_GUIDE.md` 去掉品牌分隔符、箭头和粗体标签，路径改成正常说法。
- 课程统计统一成 245 讲、22 道挑战、扩展包 206 讲。

验证：HTML 标签检查没有错误，没有未闭合标签；`pytest` 247 项通过。

## 第七批：0.7.2 发布收口

已完成：

- 版本升到 `versionCode 21` / `versionName 0.7.2`，网页端、官网、用户手册和发布材料同步。
- 新增 `docs/PRODUCT_POSITIONING.md`、`docs/QUALITY_REPORT.md`、`docs/OFFLINE_DEMO.md`、`docs/COURSE_SAMPLES.md`，补齐产品定位、质量证明、离线演示和课程样例。
- 三语 README、FAQ、官网和 `docs/LANGUAGE_COVERAGE.md` 统一课程口径：245 讲、448 个课程代码块（434 个可直接运行）、1,097 条用例。
- 补 `THIRD_PARTY_NOTICES.md`，修正 Rhino 许可证说明。
- 软著材料生成：`docs/softcopy/源程序.pdf` 60 页、`软件说明书.pdf` 13 页，源程序册覆盖 80 个原创文件、16,002 行。
- 0.7.2 由 GitHub Actions 用 release keystore 签名构建，APK 通过 `apksigner` 非 debug 证书校验和 `tests/ci_apk_probe.py --require-release`。
- 0.7.2 APK 已发布到 GitHub `badhope`、`X33834`、`Morningstar202604`、Gitee `badhope`、GitCode `badhope` 的 Release。

验证：Kotlin 单测 275 项通过，`pytest` 247 项通过，`lintDebug` 0 Error，内容校验结构问题 0。

## 还没做的

- `tools/` 里的内容生成脚本只清理了明显的横幅注释，课程正文里的 `·`、箭头属于课程排版，不按代码符号处理。
- `tests/` 的断言不随便改，注释里的历史日期还在陆续清。
- `app/src/main/python` 的深水区、语言桥接和网页端交互还需要人工点一遍。
- 官网截图还是早期界面，版本角标显示 v0.4.0；下次有设备时统一重截。
- 软著是否提交、以谁的名义提交，要申请人本人决定；新版申请表有“未使用 AI”承诺，不能代签。

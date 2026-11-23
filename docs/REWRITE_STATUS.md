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

## 还没做的

- `tools/` 里的内容生成脚本只清理了明显的横幅注释，课程正文里的 `·`、箭头属于课程排版，不按代码符号处理。
- `tests/` 的断言不随便改，注释里的历史日期还在陆续清。
- `app/src/main/python` 的深水区、语言桥接和网页端交互还需要人工点一遍。
- 0.7.1 的 APK 还是重构前构建的，风格重构没有重新打包。

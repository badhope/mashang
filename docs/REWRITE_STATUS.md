# 风格重构进度

按模块推进，每批改完都过编译和测试。行为不变，公开 API 不动。

## 第一批：核心运行时与工具

已完成：

- `lang/LanguageRuntime.kt`
- `lang/RunResultJson.kt`
- `data/JsonListFile.kt`
- `data/BackupUtil.kt`
- `utils/HttpText.kt`
- `utils/AppConstants.kt`
- `ui/components/ScreenHeader.kt`
- `ui/components/NeonTopBar.kt`

这一批主要做了：

- 删掉源码里的变更历史、设计宣言、编号式 KDoc。
- 去掉装饰符号和模板词。
- 备份逻辑去掉多余的转发函数，输出流改用 `use`。
- 返回键触摸区域按 48dp 修正。
- 公开方法和数据结构保持不变。

验证：`compileDebugKotlin` 通过，`testDebugUnitTest` 通过。

## 后续批次

- UI 页面与组件：`screens/`、`ui/`
- 语言运行时：`c/`、`lua/`、`sql/`、`ts/`、`py/`
- Python 引擎：`app/src/main/python/`
- 工具链：`tools/`、`tests/`、`web/`

每个批次单独提交，避免一次改太多导致定位困难。

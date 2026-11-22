# 路线图 · Roadmap

图例：`[x]` 已完成并发布 · `[~]` 已落地待完善 · `[ ]` 计划中。
版本节奏：修 Bug 升补丁位，新功能升次版本；具体日期不承诺，以 GitHub Releases 为准。

## v0.7.x（当前）

- [x] v0.7.0 — 六门语言统一 38 讲（基础 / 核心 / 进阶 / 专业四包），C 解释器扩展到指针、结构体、switch
- [x] v0.7.0 — 语言中心按需加载、单包下载与整门装齐，catalog 增加 lessons / bytes / tier
- [x] v0.7.0 — 品牌升级 CodeLearning · 码上，三语 README、证书、终端、官网同步
- [x] v0.7.0 — 前后端去重：ScreenHeader / NeonTopBar、JsonListFile、RunResultJson、HttpText
- [ ] v0.7.x — 平板 / 横屏自适应布局
- [ ] v0.7.x — 网页端离线缓存（Service Worker，二次打开不再下载 WASM）
- [ ] v0.7.x — 内容中心增量包与版本回滚
- [ ] v0.7.x — 语言课程深度补齐：SQL / C 增加边界用例，TypeScript 增加可运行示例

## v0.4.x（历史）

- [x] v0.4.0 — 多语言运行时抽象（`LanguageRuntime`）+ JavaScript 启程 6 讲（Rhino 1.7.15）
- [x] v0.4.0 — 网页版（Pyodide 0.29.5 / CPython 3.13.2），与 App 共用课程 JSON 与 `runner.py`
- [x] v0.4.0 — 网页端 Web Worker 执行、进度 localStorage、hash 深链、填空块、移动端布局、明暗主题
- [x] v0.4.0 — 课程路线页改为真实内容（内置主线 + 4 个内容包），删除 20 条虚构「开发中 x%」轨道
- [x] v0.4.0 — 设置页「检查更新」（只读 GitHub Releases 公开版本号，不自动下载）
- [x] v0.4.0 — 判题契约补 `__stdout__` / `__stderr__`，l01 起要求真的 print 出来
- [x] v0.4.0 — 内容中心三级源（Pages → raw → GitCode）+ Pages 资产同步校验
- [x] v0.4.0 — 全量事实校准：课程幕次（27-30 归第四幕、31-38 归第五幕）、统计口径、三语 README、CI 文档
- [x] v0.4.1 — 发布口径复查：测试/用例数字、双运行时声明、语言包承诺全部对齐仓库事实（`tests/test_docs_claims.py`）
- [x] v0.5.0 — 多语言扩容：Lua 运行时（LuaJ）+ SQL 运行时（sqlite3）+ 各 6 讲课程包 + 语言中心 + 按需下载门禁

## v0.5

- [ ] turtle 海龟画布（把绘图指令可视化）
- [ ] matplotlib 图表输出（离线渲染 PNG）
- [ ] 执行过程变量动画（逐行回放变量变化）
- [ ] QuickJS 后端（ES6+，替换 Rhino 的部分限制）
- [ ] 课程内容扩到 60 讲（新增数据结构、正则进阶、项目实战）

## v1.0

- [ ] 应用商店全渠道上架（Google Play / 华为 / 小米 / OPPO / vivo）
- [ ] 端侧 AI 助教（可选，默认关闭；不改变「零账号、零上传」底线）
- [ ] 教师模式：导出班级进度报表（本地生成，不经服务器）
- [ ] 多语言界面（英文 / 日文课程正文翻译）

## 明确不做

- 云端代码执行：与「离线优先、零数据上传」定位冲突
- 账号体系与社交排行榜：会引入数据收集与合规负担
- 课程内容付费墙：MIT 开源项目，课程保持免费

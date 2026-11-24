# 竞品对标与离线能力可行性

更新日期：2026-11-24  
调研对象：通用在线编程学习平台、移动端 IDE、开源移动学习工具、端侧模型方案。  
说明：以下结论来自公开产品页、应用商店说明和开源项目文档；没有购买竞品会员逐项实测，涉及“是否完全离线”的地方按公开描述保守标注。

## 1 结论先行

码上不该去争“手机上的完整 IDE”，也争不过题库型平台。能站住的差异只有一条：

**课程、真实运行时、判题、错题复习四件事在同一个离线 App 里闭合，再加一个不联网的本地助教。**

Pydroid 3 强在自由写代码和装库，SoloLearn / Mimo 强在课程包装和社区，编程狮强在中文资料量，LeetCode 强在题库。它们各自都有一块明显长板，但没有一个把“零网络依赖的课程 + 真运行 + 判题 + 复习”做成默认路径。

本产品当前的短板也要写清：没有社区、没有云同步、没有 iOS、没有多文件项目、题库难度上限低。对外只能打“离线编程练习”，不能打“专业 IDE”或“竞赛题库”。

## 2 两条赛道的边界

| 赛道 | 代表产品 | 用户默认预期 | 码上的位置 |
| --- | --- | --- | --- |
| 通用在线编程教学 | SoloLearn、Mimo、编程狮、夜曲编程 | 课程多、路径清楚、有社区或助教，通常要联网 | 课程规模不占优，用离线真运行和判题做差异 |
| 移动端编程工具 | Pydroid 3、Termux、各类编译器壳 | 自由写代码、装库、文件管理、终端 | 不做自由 IDE，不碰 pip 和多文件项目 |
| 题库与竞赛 | LeetCode、APAS | 题目多、难度高、题解和竞赛社区 | 不从题库规模竞争，做“从讲到练到复习”的短链路 |
| 云端 Notebook | Replit、Colab | 云端算力、协作、环境生态 | 用无网可用、隐私和轻量做差异 |
| 开源移动学习工具 | Sanzu、FlowGrid 等 | 轻量、单点功能、维护不稳定 | 用完整课程体系、六门运行时和发布门禁做差异 |

## 3 竞品功能矩阵

图例：✅ 公开说明明确支持；◐ 部分支持或需要条件；— 未明确支持或不是产品重点。

| 产品 | 全离线课程 | 本地真实运行 | 自动判题 | 课程体系 | 本地 AI 助教 | 多语言 | 社区/云 | 主要短板 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **CodeLearning（码上）** | ✅ 内置课程与判题 | ✅ 六门运行时在本机 | ✅ assert 判题 | ✅ 245 讲 | ✅ 规则+课程检索，零网络 | ✅ 六门 | — 无账号/社区 | 内容深度、题库规模、无 iOS |
| Pydroid 3 | ◐ 解释器离线，课程不是重点 | ✅ CPython、C/C++ 编译器等 | — | — | — | ◐ 以 Python 为主 | — | 没有课程路径、判题和复习 |
| SoloLearn | ◐ 课程可缓存，运行依赖环境 | ✅ Code Playground 可运行 | ◐ 课程测题与 playground | ✅ 多语言路径 | ◐ 平台侧 AI/社区答疑 | ✅ | ✅ 社区 | 离线深度有限，课程偏理论 |
| Mimo | ◐ 课程可离线缓存 | ◐ 应用内练习执行 | ◐ 交互题 | ✅ 微课路径 | ◐ 平台侧 AI | ✅ | ✅ 社区/多端 | 免费内容受限，项目深度浅 |
| 编程狮 | ◐ 教程可离线阅读 | ◐ 20+ 编译工具，是否全离线未证实 | ◐ 题库 | ✅ 1000+ 教程 | ◐ 平台侧 AI 课程 | ✅ | ✅ 社区/课程服务 | 资料型产品，学习闭环偏软 |
| 夜曲编程 | — 以在线课程为主 | ◐ 在线运行 | ◐ 互动题 | ◐ Python/数据分析主线 | ◐ 社群助教 | ◐ | ✅ 社群 | 语言面窄，依赖服务 |
| LeetCode / APAS | ◐ 题目与题解可离线 | ◐ 提交通常在线 | ✅ 题库判题 | ✅ 题库/计划 | ◐ 平台侧 AI Tutor | ✅ | ✅ 竞赛社区 | 难度高，不适合零基础系统课 |
| Replit / Colab | — 依赖云端 | ✅ 云端环境 | ◐ Notebook 执行 | ◐ 项目/课程 | ◐ 平台侧 AI | ✅ | ✅ 协作 | 无网不可用，隐私与资源成本 |
| Sanzu（开源） | ✅ 离线 Python 笔记/编译 | ✅ Python | — | ◐ 笔记型 | ◐ 宣称 AI 助手 | ◐ Python | — | 单语言、课程体系弱 |

## 4 关键竞品拆解

### 4.1 Pydroid 3：把手机当 Python 工作站

公开功能包括离线 Python 解释器、pip 与自定义 wheel 源、终端、C/C++/Fortran 编译器、PDB 调试、Tkinter/Kivy 等。它解决的是“在没有电脑时跑 Python 项目”，不是“从零开始按课学习”。

码上不跟它比装库和项目目录。码上的优势是每讲有练习、有断言判题、有错题和复习；弱点是自由项目、第三方库、文件系统操作。对外文案必须避开“IDE”这个词，否则会被直接拿去和 Pydroid 比。

来源：<https://pydroid.app/>、<https://play.google.com/store/apps/details?id=ru.iiec.pydroid3>

### 4.2 SoloLearn / Mimo：课程包装和习惯养成

SoloLearn 的强项是多语言课程、Code Playground 和社区；Mimo 的强项是 5 分钟微课、连续学习习惯和多端同步。两者都能缓存部分课程，但“完全离线运行 + 本地判题”不是默认路径。

码上不跟它们比品牌、社区和职业路径。码上能赢的是：断网后课程、运行、判题、复习都还在；输的是内容包装、运营、iOS 和多端。

来源：<https://www.sitepoint.com/6-best-coding-apps-for-beginners-to-learn-programming/>、<https://techyorker.com/the-best-python-coding-app-for-beginners-in-2025/>

### 4.3 编程狮 / 夜曲编程：中文教研和资料量

编程狮公开说明包含 1000+ 教程、20+ 编译工具、题库和离线阅读；夜曲编程主打交互式 Python 课、社群助教和每天 20 分钟。两者都证明“中文课程 + 手机练习”有真实需求。

码上的差距是教研团队和服务体系，不是功能数量。码上能守住的是：所有练习本地跑、开源、无广告、学习数据不出设备。短期内不应在“课程数量”上跟编程狮硬拼。

来源：<https://m.w3cschool.cn/download>、<https://apps.apple.com/cn/app/%E5%A4%9C%E6%9B%B2%E7%BC%96%E7%A8%8B/id6443773746>

### 4.4 LeetCode / APAS：题库和竞赛

LeetCode 的优势是 4000+ 题目、竞赛、题解和社区；APAS 这类第三方 App 还提供离线题库和 AI Tutor。它们服务的是面试和竞赛，不是零基础系统课。

码上不碰题库规模。码上的优势是“讲解—练习—判题—复习”在同一条链路里，题目难度控制在入门到进阶；弱点是高阶算法题、竞赛氛围和题解社区。

来源：<https://apps.apple.com/ba/app/leetcode/id6755816119>、<https://play.google.com/store/apps/details?id=com.freetymekiyan.apas>

### 4.5 开源移动学习工具：轻量但不成体系

Sanzu 这类开源/免费工具能做到 10 MB 级、离线 Python 笔记和编译器，但通常只覆盖一门语言，课程是笔记或示例集合，没有判题和复习闭环。FlowGrid 走的是可视化数据流编程，和文本编程学习不是同一赛道。

码上复用的是它们的思路：离线优先、单包轻量、编辑器本地渲染；不复用的是它们的单点产品结构。

来源：<https://github.com/heysanzu/sanzu>、<https://github.com/stefanhaustein/flowgrid>

## 5 开源组件与复用边界

| 组件 | 许可 | 在码上承担什么 | 复用边界 |
| --- | --- | --- | --- |
| CPython 3.13 + Chaquopy | PSF / Chaquopy 自有许可 | Python 真实运行时、判题、变量快照 | 只通过 `py/PyBridge.kt` 和 `app/src/main/python` 调用，不修改 CPython 本体 |
| Mozilla Rhino 1.7.15 | MPL-2.0 | JavaScript 解释器 | 只通过 `js/JsBridge.kt` 调用 |
| LuaJ 3.0.1 | MIT | Lua 运行时 | 只通过 `lua/LuaBridge.kt` 调用 |
| SQLite（Android 内置） | Public Domain | SQL 内存库 | 只通过 `sql/SqlBridge.kt` 调用 |
| sora-editor 0.24.6 | LGPL-2.1 | 代码编辑器、语法高亮、行号、撤销 | 只通过 `ui/components/CodeEditor.kt` 封装，不在源程序册里当作原创代码 |
| ZXing Core 3.5.4 | Apache-2.0 | 证书海报二维码 | 只用于生成二维码 |
| AndroidX / Compose / Navigation | Apache-2.0 | UI、状态、导航 | 作为基础框架依赖 |
| Pyodide（网页端） | MPL-2.0 | 网页版 Python 演示 | 与 Android APK 分离，首次使用需下载 WASM |
| Kotlin 标准库 / 协程 | Apache-2.0 | 应用层 | 作为基础依赖 |

第三方清单与完整许可见 `THIRD_PARTY_NOTICES.md` 和应用内「开源许可」页。软著源程序册只放原创 Kotlin/Python 代码，第三方依赖单独列清单，不混入原创行数。

## 6 三条可行性路径

### 6.1 全离线沙箱

已落地，不是概念验证：

- Python、JavaScript、TypeScript、C、Lua、SQL 各自有运行时适配器，统一实现 `LanguageRuntime`。
- 判题在本地执行，断言失败返回具体用例，不把代码发到服务器。
- 死循环有超时中断；运行时异常转成 `RunResult`，不让页面崩。
- 变量快照来自真实执行后的命名空间，超长值截断，超大对象跳过。
- 课程包下载后做 SHA-256 校验，校验失败不落地；已下载包离线可用。

边界：C 是教学子集解释器，TypeScript 只做类型擦除，Lua 接近 5.2，SQL 是 SQLite 不是 MySQL。这些边界已写进 README 和用户手册。

### 6.2 本地代码渲染与实时提示

- 编辑器复用 sora-editor + TextMate 语法定义，支持六门语言高亮、行号、括号匹配和撤销。
- 新增 `SyntaxCheck`：在输入时检查括号、引号、中文标点和 Python 块冒号，提示但不阻塞运行。
- 没有做完整编译器级语义检查；真正的语法错误仍以运行结果为准，避免误报打断学习。

### 6.3 本地 AI 助教

当前实现是 **端侧规则 + 课程检索**，不打包大模型，也不联网：

- 报错诊断复用 `CoachEngine`，按错误类型给检查步骤，不直接给答案。
- 思路引导按“读入—处理—返回”拆题，先让用户手算最小样例。
- 概念问题从 245 讲课程正文里检索相关段落，返回课程原文和复述检查点。
- 下一步建议结合当前课程和下一讲标题，不凭空生成课程。

为什么不在 APK 里塞端侧大模型：

- 公开资料显示，Gemma 3 270M INT8 约 304 MB，Qwen2.5 0.5B Q4 约 398 MB，1B 级模型约 0.8–1.1 GB，2B 级约 1.5–2 GB；主流应用商店对单包体积有限制，首装 39 MB 的产品塞不进。
- 端侧模型需要 4 GB 以上 RAM 才有可用速度，和“低配机、纯离线、无资源消耗”的定位冲突。
- 课程助教需要的是课程事实和教学节奏，不需要通用聊天能力。规则+检索能把这些做准，模型反而可能编造课程里没有的内容。

后续如果要接本地模型，正确做法是可选模型包：用户主动导入 GGUF/MediaPipe 模型，模型只负责润色讲解，事实和题目仍从课程库检索；不把模型权重打进主 APK，也不把“有没有模型”当成核心学习链路的前提。

## 7 差异化定位

对外一句话：

> 装在安卓手机里的离线编程训练场：六门语言，边学边跑，断言判题，错题自动复习；不联网也能用，代码和进度都不出设备。

三条护城河：

1. **离线闭环**：不是“课程能缓存”，而是运行、判题、复习都能断网完成。
2. **教学约束**：助教只给下一步，不代写答案；判题失败进错题本，不让人刷过去。
3. **轻量可信**：首装约 39 MB，无账号、无广告、无统计 SDK，第三方依赖和许可公开。

不打的牌：手机 IDE、竞赛题库、社区、云同步、iOS。短期都不做，避免把产品做成四不像。

## 8 调研来源

- Pydroid 3 官方站：<https://pydroid.app/>
- Pydroid 3 Google Play：<https://play.google.com/store/apps/details?id=ru.iiec.pydroid3>
- SoloLearn / Mimo 对比：<https://www.sitepoint.com/6-best-coding-apps-for-beginners-to-learn-programming/>
- 编程狮官方下载页：<https://m.w3cschool.cn/download>
- 夜曲编程 App Store：<https://apps.apple.com/cn/app/%E5%A4%9C%E6%9B%B2%E7%BC%96%E7%A8%8B/id6443773746>
- LeetCode App Store：<https://apps.apple.com/ba/app/leetcode/id6755816119>
- APAS（LeetCode 题库类 App）：<https://play.google.com/store/apps/details?id=com.freetymekiyan.apas>
- Sanzu 开源项目：<https://github.com/heysanzu/sanzu>
- FlowGrid 开源项目：<https://github.com/stefanhaustein/flowgrid>
- 端侧模型尺寸与内存：<https://insiderllm.com/pdfs/run-llms-old-phones-mobile-inference.pdf>、<https://www.ertas.ai/blog/gguf-llama-cpp-mobile-app-guide>
- MediaPipe LLM Inference：<https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android>

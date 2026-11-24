# 质量报告

更新日期：2026-11-24
版本：0.7.5（versionCode 24）
适用安装包：arm64-v8a，Android 10 及以上

## 结论

当前源码可以对外发布，但只能按“入门到进阶的离线练习产品”介绍，不能包装成职业级完整课纲或专业 IDE。

自动化和内容校验全部通过。线上崩溃率没有采集，应用不集成统计 SDK，所以不编造一个“崩溃率 0.0%”的数字；质量靠构建门禁、单元测试和内容校验来保证。

## 测试结果

| 项目 | 结果 |
| --- | --- |
| Kotlin 编译 | `compileDebugKotlin` 通过 |
| Android 单元测试 | 311 项，0 失败，1 跳过 |
| Python 测试 | 260 项通过，551 个 subtest 通过 |
| Lint | 0 Error，47 Warning，6 Hint |
| 内容结构校验 | 245 讲、22 道挑战、267 个判题项、1,097 条用例，结构问题 0，判题失败 0 |
| 网页端语法 | `node --check web/app.js`、`web/data.js`、`web/engine.js` 通过 |
| 官网结构 | HTML 标签检查无错误、无未闭合标签 |

## 0.7.5 修正与验证

| 项目 | 修正 | 验证 |
| --- | --- | --- |
| Python 可联网/起子进程，与隐私口径不一致 | `runner.py` 增加操作级审计钩子，执行用户代码时拒绝 INET/INET6 socket、`os.system`、`subprocess.Popen`、`os.exec`/`fork`/`kill`、`java`/`ctypes` 桥 | `test_engine_sandbox` 11 项；`asyncio`/`threading`/`os.path` 仍可用 |
| 用户代码新开的线程可能绕过 guard | `threading.Thread.start` 在 guard 内自动包一层 `sandbox_guard` | 子线程建 socket 返回 PermissionError |
| 全新安装只内置 39 讲，206 讲要联网 | 24 个包 + 离线 catalog 打进 assets，首启复制到 `filesDir/courses`；已有更高版本不覆盖 | `BundledContentTest` 5 项；APK 探针 24 包/245 讲 |
| 语言中心首屏没网时不显示已装包 | 初始读取 `assets/catalog.json`，离线也能列出 24 包；联网只检查更新 | `bundledCatalog` 测试 + 文案同步 |

## 0.7.4 修正与验证（历史）

| 问题 | 修正 | 验证 |
| --- | --- | --- |
| 练习草稿只靠 `rememberSaveable`，进程被杀就丢 | 增加 `DraftStore`，输入停顿 500ms 落盘，离开页面同步补写；起始代码不落盘，改回起始时清旧草稿 | `DraftStoreTest` 3 项 + 状态路径复核 |
| SQL `'it''s'` 被扫描器误判引号未闭合 | SQL 单/双引号连续转义按同一字符串处理 | `SyntaxCheckTest` 2 项 |
| 助教每次在 UI 线程重建 245 讲索引 | 只按当前语言建索引，检索放到 Default 线程 | `TutorEngineTest` + 编译 |
| 挑战页“下一步”误称主线走完 | `TutorRequest.isChallenge` 区分挑战语境 | `TutorEngineTest` |
| 无练习课的默认问题落到“没找到” | 增加总结意图，直接取当前讲正文 | `TutorEngineTest` |
| 长问题检索只取前 8 个二字词，漏掉句尾关键词 | 头尾各取 4 个二字词 | `TutorEngineTest` |
| XP 提示按题目 XP 相加，和真实入账（每日奖励/已完成课）对不上 | 课程与挑战统一以写入前后快照差值为准 | 编译 + 手算路径复核 |
| 最后一讲仍说“下一课解锁”，内容包课被当成主线走完 | 按 `nextInTrack` 分支显示收尾文案，内容包提示回目录挑下一讲 | 编译 + 路径复核 |
| 同一空位反复失败把错题本刷成流水账 | 同课同块只留最新一次，总量上限 300 | `MistakeMergeTest` 3 项 |
| 内容包课程不在当前章节列表时，误把第一讲当“下一课” | `lessonIdx < 0` 时不推下一讲 | 编译 + 路径复核 |
| 旋转后助教默认问题覆盖用户输入 | `initialAsked` 保存状态 | 编译 + 状态路径复核 |

## 离线压力验证

| 项目 | 结果 |
| --- | --- |
| 引擎连续提交 | 126 次正常提交 + 12 次故意坏代码，0 崩溃 |
| 重负载场景 | 15 万次循环求和、2.5 万词计数、2 万个数排序，全部判题通过 |
| 平均单次耗时 | 2.9ms（桌面 Python 3.13，含编译与判题） |
| 峰值内存 | 78.0 MB（单进程连续执行） |
| 进度写入 | 120 次练习写入、120 个 key 备份恢复，无丢失 |
| 死循环 | 1 秒超时中断生效，未拖住进程 |

这是加速压力测试，不是半个月真机长跑。它能证明“连续一百多次提交不会越来越慢、不会丢 key”，不能替代低配真机上的发热、后台被杀和存储写满测试；后者仍要在发布前用真机补。

## 课程规模

| 语言 | 讲数 | 代码块 / 可直接运行 | 课程用例 | 独立挑战 |
| --- | ---: | ---: | ---: | ---: |
| Python | 50 | 86 / 84 | 189 | 7 道 |
| JavaScript | 38 | 76 / 76 | 170 | 3 道 |
| TypeScript | 38 | 45 / 39 | 168 | 3 道 |
| C | 38 | 76 / 71 | 143 | 3 道 |
| Lua | 38 | 77 / 77 | 162 | 3 道 |
| SQL | 43 | 88 / 87 | 134 | 3 道 |
| 合计 | 245 | 448 / 434 | 966 | 22 道 / 131 例 |

Python 含 1 讲综合项目、DSA 10 讲、彩蛋 1 讲；SQL 含 5 讲边界专题。全库判题用例含挑战共 1,097 条。

## 离线能力验证

- 课程、判题脚本、Python 运行时和进度存储都在应用内；245 讲和 24 个课程包随 APK 预装，断网不影响已装内容。
- `pytest` 的判题用例在本地直接执行 Python、SQLite 和 Node，不依赖远端服务。
- JavaScript 用 Rhino，Lua 用 LuaJ，C 用内置教学解释器，均在 JVM/设备本地执行。
- 网页版是例外：首次打开要下载 Pyodide，之后靠浏览器缓存；它只覆盖 Python。

## 已知边界

- 离线助教是“规则 + 课程检索”，没有把大模型权重打进 APK；它不联网、不编造课程外内容，但也不是通用聊天模型。
- Android 10 及以上，只打 arm64-v8a；没有 iOS 版。
- 不预装第三方 Python 包，不能当数据分析环境。
- Python 操作沙箱拦的是正常执行路径上的网络/命令调用；同一解释器内不是进程级隔离，不能承诺对抗恶意代码绝对不可绕过。
- TypeScript 不做类型检查，C 是教学子集，Lua 不是完整 5.4。
- 挑战编辑器只支持单文件，没有多文件项目模式。
- 没有账号、云同步、社区；学习数据只在本机，换机靠备份 JSON。
- 不集成广告、统计、推送 SDK，因此没有线上留存和崩溃率报表。

## Lint 警告怎么处理

47 条 Warning 分三类：

- 23 条是依赖有更新版本。没有为了消警告盲目升级，避免在发布前引入行为变化。
- 16 条是 `UseKtx` 写法建议。不影响正确性，后续小版本再统一替换。
- 8 条是平台、产品选择或 Lint 版本兼容提示：固定竖屏、只支持 arm64、targetSdk 35、旧版本资源目录、自定义规则检查版本偏旧。固定竖屏和 ABI 是当前产品策略，不是漏改。

## 发布门禁

发布前按顺序执行（0.7.5 实测均通过）：

```bash
python3 -m pytest tests -q
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/tools/android-sdk ./gradlew testDebugUnitTest --no-daemon
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/tools/android-sdk ./gradlew lintDebug --no-daemon
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/tools/android-sdk ./gradlew :app:assembleRelease --no-daemon
```

签名后的 APK 还要过 `apksigner verify` 和 `tests/ci_apk_probe.py --require-release`，确认不是 debug 证书、包名和版本号正确，再上传 Release。

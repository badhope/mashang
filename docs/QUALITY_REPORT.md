# 质量报告

更新日期：2026-11-24
版本：0.7.3（versionCode 22）
适用安装包：arm64-v8a，Android 10 及以上

## 结论

当前源码可以对外发布，但只能按“入门到进阶的离线练习产品”介绍，不能包装成职业级完整课纲或专业 IDE。

自动化和内容校验全部通过。线上崩溃率没有采集，应用不集成统计 SDK，所以不编造一个“崩溃率 0.0%”的数字；质量靠构建门禁、单元测试和内容校验来保证。

## 测试结果

| 项目 | 结果 |
| --- | --- |
| Kotlin 编译 | `compileDebugKotlin` 通过 |
| Android 单元测试 | 292 项，0 失败，1 跳过 |
| Python 测试 | 249 项通过，551 个 subtest 通过 |
| Lint | 0 Error，47 Warning，5 Hint |
| 内容结构校验 | 245 讲、22 道挑战、267 个判题项、1,097 条用例，结构问题 0，判题失败 0 |
| 网页端语法 | `node --check web/app.js`、`web/data.js`、`web/engine.js` 通过 |
| 官网结构 | HTML 标签检查无错误、无未闭合标签 |

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

- 课程、判题脚本、Python 运行时和进度存储都在应用内，断网不影响已下载内容。
- `pytest` 的判题用例在本地直接执行 Python、SQLite 和 Node，不依赖远端服务。
- JavaScript 用 Rhino，Lua 用 LuaJ，C 用内置教学解释器，均在 JVM/设备本地执行。
- 网页版是例外：首次打开要下载 Pyodide，之后靠浏览器缓存；它只覆盖 Python。

## 已知边界

- 离线助教是“规则 + 课程检索”，没有把大模型权重打进 APK；它不联网、不编造课程外内容，但也不是通用聊天模型。
- Android 10 及以上，只打 arm64-v8a；没有 iOS 版。
- 不预装第三方 Python 包，不能当数据分析环境。
- TypeScript 不做类型检查，C 是教学子集，Lua 不是完整 5.4。
- 挑战编辑器只支持单文件，没有多文件项目模式。
- 没有账号、云同步、社区；学习数据只在本机，换机靠备份 JSON。
- 不集成广告、统计、推送 SDK，因此没有线上留存和崩溃率报表。

## Lint 警告怎么处理

47 条 Warning 分三类：

- 27 条是依赖有更新版本。没有为了消警告盲目升级，避免在发布前引入行为变化。
- 14 条是 `UseKtx` 写法建议。不影响正确性，后续小版本再统一替换。
- 6 条是平台与产品选择：固定竖屏、只支持 arm64、targetSdk 35、旧版本资源目录。固定竖屏和 ABI 是当前产品策略，不是漏改。

## 发布门禁

发布前按顺序执行（0.7.3 实测均通过）：

```bash
python3 -m pytest tests -q
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/android-sdk ./gradlew testDebugUnitTest --no-daemon
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/android-sdk ./gradlew lintDebug --no-daemon
JAVA_HOME=/home/user/tools/jdk17 ANDROID_HOME=/home/user/android-sdk ./gradlew :app:assembleRelease --no-daemon
```

签名后的 APK 还要过 `apksigner verify` 和 `tests/ci_apk_probe.py --require-release`，确认不是 debug 证书、包名和版本号正确，再上传 Release。

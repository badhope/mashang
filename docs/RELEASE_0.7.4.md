# 0.7.4 发布说明

**状态：已完成并发布。** 2026-11-24，`v0.7.4`，发布提交 `4c4e7e4`。

0.7.4 是可用性与数据安全修补版本，不改课程结构，也不新增语言。它修的是实际用起来会丢东西、会误判、会把用户带偏的地方。

## 这一版修了什么

- **草稿真的能活下来。** 课程和挑战编辑器在输入停顿 500ms 后写本机 `SharedPreferences`，离开页面再同步补写；被系统杀掉、从最近任务划掉，重开还能接上。起始代码不写草稿，改回起始状态会清掉旧草稿，不会再每次打开都显示“已恢复上次未提交的草稿”。
- **SQL 的 `''` / `""` 不再误报。** `'it''s fine'` 这种连续单引号转义按同一字符串处理，不再提示“引号没有闭合”。补了单引号和双引号两条回归测试。
- **助教不再压着 UI 线程建全库索引。** 检索放到 `Dispatchers.Default`，加了加载态和请求序号；连点不会覆盖前一次回答，旋转后也不会用默认问题冲掉用户输入。课程页只在打开助教时按当前语言建索引。
- **没有练习的课，问“重点是什么”有正经回答。** 新增 `SUMMARY` 意图，直接取当前讲正文；点“解释这个知识点”会先要求给出具体名词，不再拿“知识”两个字去全库乱翻。
- **“帮我读报错”不再编。** 没跑过代码就提示先跑一次；跑过且已通过就说明这次没有报错；只运行未判题则让用户贴 stdout/stderr 和预期。没有结果时不再给一套泛化排错话。
- **XP 按真实入账显示。** 课程和挑战都以写入前后 `ProgressStore.snapshot` 差值显示，不再拿题目 XP 直接相加；每日首练奖励、已完成课不会造成错账。
- **错题本去重并封顶。** 同课、同 blockType、同 blockIndex 只保留最新一次代码和报错，id 不变；总量上限 300 条，反复失败不会把错题本刷成流水账。
- **“下一课”不再乱指。** 内容包课程不在当前章节列表时，不把第一讲硬当成下一课；最后一讲不再显示“下一课解锁”，助教也会提示回课程包目录，而不是假定主线已经走完。

## 验证

| 项目 | 结果 |
| --- | --- |
| Kotlin 编译 | `compileDebugKotlin` 通过 |
| Android 单测 | 306 项，0 失败，1 跳过 |
| Python 测试 | 249 项通过，551 个 subtest 通过 |
| Lint | 0 Error，47 Warning，6 Hint |
| 内容校验 | 245 讲、22 道挑战、267 个判题项、1,097 条用例，结构问题 0 |
| 基线校验 | `tools/check_baseline.py` 通过 |
| APK 探针 | `ci_apk_probe.py --require-release` 全部通过 |

## 安装包

- 文件名：`mashang-v0.7.4.apk`
- 大小：26,324,182 字节
- SHA-256：`bf4e36c5f915007c5dd7f0aff0e7fdd536e3c16316f4005477f5bc185e35007d`
- 包名：`com.pyneon.academy`
- versionCode：23
- versionName：0.7.4
- ABI：`arm64-v8a`
- 签名证书 SHA-256：`9721440afcd2b830159af84a8c7cb436cd89c891c18f1819f491d749beac610e`

证书与 0.7.3 相同，可直接覆盖安装，不用先卸载。只打 `arm64-v8a`；Android 10（API 29）及以上。

## 下载

- GitHub `badhope`：<https://github.com/badhope/mashang/releases/tag/v0.7.4>
- GitHub `X33834`：<https://github.com/X33834/mashang/releases/tag/v0.7.4>
- GitHub `Morningstar202604`：<https://github.com/Morningstar202604/mashang/releases/tag/v0.7.4>
- Gitee：<https://gitee.com/badhope/mashang/releases/tag/v0.7.4>
- GitCode：<https://gitcode.com/badhope/mashang/releases>

## 软著材料

按 0.7.4 最终源码重算：85 个源文件（Kotlin 82 + Python 3）、17,272 行、源程序册总 346 页，提交前 30 页 + 后 30 页共 60 页；说明书 19 页。

## 已知边界

- 没有真机/模拟器长跑数据，键盘遮挡只加了 `imePadding()`，小屏 + 输入法弹起仍需真机确认。
- `SyntaxCheck` 不处理 Lua 长字符串 `[[...]]` 和长注释 `--[[...]]`；当前课程资产没有这类写法。
- 草稿只在本机，不进备份 JSON；换机恢复学习进度不会带草稿。
- 不提供自由项目、多文件工程、pip、第三方库安装；这是产品边界，不是这一版要补的缺陷。

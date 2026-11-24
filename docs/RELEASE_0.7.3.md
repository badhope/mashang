# CodeLearning 0.7.3

离线助教与学习闭环收口版本。课程结构不变，重点补上“卡住时有人给思路”和“输入时不犯低级语法错”。

## 新增

- 课程页、挑战页接入离线 AI 助教：思路拆解、报错诊断、课程概念检索、下一步建议。
- 编辑器下方语法提示：括号、引号、中文标点、Python 块冒号。
- 四模块项目基线、竞品矩阵、缺陷清单和代码审查记录。
- 120 次练习写入与备份恢复压力测试；126 次正常提交 + 12 次坏代码的引擎压力测试。

## 修正

- 挑战判题补传 stdin，避免以后加输入题时误判。
- 清掉生产代码剩余 7 处 `!!`，补齐空值分支。
- 华为商店文案系统要求改为 Android 10（API 29）。
- 软著材料按当前源码更新为 84 个文件、16,921 行、339 页。

## 质量

- Android 单测 292 项，0 失败，1 跳过。
- Python 测试 249 项，551 个 subtest，通过。
- Lint 0 Error，47 Warning，5 Hint。
- 内容校验 245 讲、22 道挑战、267 个判题项、1,097 条用例，结构问题 0。
- 压力测试平均 2.9ms，峰值 78.0MB。

## 边界

- 助教是本地规则 + 课程检索，不联网、不加载大模型；端侧模型作为可选模型包后续评估。
- 截图仍是开发期界面，版本角标不代表当前构建。
- 只支持 Android 10+ / arm64-v8a。

## 下载

- GitHub（主仓）：<https://github.com/badhope/mashang/releases/tag/v0.7.3>
- GitHub（X33834）：<https://github.com/X33834/mashang/releases/tag/v0.7.3>
- GitHub（Morningstar202604）：<https://github.com/Morningstar202604/mashang/releases/tag/v0.7.3>
- Gitee：<https://gitee.com/badhope/mashang/releases/tag/v0.7.3>
- GitCode：<https://gitcode.com/badhope/mashang/releases/tag/v0.7.3>

APK：`mashang-v0.7.3.apk`，26,307,798 字节，SHA-256 `34f13c73c48bf2f2dfbfc4a1da29b9e615b83f1c45d29cbdf5b86c4ff47b508e`。

完整说明见仓库 `docs/QUALITY_REPORT.md`、`docs/PROJECT_BASELINE.md` 和 `CHANGELOG.md`。

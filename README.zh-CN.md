<div align="center">

<img src="docs/banner.png" alt="CodeLearning（码上）" width="830"/>

**CodeLearning（码上）：手机上写代码。六门语言，共 245 讲，全程离线。**

[![License: MIT](https://img.shields.io/badge/License-MIT-00E5FF.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10.0%2B-00E5FF.svg)]()
[![Languages](https://img.shields.io/badge/Languages-6-00FF9C.svg)]()
[![Lessons](https://img.shields.io/badge/Lessons-245-F7FF00.svg)]()
[![Download](https://img.shields.io/badge/Download-APK-FF2D78.svg)](https://github.com/badhope/mashang/releases/latest)

[English](README.md) | [中文](README.zh-CN.md) | [日本語](README.ja-JP.md)

</div>

---

## 界面

| 首页 | 课程 | 判题 |
|:--:|:--:|:--:|
| ![首页](demo/app/home.png) | ![课程](demo/app/lessons.png) | ![判题](demo/app/judge_pass.png) |

| 练习编辑器 | 终端 | 语言中心 |
|:--:|:--:|:--:|
| ![练习](demo/app/lesson_exercise.png) | ![终端](demo/app/terminal.png) | ![语言中心](demo/app/contenthub.png) |

截图来自开发期构建，版本角标和课程数不代表当前版本，以安装包和课程目录为准。

---

## 六门语言，写错会报错

| 语言 | 运行时 | 课程 |
|---|---|---|
| **Python** | CPython 3.13 | 39 讲主线（含 1 讲综合项目），另有 DSA 10 讲和彩蛋 1 讲 |
| **JavaScript** | Rhino 1.7.15 | 38 讲（基础、核心、进阶、专业四包） |
| **TypeScript** | 类型擦除后交给 Rhino | 38 讲（基础、核心、进阶、专业四包） |
| **C** | 内置解释器 | 38 讲（基础、核心、进阶、专业四包） |
| **Lua** | LuaJ 3.0.1 | 38 讲（基础、核心、进阶、专业四包） |
| **SQL** | SQLite | 43 讲（38 讲主线，另 5 讲边界专题） |

Python 主线随 APK 内置；其余五门从「语言中心」按需下载。首装约 39 MB。

C 和 TypeScript 是教学子集（C 不支持预处理指令、goto 与函数指针，TS 不支持装饰器与命名空间），遇到会明确提示。

覆盖度：六门共 245 讲、448 个课程代码块（434 个可直接运行）、1,097 条判题用例，每讲都有判题。定位是入门到进阶的练习型课程，不是职业级完整课纲。明细见 [docs/LANGUAGE_COVERAGE.md](docs/LANGUAGE_COVERAGE.md)。

---

## 卡住时，问本机助教

课程页和挑战页都有「AI 助教」入口。它不联网、不加载大模型，回答只从当前题目、运行结果和课程正文里检索：

- 「这题怎么开始」：把输入、处理、返回拆成三步，先让你手算最小样例。
- 「帮我读报错」：按错误类型给检查顺序，不直接甩完整答案。
- 「解释这个知识点」：返回对应课程原文和复述检查点；课程里没有就明说没找到。
- 「下一步学什么」：结合当前进度和下一讲标题，不凭空造课。

编辑器下方还有语法提示，括号、引号、中文标点和 Python 冒号会在输入时提示，但不会打断你运行。

---

## 项目资料

- [产品定位与竞品对比](docs/PRODUCT_POSITIONING.md)
- [课程样例](docs/COURSE_SAMPLES.md)
- [质量报告](docs/QUALITY_REPORT.md)
- [离线演示步骤](docs/OFFLINE_DEMO.md)
- [语言覆盖与题量体检](docs/LANGUAGE_COVERAGE.md)

---

## 安装

- **GitHub**：[Releases](https://github.com/badhope/mashang/releases/latest) 下载 `mashang-*.apk`
- **GitCode**：[Releases](https://gitcode.com/badhope/mashang/releases) 国内直连
- **Gitee**：[Releases](https://gitee.com/badhope/mashang/releases) 国内镜像
- Android 10（API 29）及以上，arm64-v8a
- 第一次用？先看 [完整用户手册](docs/USER_GUIDE.md)：安装、上手、语言包、备份与排障

从源码构建：

```bash
./gradlew :app:assembleDebug
```

---

## 许可

MIT。第三方组件清单见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，应用内「关于」里的开源许可页有完整许可文本。

[隐私政策](PRIVACY_POLICY.md)、[用户协议](TERMS_OF_SERVICE.md)

---

<sub>源码镜像：[GitHub](https://github.com/badhope/mashang)、[GitCode](https://gitcode.com/badhope/mashang)、[Gitee](https://gitee.com/badhope/mashang)</sub>

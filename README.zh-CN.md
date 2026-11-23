<div align="center">

<img src="docs/banner.png" alt="CodeLearning · 码上" width="830"/>

**CodeLearning · 码上 —— 手机上写代码。六门语言，共 245 讲，全程离线。**

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

---

## 六门语言，写错会报错

| 语言 | 运行时 | 课程 |
|---|---|---|
| **Python** | CPython 3.13 | 39 讲内置主线 + 综合项目 + DSA + 内置函数 |
| **JavaScript** | Rhino 1.7.15 | 38 讲（基础 / 核心 / 进阶 / 专业四包） |
| **TypeScript** | 类型擦除 → Rhino | 38 讲（基础 / 核心 / 进阶 / 专业四包） |
| **C** | 内置解释器 | 38 讲（基础 / 核心 / 进阶 / 专业四包） |
| **Lua** | LuaJ 3.0.1 | 38 讲（基础 / 核心 / 进阶 / 专业四包） |
| **SQL** | SQLite | 43 讲（38 主线 + 5 边界专题） |

Python 主线随 APK 内置；其余五门从「语言中心」按需下载。首装约 39 MB。

C 和 TypeScript 是教学子集（C 不支持预处理指令、goto 与函数指针，TS 不支持装饰器与命名空间），遇到会明确提示。

覆盖度：六门共 245 讲、471 个可运行示例、1,097 条判题用例，每讲都有判题。定位是入门到进阶的练习型课程，不是职业级完整课纲。明细见 [docs/LANGUAGE_COVERAGE.md](docs/LANGUAGE_COVERAGE.md)。

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

MIT。第三方组件许可见应用内「关于 → 开源许可」。

[隐私政策](PRIVACY_POLICY.md) · [用户协议](TERMS_OF_SERVICE.md)

---

<sub>源码镜像：[GitHub](https://github.com/badhope/mashang) · [GitCode](https://gitcode.com/badhope/mashang) · [Gitee](https://gitee.com/badhope/mashang)</sub>

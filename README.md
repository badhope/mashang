<div align="center">

<img src="docs/banner.png" alt="CodeLearning" width="830"/>

**CodeLearning: write code on your phone. Six languages, 245 lessons, all offline.**

[![License: MIT](https://img.shields.io/badge/License-MIT-00E5FF.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10.0%2B-00E5FF.svg)]()
[![Languages](https://img.shields.io/badge/Languages-6-00FF9C.svg)]()
[![Lessons](https://img.shields.io/badge/Lessons-245-F7FF00.svg)]()
[![Download](https://img.shields.io/badge/Download-APK-FF2D78.svg)](https://github.com/badhope/mashang/releases/latest)

[English](README.md) | [中文](README.zh-CN.md) | [日本語](README.ja-JP.md)

</div>

---

## Screens

| Home | Lessons | Judging |
|:--:|:--:|:--:|
| ![Home](demo/app/home.png) | ![Lessons](demo/app/lessons.png) | ![Judge](demo/app/judge_pass.png) |

| Editor | Terminal | Language Center |
|:--:|:--:|:--:|
| ![Editor](demo/app/lesson_exercise.png) | ![Terminal](demo/app/terminal.png) | ![Language Center](demo/app/contenthub.png) |

---

## Six languages, real errors

| Language | Runtime | Lessons |
|---|---|---|
| **Python** | CPython 3.13 | 39 built-in mainline + capstone + DSA + builtins |
| **JavaScript** | Rhino 1.7.15 | 38 (basic / core / advanced / expert packs) |
| **TypeScript** | type erasure to Rhino | 38 (basic / core / advanced / expert packs) |
| **C** | built-in interpreter | 38 (basic / core / advanced / expert packs) |
| **Lua** | LuaJ 3.0.1 | 38 (basic / core / advanced / expert packs) |
| **SQL** | SQLite | 43 (38 mainline + 5 edge topics) |

The Python mainline ships inside the APK. The other five download on demand from the Language Center. First install ~39 MB.

C and TypeScript are teaching subsets (C has no preprocessor, `goto` or function pointers; TypeScript has no decorators or namespaces) and say so explicitly when you hit a gap.

Coverage: 245 lessons, 471 runnable examples and 1,097 judge cases across six tracks; every lesson has a graded exercise. This is introductory-to-intermediate practice, not a complete professional curriculum. Details: [docs/LANGUAGE_COVERAGE.md](docs/LANGUAGE_COVERAGE.md).

---

## Install

- **GitHub**: [Releases](https://github.com/badhope/mashang/releases/latest)
- **GitCode**: [Releases](https://gitcode.com/badhope/mashang/releases)
- **Gitee**: [Releases](https://gitee.com/badhope/mashang/releases)
- New here? Read the [User Guide](docs/USER_GUIDE.md) first.
- Android 10 (API 29)+, arm64-v8a

Build from source:

```bash
./gradlew :app:assembleDebug
```

---

## License

MIT. Third-party licenses are listed in the app under About, in the Open Source Licenses section.

[Privacy](PRIVACY_POLICY.md), [Terms](TERMS_OF_SERVICE.md)

---

<sub>Mirrors: [GitHub](https://github.com/badhope/mashang), [GitCode](https://gitcode.com/badhope/mashang), [Gitee](https://gitee.com/badhope/mashang)</sub>

<div align="center">

<img src="docs/banner.png" alt="CodeLearning（码上）" width="830"/>

**CodeLearning（码上）：スマホでコードを書く。6 言語、245 レッスン、すべてオフライン。**

[![License: MIT](https://img.shields.io/badge/License-MIT-00E5FF.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10.0%2B-00E5FF.svg)]()
[![Languages](https://img.shields.io/badge/Languages-6-00FF9C.svg)]()
[![Lessons](https://img.shields.io/badge/Lessons-245-F7FF00.svg)]()
[![Download](https://img.shields.io/badge/Download-APK-FF2D78.svg)](https://github.com/badhope/mashang/releases/latest)

[English](README.md) | [中文](README.zh-CN.md) | [日本語](README.ja-JP.md)

</div>

---

## スクリーンショット

| ホーム | レッスン | 採点 |
|:--:|:--:|:--:|
| ![ホーム](demo/app/home.png) | ![レッスン](demo/app/lessons.png) | ![採点](demo/app/judge_pass.png) |

| エディタ | ターミナル | 言語センター |
|:--:|:--:|:--:|
| ![エディタ](demo/app/lesson_exercise.png) | ![ターミナル](demo/app/terminal.png) | ![言語センター](demo/app/contenthub.png) |

---

## 6 言語、間違えれば本物のエラー

| 言語 | ランタイム | レッスン |
|---|---|---|
| **Python** | CPython 3.13 | 39（総合プロジェクト 1 講を含む）+ DSA 10 講 + 彩蛋 1 講 |
| **JavaScript** | Rhino 1.7.15 | 38（基礎、コア、上級、専門パック） |
| **TypeScript** | 型消去して Rhino | 38（基礎、コア、上級、専門パック） |
| **C** | 内蔵インタプリタ | 38（基礎、コア、上級、専門パック） |
| **Lua** | LuaJ 3.0.1 | 38（基礎、コア、上級、専門パック） |
| **SQL** | SQLite | 43（38 メインライン + 5 境界トピック） |

Python メインラインは APK に内蔵。残り 5 言語は「言語センター」からオンデマンドでダウンロード。初回インストール約 39 MB。

C と TypeScript は学習用サブセットです（C はプリプロセッサ・goto・関数ポインタなし、TypeScript はデコレータ・名前空間なし）。非対応時は明示します。

カバレッジ：6 言語で 245 レッスン、448 個のコースコードブロック（434 個は直接実行可能）、1,097 件の採点ケース。全レッスンに採点付き演習があります。入門から中級の演習コースであり、職業課程ではありません。詳細は [docs/LANGUAGE_COVERAGE.md](docs/LANGUAGE_COVERAGE.md)。

---

## プロジェクト資料

- [製品の位置づけと競合比較](docs/PRODUCT_POSITIONING.md)
- [コース例](docs/COURSE_SAMPLES.md)
- [品質レポート](docs/QUALITY_REPORT.md)
- [オフライン演示手順](docs/OFFLINE_DEMO.md)
- [言語カバレッジと問題数](docs/LANGUAGE_COVERAGE.md)

---

## インストール

- **GitHub**：[Releases](https://github.com/badhope/mashang/releases/latest)
- **GitCode**：[Releases](https://gitcode.com/badhope/mashang/releases)
- **Gitee**：[Releases](https://gitee.com/badhope/mashang/releases)
- はじめての方は [ユーザーガイド](docs/USER_GUIDE.md) を先にお読みください。
- Android 10（API 29）以上、arm64-v8a

ソースからビルド：

```bash
./gradlew :app:assembleDebug
```

---

## ライセンス

MIT。サードパーティの構成は [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) に記載。ライセンス全文はアプリ内「About のオープンソースライセンス」にあります。

[プライバシー](PRIVACY_POLICY.md)、[利用規約](TERMS_OF_SERVICE.md)

---

<sub>ミラー：[GitHub](https://github.com/badhope/mashang)、[GitCode](https://gitcode.com/badhope/mashang)、[Gitee](https://gitee.com/badhope/mashang)</sub>

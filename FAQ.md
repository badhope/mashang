# 常见问题 · FAQ

## 产品与定位

**Q：CodeLearning 是什么？和 Pydroid / SoloLearn / Mimo 有什么不同？**

CodeLearning（码上）是一台「课程 + 判题 + 运行环境」打包在一起的离线学习终端。Pydroid 给你一个空白 IDE；SoloLearn/Mimo 给你选择题和云端判题。CodeLearning 要求你**真的写代码**，用 assert 判题卡住进度，解释器就在手机里，全程可断网。

**Q：为什么叫「CodeLearning」/「码上」？**

英文名 CodeLearning 直白点题：在代码里学习。中文名「码上」谐音「马上」——打开就能写，不用等编译、不用等云端返回。

**Q：只学 Python 吗？**

内置六个离线运行时：Python（CPython 3.13）、JavaScript（Rhino）、TypeScript（类型擦除）、C（Kotlin 教学子集）、Lua（LuaJ）和 SQL（SQLite）。Python 主线 39 讲（含综合项目 1 讲）内置；语言中心可按需下载 JavaScript / TypeScript / C / Lua / SQL 课程包，以及 DSA 算法包（10 讲）与彩蛋课（1 讲）。Java / C++ / Go / Rust 需要编译器或原生工具链，语言中心明确标注「暂不支持」且不提供下载。架构上 `LanguageRuntime` 已抽象，新增可解释语言只需再实现一个「说同一套 JSON」的后端。

## 离线与网络

**Q：真的完全离线？联网权限干什么用？**

学习、写码、运行、判题 100% 本地。 `INTERNET` 权限只用于「我的 → 语言中心」手动检查/下载课程包（GitCode → GitHub raw → GitHub Pages 三级源，SHA-256 校验，零个人数据）。另外「系统设置 → 检查更新」只读取 GitHub Releases 的公开版本号。

**Q：网页版也要联网吗？**

首次打开需要下载约 12 MB 的 Pyodide（CPython 3.13 WASM），之后浏览器会缓存；断网后已缓存的部分仍可用。App 端不需要这一步。

## 数据与隐私

**Q：我的学习数据存在哪？会上传吗？**

只存本机：Android 用 DataStore + `filesDir` 的 JSON；网页版用 `localStorage`。没有账号体系，没有统计 SDK，没有上传接口。卸载 App 即全部删除（可在「我的 → 备份/恢复」先导出）。

**Q：换手机怎么迁移？**

「我的 → 备份/恢复 → 导出备份」生成 JSON 到 Downloads/CodeLearning；新机用同一入口导入。备份含 XP、通关、连击、错题本与复习卡。

**Q：可以商用吗？**

可以。MIT 协议，允许修改、分发、商用，保留许可声明即可。名称与 Logo 为保留商标。

## 使用

**Q：判题一直不过怎么办？**

点「问教练 · 只提示不代写」：离线规则引擎给出「诊断 → 三步引导 → 修复示例」。也可以看课程里的 STEPS 思路卡与 HINT。判题只看 assert 结果，不比较你的代码写法。

**Q：为什么明明写对了还是不过？**

常见原因：变量名拼错、没有 `print` 输出（部分题目要求输出，用 `assert x in __stdout__` 校验）、用了中文标点、缩进混用 Tab 与空格。报错面板会给出具体行号与类型。

**Q：终端里 `input()` 能用吗？**

终端**不支持** `input()`（应用内没有 stdin 交互通道），课程「运行」按钮同样不等待输入。需要输入的练习，请把测试数据直接写在代码里。

**Q：死循环了怎么办？**

运行/判题有 8 秒 settrace 看门狗，超时会中断并提示；网页版执行在 Web Worker，15 秒后强制终止。C 阻塞调用（如 `time.sleep`）在部分环境无法中断，网页版需刷新页面。

**Q：支持平板 / 横屏吗？**

当前按手机竖屏设计（1080×2400 验收）；平板适配在 v0.5 路线图。

## 开发与贡献

**Q：怎么本地构建？**

```bash
./gradlew :app:assembleDebug          # Debug APK
./gradlew :app:assembleRelease        # 需 KEYSTORE_PATH 等 4 个环境变量
python -m unittest discover -s tests  # 247 条测试
python tests/validate_content.py      # 57 个 Python 判题项 / 229 用例
```

**Q：怎么加一门新语言？**

实现 `LanguageRuntime` 接口（`runCode/checkExercise/replStart/replPush/version/warmup/ensureStarted`），在 `PyNeonApp.onCreate` 注册，并在 `LanguageIds` 登记。UI 与判题调度无需改动；再用 `content_packs/` 提供课程包。

**Q：为什么 CI 分 GitHub 和 GitCode 两个平台？**

GitHub Actions 负责 Android 构建与签名发布（Runner 可访问 Google SDK 源），也是唯一实际执行的门禁。GitCode 的代码化流水线需要仓库手动开通，push 不会自动触发；`.gitcode/workflows/ci.yml` 作为等价门禁的配置备份保留，2026-10-24 实测其历史运行全部为手动触发，且不含 Pages 同步与链接检查两步。

**Q：发现 Bug / 想提需求？**

用仓库的 Bug Report / Feature Request 模板提 issue；安全问题请走私密渠道（见 [SECURITY.md](SECURITY.md)）。

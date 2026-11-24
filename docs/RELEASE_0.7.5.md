# 0.7.5 发布说明

**状态：准备发布。** 2026-11-24，`v0.7.5`，版本 `versionCode 24`。

0.7.5 是离线边界收口版本：把 Python 运行时的网络/命令操作关进沙箱，把 24 个课程包全部预装，245 讲全新安装、断网也能学。

## 这一版改了什么

| 项目 | 改动 |
| --- | --- |
| Python 网络 | 执行用户代码时拦截 INET/INET6 socket、DNS、`connect`/`bind`/`sendto`；asyncio 事件循环所需的 AF_UNIX socketpair 保持可用 |
| 系统命令 | 拦截 `os.system`、`os.popen`、`os.exec`/`spawn`/`fork`/`kill`、`subprocess.Popen` |
| Java / ctypes 桥 | 拦截 `java`、`jnius`、`ctypes`、`_ctypes`、`chaquopy` 的 import |
| 子线程 | `threading.Thread.start` 在沙箱内创建线程时自动继承 guard；用户代码新开的线程也出不去 |
| 课程包 | 24 个包 + 离线 catalog 打进 APK assets，首启复制到 `filesDir/courses`；245 讲全部可用 |
| 版本更新 | 已有联网下载的更高版本包不会被预装 v2 覆盖；语言中心先读离线 catalog，没网也能列出已装包 |
| 文案 | 语言中心、帮助页、三语 README、官网、用户手册统一为「已预装，可更新」 |

## 边界说明

- 沙箱是**操作级防误触**，不是进程级隔离。执行用户代码期间，正常的 socket/命令/Java/ctypes 调用会被拒绝；同一解释器内不承诺对抗恶意代码绝对不可绕过。
- 用户关掉 `sys.settrace` 后，纯 Python 死循环仍由工作线程 `join` 超时兜底；C 阻塞调用无法强杀线程，这一点和 0.7.4 相同。
- 没有真机/模拟器环境，首启复制 1.7 MB 课程包的耗时、低配机发热与后台被杀未做真机长跑；发布前以构建门禁、Robolectric 和 APK 探针为准。

## 验证

| 项目 | 结果 |
| --- | --- |
| Kotlin 编译 | `compileDebugKotlin` 通过 |
| Android 单测 | 311 项，0 失败，1 跳过 |
| Python 测试 | 260 项通过，551 个 subtest 通过 |
| Lint | 0 Error，47 Warning，6 Hint |
| 内容校验 | 245 讲、22 挑战、267 判题项、1,097 用例，结构问题 0 |
| APK 探针 | 24 个预装包、离线 catalog、245 讲 JSON、六套语法齐全 |
| 软著 | 85 个源文件、17,443 行；源程序册总 349 页，交 60 页；说明书 20 页 |

## 安装包

- 文件名：`mashang-v0.7.5.apk`
- 包名：`com.pyneon.academy`
- 最低系统：Android 10（API 29）
- ABI：`arm64-v8a`
- 大小与 SHA-256：以 GitHub Release 附件和 `.sha256` 文件为准
- 签名：由 GitHub Actions tag 流水线用 `ANDROID_KEYSTORE_BASE64` 还原的正式 keystore 签名，证书 `CN=Mashang`，SHA-256 `9721440afcdb2b830159af84a8c7cb436cd89c891c18f1819f491d749beac610e`，可覆盖 0.7.3/0.7.4

## 下载

- GitHub `badhope`：<https://github.com/badhope/mashang/releases/tag/v0.7.5>
- GitHub `X33834`：<https://github.com/X33834/mashang/releases/tag/v0.7.5>
- GitHub `Morningstar202604`：<https://github.com/Morningstar202604/mashang/releases/tag/v0.7.5>
- Gitee：<https://gitee.com/badhope/mashang/releases/tag/v0.7.5>
- GitCode：<https://gitcode.com/badhope/mashang/releases>

## 升级提醒

- 0.7.5 可直接覆盖 0.7.4 安装，学习进度、错题、复习卡保留。
- 首次启动会把 24 个课程包从 assets 复制到应用私有目录；这是一次性动作，之后启动不会重复复制。
- 如果设备存储紧张，APK 体积比 0.7.4 增加约 0.5 MB（预装包压缩后约 1.7 MB，R8 后净增约 0.5 MB）。

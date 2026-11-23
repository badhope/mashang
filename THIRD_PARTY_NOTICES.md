# 第三方组件与许可

CodeLearning（码上）的自有代码以 MIT 协议发布。下面这些组件由第三方提供，版权归各自作者，使用时遵守对应许可证。

这份清单列的是实际打进 App 的主要运行时和库。测试依赖不进 APK，不在这里列；完整依赖以 `app/build.gradle.kts` 为准，许可证全文在 App 的「关于」里的「开源许可」页。

| 组件 | 许可证 | 用途 | 来源 |
| --- | --- | --- | --- |
| AndroidX / Jetpack Compose | Apache-2.0 | Android UI 框架、导航、数据存储 | https://developer.android.com/jetpack |
| Kotlin / kotlinx | Apache-2.0 | 开发语言、协程、序列化 | https://kotlinlang.org/ |
| Chaquopy | MIT | Android 内嵌 CPython | https://github.com/chaquo/chaquopy |
| CPython 3.13 | PSF-2.0 | Python 语言运行时 | https://www.python.org/ |
| Mozilla Rhino | MPL-2.0 | JavaScript 和 TypeScript 执行引擎 | https://github.com/mozilla/rhino |
| LuaJ 3.0.1 | MIT | Lua 语言运行时 | https://github.com/luaj/luaj |
| SQLite | Public Domain | SQL 课程内存数据库 | https://sqlite.org/ |
| sora-editor | LGPL-2.1 | 代码编辑器、语法高亮、撤销重做 | https://github.com/Rosemoe/sora-editor |
| ZXing Core | Apache-2.0 | 毕业证书二维码 | https://github.com/zxing/zxing |
| Pyodide | MPL-2.0 | 网页版 Python 运行时，不打进 App | https://pyodide.org/ |

## 使用边界

- 这些组件可以随 App 一起分发，不需要从程序里删掉，但必须保留各自的版权和许可声明。
- sora-editor 使用 LGPL-2.1，App 未修改它的源码，许可页里给了源码地址。
- 申请软件著作权时，源程序册只放自己写的 Kotlin、Python 和 JavaScript 代码。上面的第三方库、构建产物、字体图标、模板都不进源程序册，这份清单单独附在说明材料里。
- 第三方组件不能写成自己的原创，也不计入原创代码行数。

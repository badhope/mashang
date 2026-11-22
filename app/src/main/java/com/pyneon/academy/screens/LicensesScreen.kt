package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

/**
 * 开源许可页（应用内合规必备）。
 *
 * 为什么必须有：
 * - **LGPL-2.1**（sora-editor 代码编辑器）要求分发时提供许可文本，并能获取其源码；
 * - **MPL-2.0**（Rhino）要求声明源码可得；
 * - **Apache-2.0**（AndroidX / Compose / ZXing）要求保留 NOTICE；
 * - 各应用商店审核也会检查「是否展示第三方组件许可」。
 *
 * 因此这里列出全部第三方组件、许可证、用途与源码地址，并内嵌完整 MIT 许可正文
 * （本项目自身许可）。完整 LGPL/MPL/Apache 正文较长，给出官方链接与源码地址，
 * 符合各许可证「提供获取方式」的要求。
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    data class Lib(
        val name: String,
        val license: String,
        val purpose: String,
        val url: String
    )

    val libs = listOf(
        Lib("Chaquopy", "MIT", "Android 内嵌 CPython 解释器", "https://github.com/chaquo/chaquopy"),
        Lib("CPython 3.13", "PSF-2.0", "Python 语言运行时", "https://www.python.org/"),
        Lib("Mozilla Rhino", "MPL-2.0", "JavaScript / TypeScript 执行引擎", "https://github.com/mozilla/rhino"),
        Lib("LuaJ 3.0.1", "MIT", "Lua 语言运行时", "https://github.com/luaj/luaj"),
        Lib("SQLite", "Public Domain", "SQL 课程内存数据库", "https://sqlite.org/"),
        Lib("sora-editor", "LGPL-2.1", "代码编辑器（语法高亮 / 缩进 / 撤销）", "https://github.com/Rosemoe/sora-editor"),
        Lib("AndroidX / Jetpack Compose", "Apache-2.0", "UI 框架与应用架构", "https://developer.android.com/jetpack"),
        Lib("Kotlin / kotlinx", "Apache-2.0", "开发语言与协程、序列化", "https://kotlinlang.org/"),
        Lib("ZXing Core", "Apache-2.0", "毕业证书二维码生成", "https://github.com/zxing/zxing"),
        Lib("Pyodide", "MPL-2.0", "网页端运行时（非 App 内置）", "https://pyodide.org/")
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = "开源许可",
            subtitle = "第三方组件与许可全文",
            accent = NeonCyan,
            onBack = onBack
        )

        SectionHeader("本应用许可", accent = NeonGreen)
        NeonCard(accent = NeonGreen) {
            Text(
                "CodeLearning（码上）以 MIT 协议开源。你可以自由使用、修改、分发（含商业用途），" +
                    "只需保留版权声明与本许可文本。「CodeLearning」「码上」名称与标识为保留商标。",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "MIT License\n\n" +
                    "Copyright (c) 2026 CodeLearning (码上) Team\n\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                    "of this software and associated documentation files (the \"Software\"), to deal " +
                    "in the Software without restriction, including without limitation the rights " +
                    "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                    "copies of the Software, and to permit persons to whom the Software is " +
                    "furnished to do so, subject to the following conditions:\n\n" +
                    "The above copyright notice and this permission notice shall be included in all " +
                    "copies or substantial portions of the Software.\n\n" +
                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
                    "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
                    "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
                    "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
                    "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
                    "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE " +
                    "SOFTWARE.",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextDim
            )
            Spacer(Modifier.height(10.dp))
            NeonButton(
                label = "查看完整许可文本",
                accent = NeonCyan,
                onClick = { ShareHelper.openUrl(context, "${AppConstants.REPO_URL}/blob/main/LICENSE") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader("第三方组件", accent = NeonYellow)
        Text(
            "以下组件以依赖方式引入，未修改其源码。点击可在浏览器中查看各自的项目与许可。",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
        libs.forEach { lib ->
            NeonCard(accent = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lib.name, style = MaterialTheme.typography.bodyLarge, color = TextHi)
                        Text(
                            "${lib.license} · ${lib.purpose}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                NeonButton(
                    label = "源码 / 许可",
                    accent = NeonCyan,
                    onClick = { ShareHelper.openUrl(context, lib.url) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader("LGPL 特别说明", accent = NeonGreen)
        NeonCard(accent = NeonGreen) {
            Text(
                "本应用使用了 sora-editor（LGPL-2.1）。LGPL 要求：\n\n" +
                    "1. 以动态链接方式使用该库（本应用通过 Gradle 依赖引入，未静态合并其源码）；\n" +
                    "2. 提供该库的完整源码或获取方式 —— 见上方「sora-editor」条目；\n" +
                    "3. 允许用户替换为该库的其他版本 —— 由于是标准 Gradle 依赖，重新构建即可替换。\n\n" +
                    "本应用未修改 sora-editor 源码。",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
        }

        Text(
            "如对许可有任何疑问，请联系 ${AppConstants.CONTACT_EMAIL}。",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Spacer(Modifier.height(40.dp))
    }
}

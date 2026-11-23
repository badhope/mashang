package com.pyneon.academy.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.ContentCenter
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.debug.CrashHandler
import com.pyneon.academy.debug.DebugLog
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

// 开发者诊断页。离线应用出问题没有服务端日志可查，
// 把环境、运行时、内容包和日志都摊开，用户复制或发邮件给开发者就行。
// 这一页不联网，也不读用户代码。
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    var lastDump by remember { mutableStateOf<String?>(null) }

    val logEntries = remember(refreshKey) { DebugLog.entries() }
    val counts = remember(refreshKey) { DebugLog.counts() }
    val runtimeRows = remember(refreshKey) {
        LanguageRegistry.all().map { rt ->
            rt.id to (rt.displayName to (rt.isAvailable() to rt.version))
        }
    }
    val lessonCount = remember(refreshKey) {
        runCatching { LessonRepository.lessons(context).size }.getOrDefault(-1)
    }
    val mainlineCount = remember(refreshKey) {
        runCatching { LessonRepository.mainline(context).size }.getOrDefault(-1)
    }
    val packs = remember(refreshKey) {
        runCatching { ContentCenter().installedVersions(context) }.getOrDefault(emptyMap())
    }

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
            title = "开发者诊断",
            subtitle = "环境、运行时、内容、日志",
            accent = NeonCyan,
            onBack = onBack
        )

        SectionHeader("运行环境", accent = NeonCyan)
        NeonCard(accent = NeonCyan) {
            DiagRow("应用版本", "v${AppConstants.VERSION_NAME} (code ${AppConstants.VERSION_CODE})")
            DiagRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            DiagRow("设备", "${Build.MANUFACTURER} ${Build.MODEL}")
            DiagRow("ABI", Build.SUPPORTED_ABIS.joinToString(", "))
        }

        SectionHeader("运行时状态", accent = NeonGreen)
        NeonCard(accent = NeonGreen) {
            runtimeRows.forEach { (id, pair) ->
                val (name, state) = pair
                val (available, version) = state
                DiagRow(
                    name,
                    if (available) "$version 可用" else "不可用",
                    NeonGreen.takeIf { available } ?: NeonMagenta
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "能初始化就算可用；不可用时课程页会提示缺运行时。",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }

        SectionHeader("内容状态", accent = NeonYellow)
        NeonCard(accent = NeonYellow) {
            DiagRow("课程总数", if (lessonCount >= 0) "$lessonCount 讲" else "解析失败")
            DiagRow("Python 主线", if (mainlineCount >= 0) "$mainlineCount 讲" else "解析失败")
            DiagRow("已装语言包", if (packs.isEmpty()) "无" else packs.entries.joinToString { "${it.key} v${it.value}" })
        }

        val lastCrash = remember(refreshKey) { CrashHandler.lastCrash(context) }
        if (lastCrash != null) {
            SectionHeader("上次异常退出", accent = NeonMagenta)
            NeonCard(accent = NeonMagenta) {
                Text(
                    "${lastCrash.type}: ${lastCrash.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonMagenta
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    lastCrash.stackHead.take(600),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextDim
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        label = "发送崩溃报告",
                        accent = NeonMagenta,
                        onClick = { CrashHandler.shareByEmail(context, AppConstants.CONTACT_EMAIL) },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        label = "忽略",
                        accent = TextDim,
                        onClick = { CrashHandler.clear(context); refreshKey++ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SectionHeader("运行日志", accent = NeonMagenta)
        NeonCard(accent = NeonMagenta) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiagChip("错误", counts[DebugLog.Level.ERROR] ?: 0, NeonMagenta)
                DiagChip("警告", counts[DebugLog.Level.WARN] ?: 0, NeonYellow)
                DiagChip("信息", counts[DebugLog.Level.INFO] ?: 0, NeonCyan)
                DiagChip("调试", counts[DebugLog.Level.DEBUG] ?: 0, TextDim)
            }
            Spacer(Modifier.height(10.dp))
            if (logEntries.isEmpty()) {
                Text(
                    "暂无日志。正常使用后这里会积累运行时启动、执行、下载等事件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            } else {
                // 只显示最近 80 条，完整内容在导出里。
                logEntries.takeLast(80).reversed().forEach { entry ->
                    val color = when (entry.level) {
                        DebugLog.Level.ERROR -> NeonMagenta
                        DebugLog.Level.WARN -> NeonYellow
                        DebugLog.Level.INFO -> TextMid
                        DebugLog.Level.DEBUG -> TextDim
                    }
                    Text(
                        entry.format(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = color,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                if (logEntries.size > 80) {
                    Text(
                        "… 仅显示最近 80 条，共 ${logEntries.size} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(
                    label = "复制日志",
                    accent = NeonCyan,
                    onClick = {
                        ShareHelper.copyText(context, DebugLog.export())
                        lastDump = "已复制到剪贴板"
                    },
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    label = "导出文件",
                    accent = NeonGreen,
                    onClick = {
                        lastDump = runCatching { DebugLog.dumpToFile(context) }
                            .fold({ "已导出：$it" }, { "导出失败：${it.message}" })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(
                    label = "刷新",
                    accent = NeonYellow,
                    onClick = { refreshKey++; lastDump = "已刷新" },
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    label = "清空",
                    accent = NeonMagenta,
                    onClick = {
                        DebugLog.purge(context)
                        refreshKey++
                        lastDump = "日志已清空"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            lastDump?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMid,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Text(
            "日志只保存在本机，不会上传。不含你的代码内容，只有事件类型与错误摘要。",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DiagRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextHi) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextDim,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiagChip(label: String, count: Int, accent: androidx.compose.ui.graphics.Color) {
    Column(
        Modifier
            .background(SurfaceHigh.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium, color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
    }
}

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.ContentCatalog
import com.pyneon.academy.data.ContentCenter
import com.pyneon.academy.data.ContentPack
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.debug.DebugLog
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 语言中心：运行时和课程包分开列。
 * 没运行时的语言只标「暂不支持」，不给下载入口，不然用户点了也白点。
 */
@Composable
fun LanguageCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val center = remember { ContentCenter() }

    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    var catalogBase by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var installed by remember { mutableStateOf(center.installedVersions(context)) }

    fun refresh() {
        scope.launch {
            loading = true
            message = null
            try {
                val (cat, base) = withContext(Dispatchers.IO) { center.fetchCatalog() }
                catalog = cat
                catalogBase = base
            } catch (e: Exception) {
                message = "连接失败：${e.message?.take(60) ?: "网络不可用"}（离线课程不受影响）"
            }
            loading = false
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonButton(label = "返回", accent = TextMid, onClick = onBack)
            Spacer(Modifier.size(10.dp))
            GlitchText("语言中心", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
        }

        Text(
            "运行时随 APK 内置，课程包按需下载。下载前不会出现在课程列表里。",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )

        SectionHeader("已内置运行时", accent = NeonGreen)
        LanguageRegistry.all().forEach { runtime ->
            NeonCard(accent = NeonGreen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(runtime.displayName, style = MaterialTheme.typography.titleMedium, color = TextHi)
                        Text(runtime.version, style = MaterialTheme.typography.bodySmall, color = TextMid)
                    }
                    Text(
                        if (runtime.isAvailable()) "可离线运行" else "初始化失败",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (runtime.isAvailable()) NeonGreen else NeonMagenta
                    )
                }
            }
        }

        SectionHeader("暂不支持", accent = NeonMagenta)
        NeonCard(accent = NeonMagenta) {
            Text(
                LanguageIds.PLANNED.joinToString("、") { LanguageIds.displayNameOf(it) },
                style = MaterialTheme.typography.bodyMedium,
                color = TextHi
            )
            Text(
                "这些语言需要编译器或原生工具链，无法随 APK 离线提供，也没有下载入口。",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SectionHeader("课程包（按需下载）", accent = NeonYellow)
        Text(
            "每门语言至少 38 讲；SQL 另有 4 讲边界专题。课程拆成基础 / 核心 / 进阶 / 专业四包。点哪包下哪包，也可以整门一次装齐。",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                label = if (loading) "检查中..." else "检查课程包",
                accent = NeonCyan,
                enabled = !loading,
                onClick = { refresh() },
                modifier = Modifier.weight(1f)
            )
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.startsWith("连接")) TextMid else TextDim)
        }

        catalog?.let { cat ->
            val installedLangs = LessonRepository.lessons(context).map { it.lang }.distinct()

            // 下载一个包，批量下载也调这里。
            fun downloadPack(pack: ContentPack): Boolean = try {
                center.download(context, pack, catalogBase)
                installed = center.installedVersions(context)
                LessonRepository.invalidateCache()
                DebugLog.i("Content", "课程包安装成功 ${pack.id} v${pack.version}")
                true
            } catch (e: Exception) {
                DebugLog.e("Content", "课程包下载失败 ${pack.id}", e)
                throw e
            }

            // 同门语言的四个课程包放一张卡，彩蛋、DSA 这类增补包各算各的。
            val isTierPack: (ContentPack) -> Boolean = { it.tier.isNotBlank() }
            val grouped = cat.packs.groupBy { pack ->
                if (isTierPack(pack)) "lang:${pack.lang}" else "extra:${pack.id}"
            }

            grouped.forEach { (groupKey, packs) ->
                val lang = packs.first().lang
                val isExtra = !isTierPack(packs.first())
                val runtime = if (lang.isBlank()) null else LanguageRegistry.get(lang)
                val runtimeReady = runtime == null || runtime.isAvailable()

                val lessonSum = packs.sumOf { it.lessons }
                val sizeSum = packs.sumOf { it.bytes }
                val missing = packs.filter { (installed[it.id] ?: 0) < it.version }
                val allInstalled = missing.isEmpty()

                NeonCard(accent = if (allInstalled) NeonGreen else NeonYellow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    lang.isBlank() -> "通用资源"
                                    isExtra -> packs.first().name
                                    else -> "${LanguageIds.displayNameOf(lang)} 全阶课程"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHi
                            )
                            Text(
                                listOfNotNull(
                                    "$lessonSum 讲".takeIf { lessonSum > 0 },
                                    formatBytes(sizeSum).takeIf { sizeSum > 0 },
                                    "${packs.size} 个包".takeIf { packs.size > 1 },
                                    if (lang.isNotBlank()) {
                                        if (installedLangs.contains(lang)) "已可学习" else "待下载"
                                    } else null
                                ).joinToString("，"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }
                        if (allInstalled) {
                            Text("已装齐", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                        }
                    }

                    if (!runtimeReady) {
                        Text(
                            "缺运行时，暂不可下载",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonMagenta,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        if (missing.isNotEmpty() && packs.size > 1) {
                            NeonButton(
                                label = "全部下载（${missing.size} 包）",
                                accent = NeonCyan,
                                modifier = Modifier.padding(top = 8.dp),
                                onClick = {
                                    scope.launch {
                                        var ok = 0
                                        var failed: String? = null
                                        for (pack in missing) {
                                            message = "下载中 ${pack.name}..."
                                            try {
                                                withContext(Dispatchers.IO) { downloadPack(pack) }
                                                ok++
                                            } catch (e: Exception) {
                                                failed = "${pack.name}：${e.message?.take(40)}"
                                                break
                                            }
                                        }
                                        // 失败提示要压过成功提示，别让用户以为整门都装好了。
                                        message = if (failed != null) {
                                            "已装 $ok/${missing.size} 包，失败：$failed（可重试）"
                                        } else {
                                            "已安装 $ok 个包，课程列表已刷新"
                                        }
                                    }
                                }
                            )
                        }

                        packs.forEach { pack ->
                            val ver = installed[pack.id]
                            val state = when {
                                ver == null -> "下载"
                                ver < pack.version -> "更新到 v${pack.version}"
                                else -> null
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        tierLabel(pack.tier).ifBlank { pack.name },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextHi
                                    )
                                    Text(
                                        buildString {
                                            if (pack.lessons > 0) append("${pack.lessons} 讲，")
                                            if (pack.bytes > 0) append("${formatBytes(pack.bytes)}，")
                                            append("v${pack.version}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextDim
                                    )
                                }
                                if (state != null) {
                                    NeonButton(
                                        label = state,
                                        accent = NeonYellow,
                                        onClick = {
                                            scope.launch {
                                                message = "下载中 ${pack.name}..."
                                                try {
                                                    withContext(Dispatchers.IO) { downloadPack(pack) }
                                                    message = "安装完成，课程列表已刷新"
                                                } catch (e: Exception) {
                                                    message = "下载失败：${e.message?.take(50)}"
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    Text("v$ver", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                                }
                            }
                            Text(
                                pack.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
            if (cat.packs.isEmpty()) {
                Text("目录为空，敬请期待新资料片。", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        }
    }
}

// 课程包分档显示，标题短一点。
private fun tierLabel(tier: String): String = when (tier) {
    "basics" -> "基础篇：入门到能写"
    "core" -> "核心篇：语言主体"
    "advanced" -> "进阶篇：高级应用"
    "expert" -> "专业篇：工程与实战"
    else -> ""
}

// 课程包体积，按大小换单位。
private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

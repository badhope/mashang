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
 * 语言中心：把「运行时是否内置」与「课程包是否已下载」分成两栏展示。
 *
 * 设计红线（2026-10-24 复核后确立）：
 * - 内置运行时（Python / JavaScript / Lua / SQL）显示为可用，课程包按需下载；
 * - 未内置运行时的语言（Java / C++ / Go / Rust）只显示「暂不支持」，
 *   不出现任何下载入口——项目没有运行时下载机制，不能让用户白点。
 * - 课程包未下载前，课程列表里不出现该语言的任何讲；
 *   预置包（js/lua/sql）只下载课程内容，不下载运行时。
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
            NeonButton(label = "◄ 返回", accent = TextMid, onClick = onBack)
            Spacer(Modifier.size(10.dp))
            GlitchText("语言中心 // LANGS", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
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
                LanguageIds.PLANNED.joinToString(" · ") { LanguageIds.displayNameOf(it) },
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
                label = if (loading) "检查中…" else "检查课程包",
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

            /** 下载一个包；返回是否成功。批量下载循环复用，避免两套错误处理。 */
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

            // 按「语言 + 产品线」分组：正式语言四包（基础/核心/进阶/专业）合成一张卡，
            // 增补包（彩蛋 / DSA）单独成卡。旧实现只按 lang 分组，会把「彩蛋 + DSA 基础
            // + DSA 进阶」并进「Python · 全阶课程 11 讲」，批量下载时混装三个不相关包
            // （2026-11 审查发现）。
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
                                    else -> "${LanguageIds.displayNameOf(lang)} · 全阶课程"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHi
                            )
                            Text(
                                buildString {
                                    if (lessonSum > 0) append("$lessonSum 讲")
                                    if (sizeSum > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append(formatBytes(sizeSum))
                                    }
                                    if (packs.size > 1) {
                                        if (isNotEmpty()) append(" · ")
                                        append("${packs.size} 个包")
                                    }
                                    if (lang.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(if (installedLangs.contains(lang)) "已可学习" else "待下载")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }
                        if (allInstalled) {
                            Text("✓ 已装齐", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
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
                                            message = "下载中 ${pack.name}…"
                                            try {
                                                withContext(Dispatchers.IO) { downloadPack(pack) }
                                                ok++
                                            } catch (e: Exception) {
                                                failed = "${pack.name}：${e.message?.take(40)}"
                                                break
                                            }
                                        }
                                        // 失败必须压过成功提示：旧实现只要成功过一个包
                                        // 就把「下载失败」覆盖成「已安装 N 个包」，
                                        // 用户会以为整门都装好了（2026-11 审查发现）。
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
                                            if (pack.lessons > 0) append("${pack.lessons} 讲 · ")
                                            if (pack.bytes > 0) append("${formatBytes(pack.bytes)} · ")
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
                                                message = "下载中 ${pack.name}…"
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
                                    Text("✓ v$ver", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
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

/** 层级标签：课程包按基础/核心/进阶拆包时，UI 只显示这三档，避免三行长标题。 */
private fun tierLabel(tier: String): String = when (tier) {
    "basics" -> "基础篇 · 入门到能写"
    "core" -> "核心篇 · 语言主体"
    "advanced" -> "进阶篇 · 高级应用"
    "expert" -> "专业篇 · 工程与实战"
    else -> ""
}

/** 体积显示：KB 以下给 B，1MB 以下给 KB，其余给一位小数 MB。 */
private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

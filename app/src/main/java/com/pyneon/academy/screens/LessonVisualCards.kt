package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.Block
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.ui.components.ConsoleResult
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.PythonCodeField
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid

// 多语言对照和过程可视化，两块单独放这里，页面文件只留骨架。

@Composable
internal fun CompareCard(
    block: Block.Compare,
    results: Map<Int, RunResult>,
    runningKey: Int?,
    onRun: (Int, Block.CompareColumn) -> Unit
) {
    NeonCard(accent = NeonMagenta) {
        Text(block.title, style = MaterialTheme.typography.titleSmall, color = NeonMagenta)
        Spacer(Modifier.height(10.dp))
        block.columns.forEachIndexed { index, col ->
            val langName = com.pyneon.academy.lang.LanguageIds.displayNameOf(col.lang)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .background(NeonMagenta.copy(alpha = 0.15f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(langName, style = MaterialTheme.typography.labelMedium, color = NeonMagenta)
                }
                if (col.note.isNotEmpty()) {
                    Text(col.note, style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
            Spacer(Modifier.height(4.dp))
            PythonCodeField(
                value = remember(col.code) { TextFieldValue(col.code) },
                onValueChange = {},
                readOnly = true,
                language = col.lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceHigh.copy(alpha = 0.45f))
                    .border(1.dp, NeonMagenta.copy(alpha = 0.15f), MaterialTheme.shapes.extraSmall)
                    .padding(4.dp),
                minHeight = 60
            )
            if (col.runnable) {
                Spacer(Modifier.height(6.dp))
                NeonButton(
                    label = if (runningKey == index) "运行中…" else "运行 $langName",
                    accent = NeonGreen,
                    enabled = runningKey == null,
                    onClick = { onRun(index, col) }
                )
            }
            results[index]?.let {
                Spacer(Modifier.height(6.dp))
                ConsoleResult(result = it, running = false)
            }
            if (index < block.columns.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// 过程拆帧播放。上一帧、下一帧、自动播放、重置都能点。
// 变化项会高亮，哪个变量动了看一眼就知道。
// 简洁风格关掉自动播放，手动翻帧还在。
@Composable
internal fun VisualizeCard(block: Block.Visualize) {
    if (block.frames.isEmpty()) return
    var frame by remember(block) { mutableIntStateOf(0) }
    var playing by remember(block) { mutableStateOf(false) }
    val style = com.pyneon.academy.ui.theme.LocalVisualStyle.current
    // 自动播放：每 900ms 前进一帧，到末帧停止
    LaunchedEffect(playing, block) {
        if (!playing) return@LaunchedEffect
        while (playing && frame < block.frames.lastIndex) {
            kotlinx.coroutines.delay(900)
            if (!playing) break
            frame += 1
        }
        if (frame >= block.frames.lastIndex) playing = false
    }
    val current = block.frames[frame.coerceIn(0, block.frames.lastIndex)]

    NeonCard(accent = NeonYellow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(block.title, style = MaterialTheme.typography.titleSmall, color = NeonYellow)
            Spacer(Modifier.weight(1f))
            Text(
                "${frame + 1} / ${block.frames.size}",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        if (block.caption.isNotEmpty()) {
            Text(
                block.caption,
                style = MaterialTheme.typography.bodySmall,
                color = TextMid,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(10.dp))

        // 当前帧的说明
        Text(current.label, style = MaterialTheme.typography.bodyMedium, color = TextHi)

        Spacer(Modifier.height(8.dp))
        // 变量格子
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            current.items.forEach { item ->
                val changed = item.name in current.highlight
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (changed) NeonYellow.copy(alpha = 0.12f) else Color.Transparent,
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (changed) NeonYellow else TextMid,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (changed) NeonYellow else TextHi
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton(
                label = "上一帧",
                accent = NeonCyan,
                enabled = frame > 0,
                onClick = { playing = false; frame -= 1 }
            )
            NeonButton(
                label = "下一帧",
                accent = NeonCyan,
                enabled = frame < block.frames.lastIndex,
                onClick = { playing = false; frame += 1 }
            )
            // 简洁风格不自动播，免得一直动。
            if (style.pressAnimationEnabled) {
                NeonButton(
                    label = if (playing) "暂停" else "自动播放",
                    accent = NeonGreen,
                    enabled = frame < block.frames.lastIndex || playing,
                    onClick = { playing = !playing }
                )
            }
            NeonButton(
                label = "重置",
                accent = TextDim,
                onClick = { playing = false; frame = 0 }
            )
        }
    }
}

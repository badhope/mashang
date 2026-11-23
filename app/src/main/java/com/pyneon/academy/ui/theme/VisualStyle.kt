package com.pyneon.academy.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 颜色和视觉风格分开设置：换配色不影响动效，换风格也不改色值。
 */
enum class VisualStyle(
    val id: String,
    val label: String,
    val description: String
) {
    // 默认风格，故障字、扫描线、霓虹描边都开着。
    CYBER(
        id = "cyber",
        label = "赛博朋克",
        description = "故障字，扫描线，霓虹描边，切角卡片"
    ),

    // 适合长时间看正文，动效基本关掉。
    MINIMAL(
        id = "minimal",
        label = "简洁",
        description = "静态排版，细描边，圆角卡片，无动效"
    );

    val glitchEnabled: Boolean get() = this == CYBER

    val backdropEnabled: Boolean get() = this == CYBER

    val neonBorderEnabled: Boolean get() = this == CYBER

    val pressAnimationEnabled: Boolean get() = this == CYBER

    // 赛博用切角，简洁用圆角。
    val cardShape: Shape
        get() = if (this == CYBER) CutCornerShape(14.dp) else RoundedCornerShape(12.dp)

    companion object {
        val DEFAULT = CYBER

        fun fromId(id: String?): VisualStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

val LocalVisualStyle = staticCompositionLocalOf { VisualStyle.DEFAULT }

// 必须是 Composable 函数：CompositionLocal 的 current 只能在 Composable 里读。
@Composable
fun isCyberStyle(): Boolean = LocalVisualStyle.current == VisualStyle.CYBER

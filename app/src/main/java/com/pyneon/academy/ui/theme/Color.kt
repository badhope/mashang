package com.pyneon.academy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 界面颜色都从这里取。切主题就是换一套值，调用方不用动。
data class NeonTokens(
    // 背景层级（从最深到最浅）
    val bg0: Color,          // 页面主背景
    val bg1: Color,          // 次级背景（代码/输出面板等）
    val surfaceDark: Color,  // 卡片/弹窗表面
    val surfaceHigh: Color,  // 高亮表面（表头、输入框等）
    // 文字层级（从最亮到最暗）
    val textHi: Color,       // 主文字
    val textMid: Color,      // 次级文字
    val textDim: Color,      // 弱化文字
    // 强调色，每套主题单独调过，保证在各自背景上看得清。
    val primary: Color,      // 主色（青）
    val secondary: Color,    // 辅助色（品红）
    val accent: Color,       // 强调色（黄）
    val success: Color,      // 成功绿
    val danger: Color,       // 错误红
    val gold: Color,         // 金色（任务/奖励）
    val purple: Color,       // 紫色（进阶）
    // 编辑器靠它选深色还是浅色配色。
    val isLight: Boolean = false
)

// 默认深色，赛博霓虹。
val DarkTokens = NeonTokens(
    bg0 = Color(0xFF0E1420),
    bg1 = Color(0xFF182232),
    surfaceDark = Color(0xFF151E2C),
    surfaceHigh = Color(0xFF1E2A3C),
    textHi = Color(0xFFFBFDFF),
    textMid = Color(0xFFB4C4DA),
    textDim = Color(0xFF93A3B8),
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFFF2D78),
    accent = Color(0xFFF7FF00),
    success = Color(0xFF00FF9C),
    danger = Color(0xFFFF4D6A),
    gold = Color(0xFFF7FF00),
    purple = Color(0xFFC35DF5)
)

// 白纸主题，颜色按白底重新调过。
val LightTokens = NeonTokens(
    bg0 = Color(0xFFF5F7FB),
    bg1 = Color(0xFFE9EEF5),
    surfaceDark = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE2E8F2),
    textHi = Color(0xFF10151F),
    textMid = Color(0xFF2E3B4A),
    textDim = Color(0xFF5A6B7E),
    primary = Color(0xFF007E96),
    secondary = Color(0xFFC2185B),
    accent = Color(0xFFB08900),
    success = Color(0xFF00965C),
    danger = Color(0xFFC62828),
    gold = Color(0xFFB08900),
    purple = Color(0xFF7B1FA2),
    isLight = true
)

val LocalNeonTokens = staticCompositionLocalOf { DarkTokens }

// 背景层级
val Bg0: Color
    @Composable get() = LocalNeonTokens.current.bg0
val Bg1: Color
    @Composable get() = LocalNeonTokens.current.bg1
val SurfaceDark: Color
    @Composable get() = LocalNeonTokens.current.surfaceDark
val SurfaceHigh: Color
    @Composable get() = LocalNeonTokens.current.surfaceHigh

// 文字层级
val TextHi: Color
    @Composable get() = LocalNeonTokens.current.textHi
val TextMid: Color
    @Composable get() = LocalNeonTokens.current.textMid
val TextDim: Color
    @Composable get() = LocalNeonTokens.current.textDim

// 强调色
val NeonCyan: Color
    @Composable get() = LocalNeonTokens.current.primary
val NeonMagenta: Color
    @Composable get() = LocalNeonTokens.current.secondary
val NeonGreen: Color
    @Composable get() = LocalNeonTokens.current.success
val NeonYellow: Color
    @Composable get() = LocalNeonTokens.current.gold
val NeonPurple: Color
    @Composable get() = LocalNeonTokens.current.purple
val DangerRed: Color
    @Composable get() = LocalNeonTokens.current.danger

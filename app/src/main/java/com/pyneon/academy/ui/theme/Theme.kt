package com.pyneon.academy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// 按 AppTheme 生成 Material 配色。亮暗只差基底和 error，其余统一 copy 覆盖。
private fun createColorScheme(theme: AppTheme): ColorScheme {
    val base = if (theme.isLight) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = theme.primary,
        onPrimary = theme.background,
        primaryContainer = theme.surfaceHigh,
        onPrimaryContainer = theme.primary,
        secondary = theme.secondary,
        onSecondary = theme.background,
        secondaryContainer = theme.surfaceHigh,
        onSecondaryContainer = theme.secondary,
        tertiary = theme.accent,
        onTertiary = theme.background,
        background = theme.background,
        onBackground = theme.textPrimary,
        surface = theme.surface,
        onSurface = theme.textPrimary,
        surfaceVariant = theme.surfaceHigh,
        onSurfaceVariant = theme.textSecondary,
        outline = theme.textDim,
        error = if (theme.isLight) theme.textDim else DarkTokens.danger,
        onError = theme.background
    )
}

// 默认赛博霓虹配色。给预览和旧调用点用，直接取深色常量。
private val PyNeonColorScheme = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.bg0,
    primaryContainer = DarkTokens.surfaceHigh,
    onPrimaryContainer = DarkTokens.primary,
    secondary = DarkTokens.secondary,
    onSecondary = DarkTokens.bg0,
    secondaryContainer = DarkTokens.surfaceHigh,
    onSecondaryContainer = DarkTokens.secondary,
    tertiary = DarkTokens.success,
    onTertiary = DarkTokens.bg0,
    background = DarkTokens.bg0,
    onBackground = DarkTokens.textHi,
    surface = DarkTokens.surfaceDark,
    onSurface = DarkTokens.textHi,
    surfaceVariant = DarkTokens.surfaceHigh,
    onSurfaceVariant = DarkTokens.textMid,
    outline = DarkTokens.textDim,
    error = DarkTokens.danger,
    onError = DarkTokens.bg0
)

// 默认主题，不跟系统切换。
@Composable
fun PyNeonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PyNeonColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// 支持运行时切明暗。颜色 Token 和视觉风格都从这里下发，界面代码不用自己判断。
@Composable
fun PyNeonTheme(
    appTheme: AppTheme,
    visualStyle: VisualStyle = VisualStyle.DEFAULT,
    fontScale: FontScale = FontScale.DEFAULT,
    content: @Composable () -> Unit
) {
    val tokens = appTheme.toNeonTokens()
    CompositionLocalProvider(
        LocalNeonTokens provides tokens,
        LocalVisualStyle provides visualStyle
    ) {
        MaterialTheme(
            colorScheme = createColorScheme(appTheme),
            typography = typographyFor(fontScale),
            shapes = AppShapes,
            content = content
        )
    }
}

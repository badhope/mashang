package com.pyneon.academy.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class AppTheme(
    val id: String,
    val name: String,
    val primary: Color,        // 主色调
    val secondary: Color,      // 辅助色
    val accent: Color,         // 强调色
    val background: Color,     // 背景色
    val surface: Color,        // 表面色
    val surfaceHigh: Color,    // 高亮表面色
    val textPrimary: Color,    // 主要文字色
    val textSecondary: Color,  // 次要文字色
    val textDim: Color,        // 暗淡文字色
    val isLight: Boolean = false  // 亮色主题，决定用哪套配色。
) {
    // 转成界面用的 NeonTokens，颜色都跟着当前主题走。
    fun toNeonTokens(): NeonTokens = NeonTokens(
        bg0 = background,
        bg1 = surface,
        surfaceDark = surface,
        surfaceHigh = surfaceHigh,
        textHi = textPrimary,
        textMid = textSecondary,
        textDim = textDim,
        primary = primary,
        secondary = secondary,
        accent = accent,
        success = if (isLight) LightTokens.success else DarkTokens.success,
        danger = if (isLight) LightTokens.danger else DarkTokens.danger,
        gold = accent,
        purple = secondary
    )
}

object AppThemes {
    val cyberNeon = AppTheme(
        id = "cyber_neon",
        name = "赛博霓虹",
        primary = Color(0xFF00E5FF),      // 青色
        secondary = Color(0xFFFF2D78),     // 品红
        accent = Color(0xFFF7FF00),        // 黄色
        background = Color(0xFF0E1420),    // 深蓝黑
        surface = Color(0xFF151E2C),       // 深灰蓝
        surfaceHigh = Color(0xFF1E2A3C),   // 浅灰蓝
        textPrimary = Color(0xFFFBFDFF),   // 亮白
        textSecondary = Color(0xFFB4C4DA), // 浅灰蓝
        textDim = Color(0xFF93A3B8)        // 中灰蓝
    )

    val deepSpace = AppTheme(
        id = "deep_space",
        name = "深空灰",
        primary = Color(0xFF64B5F6),      // 浅蓝
        secondary = Color(0xFF90CAF9),     // 更浅蓝
        accent = Color(0xFF42A5F5),        // 中蓝
        background = Color(0xFF17191D),    // 深灰
        surface = Color(0xFF23262C),       // 灰
        surfaceHigh = Color(0xFF31353D),   // 浅灰
        textPrimary = Color(0xFFEDF0F4),   // 亮白
        textSecondary = Color(0xFFC6CCD6), // 灰
        textDim = Color(0xFF9AA1AC)        // 中灰
    )

    val auroraGreen = AppTheme(
        id = "aurora_green",
        name = "极光绿",
        primary = Color(0xFF00E676),      // 亮绿
        secondary = Color(0xFF69F0AE),     // 浅绿
        accent = Color(0xFF00C853),        // 深绿
        background = Color(0xFF0F1F13),    // 深绿黑
        surface = Color(0xFF172A1D),       // 深绿灰
        surfaceHigh = Color(0xFF203A28),   // 浅绿灰
        textPrimary = Color(0xFFEAF7EE),   // 亮白绿
        textSecondary = Color(0xFFB5E4C2), // 浅绿
        textDim = Color(0xFF8CC79A)        // 中绿
    )

    val twilightPurple = AppTheme(
        id = "twilight_purple",
        name = "暮光紫",
        primary = Color(0xFFBB86FC),      // 亮紫
        secondary = Color(0xFFCF6679),     // 粉紫
        accent = Color(0xFF9C27B0),        // 深紫
        background = Color(0xFF171126),    // 深紫黑
        surface = Color(0xFF211A31),       // 深紫灰
        surfaceHigh = Color(0xFF2C243E),   // 浅紫灰
        textPrimary = Color(0xFFF6EEFB),   // 亮白紫
        textSecondary = Color(0xFFD8C3EA), // 浅紫
        textDim = Color(0xFFB795CE)        // 中紫
    )

    val sunsetOrange = AppTheme(
        id = "sunset_orange",
        name = "暮光橙",
        primary = Color(0xFFFFAB40),      // 亮橙
        secondary = Color(0xFFFFD54F),     // 浅黄
        accent = Color(0xFFFF6D00),        // 深橙
        background = Color(0xFF1F140C),    // 深棕黑
        surface = Color(0xFF2A1D12),       // 深棕灰
        surfaceHigh = Color(0xFF352619),   // 浅棕灰
        textPrimary = Color(0xFFFFF6EA),   // 亮橙白
        textSecondary = Color(0xFFFFD9A8), // 浅橙
        textDim = Color(0xFFF5B878)        // 中橙
    )

    val paperLight = AppTheme(
        id = "paper_light",
        name = "白纸模式",
        primary = Color(0xFF007E96),      // 深青
        secondary = Color(0xFFC2185B),     // 深品红
        accent = Color(0xFFB08900),        // 深金
        background = Color(0xFFF5F7FB),    // 近白
        surface = Color(0xFFFFFFFF),       // 纯白
        surfaceHigh = Color(0xFFE2E8F2),   // 浅灰蓝
        textPrimary = Color(0xFF10151F),   // 近黑
        textSecondary = Color(0xFF2E3B4A), // 深灰蓝
        textDim = Color(0xFF5A6B7E),       // 中灰
        isLight = true
    )

    val softEyeCare = AppTheme(
        id = "soft_eye_care",
        name = "柔光护眼",
        primary = Color(0xFF8FBFAF),      // 低饱和青绿
        secondary = Color(0xFFC9A26B),     // 暖棕
        accent = Color(0xFFD9B26A),        // 暖金
        background = Color(0xFF2B2B25),    // 暖深灰
        surface = Color(0xFF34342C),       // 暖灰
        surfaceHigh = Color(0xFF3F3F35),   // 浅暖灰
        textPrimary = Color(0xFFE9E5D9),   // 暖白
        textSecondary = Color(0xFFC4BFA9), // 浅暖灰
        textDim = Color(0xFF9C9680)        // 中暖灰
    )


    val allThemes = listOf(
        cyberNeon,
        deepSpace,
        auroraGreen,
        twilightPurple,
        sunsetOrange,
        softEyeCare,
        paperLight
    )

    // 按 id 找主题，找不到就回赛博霓虹。

    fun getThemeById(id: String): AppTheme {
        return allThemes.find { it.id == id } ?: cyberNeon
    }

    // 自定义主题在 ThemePreference 里的固定 id。
    const val CUSTOM_THEME_ID = "custom"

    // 支持 #RRGGBB 和 #AARRGGBB，格式不对返回 null。
    fun parseHex(hex: String): Color? {
        val trimmed = hex.trim().removePrefix("#")
        if (trimmed.length != 6 && trimmed.length != 8) return null
        return try {
            Color(AndroidColor.parseColor("#$trimmed"))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // 拿一个主色生成整套主题。辅助色和强调色在色环上往后转，饱和度降一点；
    // 主色太暗就换成亮底，不然字看不清。
    fun customTheme(hexColor: String): AppTheme {
        val base = parseHex(hexColor) ?: cyberNeon.primary
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(base.toArgb(), hsv)
        val h = hsv[0]; val s = hsv[1]; val v = hsv[2]
        val secondary = Color(AndroidColor.HSVToColor(floatArrayOf((h + 0.11f) % 1f, (s * 0.85f).coerceIn(0f, 1f), v)))
        val accent = Color(AndroidColor.HSVToColor(floatArrayOf((h + 0.22f) % 1f, (s * 0.70f).coerceIn(0f, 1f), v)))
        val light = v < 0.35f
        val baseTheme = if (light) paperLight else cyberNeon
        return AppTheme(
            id = CUSTOM_THEME_ID,
            name = "自定义",
            primary = base,
            secondary = secondary,
            accent = accent,
            background = baseTheme.background,
            surface = baseTheme.surface,
            surfaceHigh = baseTheme.surfaceHigh,
            textPrimary = baseTheme.textPrimary,
            textSecondary = baseTheme.textSecondary,
            textDim = baseTheme.textDim,
            isLight = light
        )
    }
}

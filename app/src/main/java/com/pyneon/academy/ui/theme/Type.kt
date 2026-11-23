package com.pyneon.academy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    // 标题/标签保留等宽字以维持赛博视觉；正文改用系统无衬线并放大，保证长文阅读清晰
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 15.sp)
)

// 代码统一用等宽，缩进才对得齐。行高按字号比例给，免得不同字体下忽高忽低。
val MonoCode = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    lineHeight = 21.sp
)

// 字号档位。系统字体缩放会把代码块弄乱，所以这里自己放大正文，
// 代码块和编辑器保持原样。
enum class FontScale(val id: String, val label: String, val factor: Float) {
    COMPACT("compact", "紧凑", 0.9f),
    NORMAL("normal", "标准", 1.0f),
    COMFORT("comfort", "舒适", 1.15f),
    LARGE("large", "大字", 1.3f);

    companion object {
        val DEFAULT = NORMAL
        fun fromId(id: String?): FontScale = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

// 按档位生成排版，只放大三档正文。标题和标签不动，
// 不然中文标题在窄屏上会乱换行。
fun typographyFor(scale: FontScale): Typography {
    if (scale == FontScale.NORMAL) return AppTypography
    val f = scale.factor
    fun TextStyle.scaled(): TextStyle = copy(
        fontSize = (fontSize.value * f).sp,
        lineHeight = (lineHeight.value * f).sp
    )
    // 只放大正文，标题和标签保持原样。
    return AppTypography.copy(
        bodyLarge = AppTypography.bodyLarge.scaled(),
        bodyMedium = AppTypography.bodyMedium.scaled(),
        bodySmall = AppTypography.bodySmall.scaled()
    )
}

package com.pyneon.academy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextMid

/**
 * 二级页面统一头部：48dp 返回热区 + 标题 + 可选副标题。
 *
 * 2026-11 去重：备份 / 历史 / 路线 / 复习 / 诊断 / 设置 / 许可 / 关于 / 帮助 / 隐私
 * 十处曾各写一份 Row + Icon + Column，触摸目标与文案层级已经出现漂移。
 * 这里作为单一实现，后续页面只能通过它加头部，避免再复制。
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = NeonCyan,
    useGlitch: Boolean = true
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "返回",
            tint = TextMid,
            modifier = Modifier
                // 视觉 28dp + 四周 10dp padding = 48dp 无障碍最小触摸目标
                .padding(10.dp)
                .size(28.dp)
                .clickable(onClick = onBack)
        )
        Column(Modifier.padding(start = 12.dp)) {
            if (useGlitch) {
                GlitchText(title, style = MaterialTheme.typography.headlineSmall, color = accent)
            } else {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = accent)
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

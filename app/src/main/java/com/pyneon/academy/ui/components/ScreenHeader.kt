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
 * 二级页面的返回头，返回键按 48dp 触摸区域做。
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
                .size(48.dp)
                .clickable(onClick = onBack)
                .padding(10.dp)
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

package com.pyneon.academy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.TextMid

// 语言选择条，终端和课程列表共用。
// options 里是 id 和显示名，selected 为 null 表示全部。
@Composable
fun LangChipRow(
    options: List<Pair<String?, String>>,
    selected: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val active = id == selected
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) Bg0 else TextMid,
                modifier = Modifier
                    .clip(CutCornerShape(6.dp))
                    .background(if (active) accent else accent.copy(alpha = 0.10f))
                    .clickable { onSelect(id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

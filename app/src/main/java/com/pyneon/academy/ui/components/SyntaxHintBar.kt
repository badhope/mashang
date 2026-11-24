package com.pyneon.academy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ai.SyntaxCheck
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim

/**
 * 编辑器下方的语法提示条。只处理括号、引号、中文标点和 Python 冒号，
 * 不抢编辑器的输入焦点，也不阻塞运行。
 */
@Composable
fun SyntaxHintBar(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val hints = remember(code, language) { SyntaxCheck.hints(code, language) }
    if (hints.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        hints.forEach { hint ->
            Text(
                "语法提示：$hint",
                style = MaterialTheme.typography.labelSmall,
                color = NeonYellow
            )
        }
        Text(
            "只是静态检查，最终结果以运行输出为准。",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

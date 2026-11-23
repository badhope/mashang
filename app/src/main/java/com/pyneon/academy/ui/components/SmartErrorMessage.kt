package com.pyneon.academy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.theme.DangerRed
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim

// 把引擎报错换成用户看得懂的话，再给一句修法。
@Composable
fun SmartErrorMessage(
    errorText: String,
    modifier: Modifier = Modifier,
    suggestion: String? = null
) {
    val (friendlyMessage, hint) = parseError(errorText)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceHigh.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(1.dp, NeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = NeonMagenta,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = friendlyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = DangerRed
            )
        }
        
        if (hint != null || suggestion != null) {
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    hint?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                    }
                    suggestion?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                    }
                }
            }
        }
    }
}


private fun parseError(errorText: String): Pair<String, String?> {
    return when {
        // 判题未通过（不是运行时错误：用户代码跑完了，只是断言没过）
        errorText.contains("TestFailed") -> {
            "判题未通过" to "按用例提示修正逻辑后重新判题；需要思路可点「问教练」，不会直接给答案"
        }

        // 语法错误
        errorText.contains("SyntaxError") -> {
            val line = extractLineNumber(errorText)
            "第 $line 行有语法错误" to "检查是否缺少冒号、括号或引号"
        }
        
        // 名称错误（变量未定义）
        errorText.contains("NameError") -> {
            val varName = extractVariableName(errorText)
            "变量 '$varName' 未定义" to "检查拼写是否正确，或者是否忘记先赋值"
        }
        
        // 类型错误
        errorText.contains("TypeError") -> {
            "类型不匹配" to "检查数据类型是否正确（比如不能用字符串加数字）"
        }
        
        // 索引错误
        errorText.contains("IndexError") -> {
            "列表索引超出范围" to "检查索引是否在 0 到长度-1 之间"
        }
        
        // 键错误
        errorText.contains("KeyError") -> {
            val key = extractKeyName(errorText)
            "字典中找不到键 '$key'" to "检查键名是否正确，或者先用 in 操作符检查是否存在"
        }
        
        // 缩进错误
        errorText.contains("IndentationError") -> {
            val line = extractLineNumber(errorText)
            "第 $line 行缩进不正确" to "Python 对缩进很敏感，确保使用空格而不是 Tab"
        }
        
        // 值错误
        errorText.contains("ValueError") -> {
            "值不正确" to "检查传入的值是否符合要求"
        }
        
        // 属性错误
        errorText.contains("AttributeError") -> {
            "对象没有这个属性或方法" to "检查对象类型是否正确，或者查看文档确认可用的方法"
        }
        
        // 导入错误
        errorText.contains("ImportError") || errorText.contains("ModuleNotFoundError") -> {
            "无法导入模块" to "该模块可能不存在或未安装（离线模式下只能使用标准库）"
        }
        
        // 超时错误
        errorText.contains("timeout") || errorText.contains("Timeout") -> {
            "代码运行超时" to "检查是否有无限循环，或尝试简化代码"
        }
        
        // 默认情况
        else -> {
            "代码运行出错" to extractFirstLine(errorText)
        }
    }
}

private fun extractLineNumber(errorText: String): String {
    val regex = """line (\d+)""".toRegex()
    return regex.find(errorText)?.groupValues?.get(1) ?: "?"
}

private fun extractVariableName(errorText: String): String {
    val regex = """name '(\w+)' is not defined""".toRegex()
    return regex.find(errorText)?.groupValues?.get(1) ?: "?"
}

private fun extractKeyName(errorText: String): String {
    val regex = """KeyError: '(\w+)'""".toRegex()
    return regex.find(errorText)?.groupValues?.get(1) ?: "?"
}

private fun extractFirstLine(errorText: String): String? {
    return errorText.lines().firstOrNull { it.isNotBlank() }?.take(60)
}

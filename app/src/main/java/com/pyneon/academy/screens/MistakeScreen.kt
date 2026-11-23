package com.pyneon.academy.screens

import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pyneon.academy.ui.theme.AppTypography
import com.pyneon.academy.ui.theme.LocalNeonTokens
import com.pyneon.academy.ui.theme.MonoCode
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.data.MistakeViewModel
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.NeonTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeScreen(
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit = {},
    lessonId: String? = null,
    viewModel: MistakeViewModel = viewModel()
) {
    val mistakes by if (lessonId != null) {
        viewModel.getByLesson(lessonId).collectAsState(emptyList())
    } else {
        viewModel.allMistakes.collectAsState(emptyList())
    }
    var weakConcepts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        weakConcepts = viewModel.getWeakConcepts()
    }

    var filterConcept by remember { mutableStateOf<String?>(null) }
    val displayMistakes = remember(mistakes, filterConcept) {
        mistakes.filter { m ->
            filterConcept == null || m.conceptTags.contains(filterConcept ?: "")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalNeonTokens.current.bg0
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            NeonTopBar(
                title = lessonId?.let { "错题本 $it" } ?: "错题本",
                onBack = onBack
            ) {
                if (displayMistakes.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空", tint = LocalNeonTokens.current.danger)
                    }
                }
            }

            // 筛选条用横向 Row 自己画。TabRow 在深色主题下会被环境色染成一条实心块。
            if (weakConcepts.isNotEmpty()) {
                val options = listOf<String?>(null) + weakConcepts.map { it.first }
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options.size) { i ->
                        val concept = options[i]
                        val label = if (concept == null) "全部" else "$concept (${
                            weakConcepts.first { it.first == concept }.second
                        })"
                        val selected = filterConcept == concept
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .background(
                                    if (selected) LocalNeonTokens.current.primary.copy(alpha = 0.16f)
                                    else LocalNeonTokens.current.surfaceDark
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) LocalNeonTokens.current.primary
                                    else LocalNeonTokens.current.primary.copy(alpha = 0.25f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                                .clickable { filterConcept = concept }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                style = AppTypography.bodySmall,
                                color = if (selected) LocalNeonTokens.current.primary
                                else LocalNeonTokens.current.textDim
                            )
                        }
                    }
                }
            }

            if (displayMistakes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NeonCard(elevation = 0.dp) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "", tint = LocalNeonTokens.current.textDim, modifier = Modifier.size(48.dp))
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (lessonId != null) "该课暂无错题" else "还没有错题，继续保持。",
                                style = AppTypography.bodyMedium,
                                color = LocalNeonTokens.current.textDim
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayMistakes) { mistake ->
                        MistakeItem(mistake, onRetry = { onOpenLesson(mistake.lessonId) }, onDelete = { viewModel.deleteMistake(mistake.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun MistakeItem(
    mistake: com.pyneon.academy.data.MistakeRecord,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val blockLabel = if (mistake.blockIndex >= 0) "l${mistake.blockIndex + 1}" else "练习"
                Text(
                    "$blockLabel，${mistake.blockType.uppercase()}",
                    style = AppTypography.titleMedium,
                    color = LocalNeonTokens.current.primary
                )
                Text(
                    android.text.format.DateFormat.format("MM-dd HH:mm", mistake.timestamp).toString(),
                    style = AppTypography.bodySmall,
                    color = LocalNeonTokens.current.textDim
                )
            }

            if (mistake.conceptTags.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "标签: ${mistake.conceptTags.joinToString(", ")}",
                    style = AppTypography.bodySmall,
                    color = LocalNeonTokens.current.textDim
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            // 用户代码
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = LocalNeonTokens.current.surfaceDark.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row {
                        Icon(Icons.Default.Code, contentDescription = "", tint = LocalNeonTokens.current.danger, modifier = Modifier.padding(end = 8.dp))
                        Text("你的代码", style = AppTypography.bodySmall, color = LocalNeonTokens.current.danger)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                    Text(mistake.userCode, style = MonoCode, color = LocalNeonTokens.current.textHi)
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

            // 预期和实际
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 两列必须显式等宽：NeonCard 内部 Column 以内容固有宽度参与测量，
                // 只给 weight(1f) 时长 assert 文本会把左列撑到很窄、右列留白。
                Column(modifier = Modifier.weight(1f)) {
                    NeonCard(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("期望输出", style = AppTypography.bodySmall, color = LocalNeonTokens.current.success)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                            Text(mistake.expectedOutput, style = MonoCode, color = LocalNeonTokens.current.textHi)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    NeonCard(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("实际输出", style = AppTypography.bodySmall, color = LocalNeonTokens.current.danger)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                            Text(mistake.actualOutput.ifBlank { "（无输出）" }, style = MonoCode, color = LocalNeonTokens.current.textHi)
                        }
                    }
                }
            }

            if (mistake.errorMessage.isNotBlank()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                NeonCard(elevation = 1.dp, backgroundColor = LocalNeonTokens.current.danger.copy(alpha = 0.1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(Icons.Default.BugReport, contentDescription = "", tint = LocalNeonTokens.current.danger, modifier = Modifier.padding(end = 8.dp))
                            Text("错误信息", style = AppTypography.bodySmall, color = LocalNeonTokens.current.danger)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        Text(mistake.errorMessage, style = MonoCode, color = LocalNeonTokens.current.danger)
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                NeonButton(label = "删除", accent = LocalNeonTokens.current.textDim, onClick = onDelete)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                NeonButton(label = "重做此题", accent = LocalNeonTokens.current.primary, onClick = onRetry)
            }
        }
    }
}

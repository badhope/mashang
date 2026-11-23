package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.data.ReviewCard
import com.pyneon.academy.data.ReviewViewModel
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid

private const val TAG = "ReviewScreen"

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val dueCards by viewModel.dueCards.collectAsState()
    val lessons = remember { LessonRepository.lessons(context) }
    var cardIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDueCards()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = "复习台 // REVIEW",
            subtitle = "间隔记忆，今日到期 ${dueCards.size} 张",
            accent = NeonGreen,
            onBack = onBack
        )

        if (dueCards.isEmpty()) {
            NeutraEmptyState()
        } else {
            val safeIndex = cardIndex.coerceIn(0, dueCards.size - 1)
            val card = dueCards[safeIndex]
            ReviewCardItem(
                card = card,
                lessonTitle = lessons.firstOrNull { it.id == card.lessonId }?.title ?: card.lessonId,
                showAnswer = showAnswer,
                onShowAnswer = { showAnswer = true },
                onGrade = { quality ->
                    viewModel.answerCard(card, quality)
                    showAnswer = false
                    if (safeIndex >= dueCards.size - 1) cardIndex = 0 else cardIndex = safeIndex + 1
                }
            )
        }

        Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun NeutraEmptyState() {
    NeonCard(accent = NeonGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TextDim, modifier = Modifier.size(28.dp))
            Text("今日暂无到期卡片", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
            Text("完成课程后会自动生成复习卡片，到期时这里会提醒你巩固记忆。", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

@Composable
private fun ReviewCardItem(
    card: ReviewCard,
    lessonTitle: String,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit,
    onGrade: (Int) -> Unit
) {
    NeonCard(accent = if (showAnswer) NeonGreen else NeonCyan, filled = false) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lessonTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    blockLabel(card),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
            SectionHeader(card.question, accent = NeonYellow)
            Text(
                "卡片难度 ${"%.1f".format(card.difficulty)}，复习 ${card.repetitions} 次",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            if (!showAnswer) {
                NeonButton(
                    label = "显示答案",
                    accent = NeonGreen,
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                NeonCard(accent = NeonGreen, filled = true) {
                    Text("答案：", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                    Text(
                        card.answer,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextHi
                    )
                }
                Text("回忆效果如何？", style = MaterialTheme.typography.labelMedium, color = TextMid)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeButton("忘记", NeonMagenta, 0, onGrade, Modifier.weight(1f))
                    GradeButton("困难", NeonYellow, 1, onGrade, Modifier.weight(1f))
                    GradeButton("顺利", NeonGreen, 2, onGrade, Modifier.weight(1f))
                    GradeButton("轻松", NeonCyan, 3, onGrade, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GradeButton(label: String, accent: androidx.compose.ui.graphics.Color, quality: Int, onGrade: (Int) -> Unit, modifier: Modifier = Modifier) {
    NeonButton(label = label, accent = accent, onClick = { onGrade(quality) }, modifier = modifier)
}

private fun blockLabel(card: ReviewCard): String =
    when (card.blockType) {
        "exercise" -> "实战演练"
        "quiz" -> "随堂一问"
        "fill" -> "填空补全"
        "order" -> "代码排序"
        else -> card.blockType.uppercase()
    }

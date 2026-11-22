package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.ActivityRecord
import com.pyneon.academy.data.ActivityType
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.data.Progress
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.DarkTokens
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val progress by ProgressStore.flow(context).collectAsState(initial = Progress())
    val lessons = remember { LessonRepository.lessons(context) }
    val challenges = remember { LessonRepository.challenges(context) }

    val lessonTitle = remember(lessons) { lessons.associate { it.id to it.title } }
    val challengeTitle = remember(challenges) { challenges.associate { it.id to it.title } }

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
            title = "学习历史",
            subtitle = "最近 ${progress.recentActivities.size} 条活动记录",
            accent = NeonCyan,
            onBack = onBack
        )

        if (progress.recentActivities.isEmpty()) {
            NeonCard(accent = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = TextDim, modifier = Modifier.size(28.dp))
                    Text("暂无活动记录", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Text("完成课程、通过练习或挑战后，这里会生成你的学习时间线。", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
            }
        } else {
            val grouped = groupByDay(progress.recentActivities)
            grouped.forEach { (dayLabel, records) ->
                SectionHeader(dayLabel, accent = NeonYellow)
                records.forEach { rec ->
                    HistoryRow(
                        rec = rec,
                        lessonTitle = lessonTitle,
                        challengeTitle = challengeTitle
                    )
                }
            }
        }

        Spacer(Modifier.size(8.dp))
    }
}

private fun groupByDay(records: List<ActivityRecord>): List<Pair<String, List<ActivityRecord>>> {
    val fmt = SimpleDateFormat("MM-dd", Locale.getDefault())
    val today = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = SimpleDateFormat("MM-dd", Locale.getDefault()).format(cal.time)
    return records
        .groupBy { fmt.format(Date(it.epochMs)) }
        .map { (day, list) ->
            val label = when (day) {
                today -> "今天"
                yesterday -> "昨天"
                else -> day
            }
            label to list
        }
}

@Composable
private fun HistoryRow(
    rec: ActivityRecord,
    lessonTitle: Map<String, String>,
    challengeTitle: Map<String, String>
) {
    val (label, accent, icon) = describeActivity(rec, lessonTitle, challengeTitle)
    NeonCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = TextHi)
                Text(formatTime(rec.epochMs), style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

private fun describeActivity(
    rec: ActivityRecord,
    lessonTitle: Map<String, String>,
    challengeTitle: Map<String, String>
): Triple<String, Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (rec.type) {
        ActivityType.LESSON_OPEN -> {
            val name = rec.refId.removePrefix("les_").let { lessonTitle[it] ?: rec.refId }
            Triple("开始学习「$name」", DarkTokens.primary, Icons.Outlined.MenuBook)
        }
        ActivityType.LESSON_DONE -> {
            val name = lessonTitle[rec.refId] ?: rec.refId
            Triple("完成课程「$name」", DarkTokens.success, Icons.Outlined.CheckCircle)
        }
        ActivityType.EXERCISE -> {
            val name = rec.refId.removePrefix("les_").let { lessonTitle[it] ?: rec.refId }
            Triple("通过练习「$name」", DarkTokens.secondary, Icons.Outlined.Terminal)
        }
        ActivityType.CHALLENGE -> {
            val id = rec.refId.removePrefix("chal_")
            val name = challengeTitle[id] ?: id
            Triple("通过挑战「$name」", DarkTokens.gold, Icons.Outlined.LocalFireDepartment)
        }
    }
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

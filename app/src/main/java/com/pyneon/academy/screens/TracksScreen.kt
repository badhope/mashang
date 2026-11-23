package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.pyneon.academy.data.Progress
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.data.Track
import com.pyneon.academy.data.TrackCategory
import com.pyneon.academy.data.TrackRepository
import com.pyneon.academy.data.TrackStatus
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextMid

private fun Track.accentColor(): Color = Color(this.accentArgb)

@Composable
fun TracksScreen(
    onBack: () -> Unit,
    onOpenLessons: () -> Unit,
    onOpenContentHub: () -> Unit
) {
    val context = LocalContext.current
    val progress by ProgressStore.flow(context).collectAsState(initial = Progress())
    val categories = remember(progress.completedLessons) {
        TrackRepository.categories(context, progress.completedLessons)
    }
    val totalLessons = categories.sumOf { c -> c.tracks.sumOf { it.lessonCount } }
    val totalDone = categories.sumOf { c -> c.tracks.sumOf { it.doneCount } }

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
            title = "课程路线",
            subtitle = "只列真实内容，已通关 $totalDone/$totalLessons 讲",
            accent = NeonCyan,
            onBack = onBack
        )

        categories.forEach { category ->
            TrackCategorySection(
                category = category,
                onOpenLessons = onOpenLessons,
                onOpenContentHub = onOpenContentHub
            )
        }

        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun TrackCategorySection(
    category: TrackCategory,
    onOpenLessons: () -> Unit,
    onOpenContentHub: () -> Unit
) {
    val readyCount = category.tracks.count { it.status == TrackStatus.READY }
    SectionHeader(
        "${category.name}，${category.tracks.size} 条路线",
        accent = if (readyCount > 0) NeonGreen else NeonCyan
    )
    category.tracks.forEach { track ->
        TrackRow(
            track = track,
            onClick = {
                if (track.status == TrackStatus.READY) onOpenLessons() else onOpenContentHub()
            }
        )
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    val accent = track.accentColor()
    NeonCard(accent = accent, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, color = accent)
                    Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                StatusBadge(track)
                Icon(
                    if (track.status == TrackStatus.READY) Icons.Outlined.PlayArrow else Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            LinearProgressIndicator(
                progress = { track.progressPercent / 100f },
                color = accent,
                trackColor = accent.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Text(track.progressHint, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
    }
}

@Composable
private fun StatusBadge(track: Track) {
    val (label, color, icon) = when (track.status) {
        TrackStatus.READY -> Triple("已装载", track.accentColor(), Icons.Outlined.CheckCircle)
        TrackStatus.NOT_INSTALLED -> Triple("可下载", TextDim, Icons.Outlined.CloudDownload)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

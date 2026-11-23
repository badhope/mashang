package com.pyneon.academy.screens

import com.pyneon.academy.utils.AppConstants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.data.Progress
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.data.Ranks
import com.pyneon.academy.data.Track
import com.pyneon.academy.data.TrackRepository
import com.pyneon.academy.data.dailyMissionDone
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.ProgressRing
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.effects.cyberGrid
import com.pyneon.academy.ui.effects.scanlines
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid

@Composable
fun HomeScreen(
    onOpenLesson: (String) -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenArena: () -> Unit,
    onOpenLessons: () -> Unit,
    onOpenTracks: () -> Unit,
    // 全部学完后，继续学习按钮改成复习入口。
    onOpenReview: () -> Unit = {}
) {
    val context = LocalContext.current
    val progress by ProgressStore.flow(context).collectAsState(initial = Progress())
    val lessons = remember { LessonRepository.lessons(context) }
    // 首页只算 Python 主线。下载的语言包和增补包算进去，两个进度数字会对不上。
    val coreLessons = remember(lessons) { LessonRepository.mainline(context) }
    val runtime = remember { LanguageRegistry.current(context) }
    val pythonVersion = remember { runtime.version }

    // 继续学习优先找下一节没完成的课；全学完了就交给调用方指到复习台，
    // 别把人踹回 l01 重头再来。
    val continueTarget = remember(progress.completedLessons) {
        lessons.firstOrNull { it.id !in progress.completedLessons }?.id
    }
    val doneCount = coreLessons.count { it.id in progress.completedLessons }

    // 断点续读用最近打开的那节，不是最近完成的。和「继续学习」互补：
    // 一个往前推，一个回到还没读完的地方。
    val resumeTarget by produceState("", progress.completedLessons) {
        value = ProgressStore.lastOpenedLesson(context) ?: ""
    }
    val resumeLesson = remember(resumeTarget, lessons) {
        lessons.firstOrNull { it.id == resumeTarget }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .verticalScroll(rememberScrollState())
            .cyberGrid(NeonCyan.copy(alpha = 0.04f), 48.dp)
            .scanlines()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                GlitchText("CodeLearning", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                Text("码上编程学院 v${AppConstants.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
            // 运行时版本串可能很长，限宽单行，不然左边的品牌名会被挤下去。
            Text(
                if (runtime.id == "python") "${runtime.displayName} $pythonVersion 就绪"
                else "${runtime.displayName} 就绪",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreen.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }

        NeonCard(accent = NeonCyan) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProgressRing(
                    fraction = Ranks.fraction(progress.xpTotal),
                    diameter = 104.dp,
                    stroke = 9.dp,
                    accent = NeonCyan,
                    trackColor = SurfaceHigh,
                    modifier = Modifier.size(104.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rank = Ranks.forXp(progress.xpTotal)
                        Text(rank.name, style = MaterialTheme.typography.titleMedium, color = rank.color)
                        Text("${progress.xpTotal} XP", style = MaterialTheme.typography.labelMedium, color = TextMid)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatLine(Icons.Outlined.Bolt, "连击 ${progress.streakDays} 天", NeonYellow)
                    StatLine(Icons.Outlined.CheckCircle, "课程进度 $doneCount/${coreLessons.size}", NeonGreen)
                    val next = Ranks.next(progress.xpTotal)
                    Text(
                        if (next != null) "距「${next.name}」还需 ${next.minXp - progress.xpTotal} XP"
                        else "已达最高阶",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
            }
        }

        SectionHeader("每日任务")
        NeonCard(accent = if (dailyMissionDone(progress)) NeonGreen else NeonYellow, filled = dailyMissionDone(progress)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (dailyMissionDone(progress)) NeonGreen else NeonYellow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("今日通过 1 个练习或挑战", style = MaterialTheme.typography.bodyLarge, color = TextHi)
                    Text("奖励：+30 XP", style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                if (dailyMissionDone(progress)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NeonGreen)
                } else {
                    Text("待完成", style = MaterialTheme.typography.labelMedium, color = TextDim)
                }
            }
        }

        SectionHeader("课程体系")
        val readyTrack = TrackRepository.readyTrack(context, progress.completedLessons)
        if (readyTrack != null) {
            NeonCard(accent = NeonCyan, filled = true, onClick = onOpenTracks) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(NeonCyan.copy(alpha = 0.12f), androidx.compose.foundation.shape.CutCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(readyTrack.title, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        Text("${readyTrack.progressHint}，查看全部路线", style = MaterialTheme.typography.bodySmall, color = TextMid)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                        Text(" 已上线", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                    }
                }
            }
        }

        resumeLesson?.let { resume ->
            if (resume.id !in progress.completedLessons) {
                SectionHeader("继续上次")
                NeonCard(accent = NeonGreen, onClick = { onOpenLesson(resume.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                resume.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextHi
                            )
                            Text(
                                "上次学到这一讲，点这里接着学",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = NeonGreen
                        )
                    }
                }
            }
        }

        SectionHeader("继续行动")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonButton(
                label = if (continueTarget != null) "继续学习" else "已完成，去复习",
                onClick = {
                    // 全部学完就进复习，别再回第一课。
                    if (continueTarget != null) onOpenLesson(continueTarget) else onOpenReview()
                },
                modifier = Modifier.weight(1f),
                accent = NeonMagenta,
                leadingIcon = Icons.Outlined.PlayArrow
            )
            NeonButton(
                label = "课程列表",
                onClick = onOpenLessons,
                modifier = Modifier.weight(1f),
                accent = NeonCyan,
                leadingIcon = Icons.AutoMirrored.Outlined.MenuBook
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonButton(
                label = "终端 REPL",
                onClick = onOpenTerminal,
                modifier = Modifier.weight(1f),
                accent = NeonGreen,
                leadingIcon = Icons.Outlined.Terminal
            )
            NeonButton(
                label = "挑战",
                onClick = onOpenArena,
                modifier = Modifier.weight(1f),
                accent = NeonYellow,
                leadingIcon = Icons.Outlined.LocalFireDepartment
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMid)
    }
}

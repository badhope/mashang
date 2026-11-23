package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.data.Progress
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.data.Ranks
import com.pyneon.academy.data.unlockedAchievements
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.cyberGrid
import com.pyneon.academy.ui.effects.scanlines
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.screens.CertificatePoster
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CertificateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val progress by ProgressStore.flow(context).collectAsState(initial = Progress())
    val lessons = remember { LessonRepository.mainline(context) }
    val unlocked = remember(progress, lessons.size) { unlockedAchievements(progress, lessons.size) }
    val rank = Ranks.forXp(progress.xpTotal)
    val dateText = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .cyberGrid(NeonYellow.copy(alpha = 0.04f), 40.dp)
            .scanlines()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        NeonButton(label = "返回", accent = TextMid, onClick = onBack)
        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(20.dp))
                .background(Bg0)
                .border(2.dp, NeonYellow.copy(alpha = 0.75f), CutCornerShape(20.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), CutCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(44.dp)
                )
                GlitchText("CodeLearning", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                Text("CodeLearning（码上）毕业认证", style = MaterialTheme.typography.titleMedium, color = TextMid)
                Box(Modifier.fillMaxWidth().height(1.dp).background(NeonYellow.copy(alpha = 0.4f)))
                Spacer(Modifier.height(4.dp))
                Text("兹证明 本机学习者", style = MaterialTheme.typography.bodyLarge, color = TextMid)
                Text("已完成全部课程闯关并通过判题实战", style = MaterialTheme.typography.bodyLarge, color = TextHi)
                Text("特此授予 毕业成就认证", style = MaterialTheme.typography.bodyLarge, color = TextMid)
                Spacer(Modifier.height(6.dp))
                Text("段位：${rank.name}", style = MaterialTheme.typography.labelLarge, color = rank.color)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    CertStat("${progress.xpTotal}", "XP")
                    CertStat("${lessons.count { it.id in progress.completedLessons }}/${lessons.size}", "课时")
                    CertStat("${unlocked.size}", "徽章")
                }
                Spacer(Modifier.height(12.dp))
                Text(dateText, style = MaterialTheme.typography.labelSmall, color = TextDim)
                Text("离线学习，设备端运行", style = MaterialTheme.typography.labelSmall, color = NeonGreen.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "这张证书只存在本机，截图就能分享。",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        CertificatePoster.GenerateCertificatePosterButton { success, msg ->
            android.widget.Toast.makeText(
                context,
                if (success) "分享海报已保存到相册" else "生成失败：${msg ?: "未知错误"}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
private fun CertStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = NeonYellow)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
    }
}

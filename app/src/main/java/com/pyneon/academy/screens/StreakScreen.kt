package com.pyneon.academy.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.map
import com.pyneon.academy.data.StreakViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.NeonTopBar
import com.pyneon.academy.ui.theme.DarkTokens
import com.pyneon.academy.ui.theme.AppTypography
import com.pyneon.academy.ui.theme.LocalNeonTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    onBack: () -> Unit,
    onOpenReview: () -> Unit = {},
    viewModel: StreakViewModel = viewModel()
) {
    val streak by viewModel.streak.collectAsState()
    var dueCount by remember { mutableStateOf(0) }

    // Load due count
    LaunchedEffect(Unit) {
        dueCount = viewModel.getDueReviewCount()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalNeonTokens.current.bg0
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            NeonTopBar(title = "连击台", onBack = onBack)

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

            // Main streak card
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "火焰",
                        modifier = Modifier.size(64.dp),
                        tint = LocalNeonTokens.current.secondary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${streak.currentStreak}",
                        style = AppTypography.titleLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Bold),
                        color = LocalNeonTokens.current.secondary
                    )
                    Text(
                        "天连击",
                        style = AppTypography.bodyMedium,
                        color = LocalNeonTokens.current.textDim
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem("最长", "${streak.longestStreak} 天", Icons.Default.EmojiEvents)
                        StatItem("累计", "${streak.totalActiveDays} 天", Icons.Default.CalendarToday)
                        StatItem("待复习", "$dueCount 道", Icons.Default.Refresh)
                    }
                    if (dueCount > 0) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(14.dp))
                        NeonButton(
                            label = "开始复习 $dueCount 张",
                            accent = LocalNeonTokens.current.primary,
                            onClick = onOpenReview,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            // Badges
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("已解锁勋章", style = AppTypography.titleMedium, color = LocalNeonTokens.current.textDim)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    val unlocked = streak.badgesUnlocked.split(",").filter { it.isNotBlank() }
                    if (unlocked.isEmpty()) {
                        Text(
                            "还没有解锁的勋章。保持学习，连续打卡、深夜编码、完成练习都会点亮新的勋章。",
                            style = AppTypography.bodySmall,
                            color = LocalNeonTokens.current.textDim
                        )
                    } else {
                        BadgeGrid(badges = unlocked)
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            // Locked badges preview
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("未解锁勋章", style = AppTypography.titleMedium, color = LocalNeonTokens.current.textDim)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    BadgeGrid(
                        badges = BADGE_DEFS.filter { it.id !in streak.badgesUnlocked.split(",") }.map { it.id },
                        locked = true
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = "", tint = LocalNeonTokens.current.primary, modifier = Modifier.size(20.dp))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = LocalNeonTokens.current.primary)
        Text(label, style = AppTypography.bodySmall, color = LocalNeonTokens.current.textDim)
    }
}

@Composable
fun BadgeGrid(badges: List<String>, locked: Boolean = false) {
    // 徽章是 100dp 方形格子：原来用 LazyColumn 会变成一列竖排 100dp 卡片，
    // 在大屏上留下大片空白（实测 1080×2400 只占左上角）。改成自适应行网格。
    val defs = badges.mapNotNull { id -> BADGE_DEFS.firstOrNull { it.id == id } }
    if (defs.isEmpty()) return
    // 列数按实际可用宽度算（每格最小 92dp），并让每格等宽填满一行：
    // 写死列数 + 固定宽度会在窄屏溢出被裁、在宽屏右侧留一大片空白。
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + 8.dp) / 100.dp).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            defs.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { def -> BadgeItem(def, locked, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun BadgeItem(def: BadgeDef, locked: Boolean, modifier: Modifier = Modifier) {
    val (icon, color) = if (locked) {
        Icons.Default.Lock to LocalNeonTokens.current.textDim.copy(alpha = 0.75f)
    } else {
        def.icon to def.color
    }
    // 注意：NeonCard 内部自带 16dp padding，这里不能再叠加，否则 118dp 高度里
    // 只剩不到 24dp 留给文字，徽章名会被裁掉半截（实测「初登基地」只剩上半）。
    NeonCard(
        modifier = modifier.height(118.dp),
        elevation = if (locked) 0.dp else 4.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = "", tint = color, modifier = Modifier.size(30.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
            Text(
                def.name,
                style = AppTypography.bodySmall,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

data class BadgeDef(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

val BADGE_DEFS = listOf(
    // 徽章色取品牌深色常量（非组合上下文初始化；亮色下作为彩色徽标仍可辨）
    BadgeDef("first_day", "初登基地", Icons.Default.LocalFireDepartment, DarkTokens.secondary),
    BadgeDef("three_days", "三日不绝", Icons.Default.TrendingUp, DarkTokens.primary),
    BadgeDef("week_warrior", "周末战士", Icons.Default.EmojiEvents, DarkTokens.purple),
    BadgeDef("fortnight", "半月常驻", Icons.Default.Star, DarkTokens.primary),
    BadgeDef("month_master", "月度大师", Icons.Default.Diamond, DarkTokens.secondary),
    BadgeDef("centurion", "百日统领", Icons.Default.MilitaryTech, DarkTokens.gold)
)

// Drawable resource for fire icon
// Create app/src/main/res/drawable/ic_fire.xml if needed

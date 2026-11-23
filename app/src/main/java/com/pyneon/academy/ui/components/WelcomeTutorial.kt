package com.pyneon.academy.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.components.NeonButton
import kotlinx.coroutines.launch

data class WelcomePage(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeTutorial(onComplete: () -> Unit) {
    val pages = listOf(
        WelcomePage(
            title = "完全离线学习",
            description = "内置语言运行时\n地铁、飞机、偏远地区都能学\n无需网络，随时学习",
            icon = Icons.Outlined.WifiOff,
            accentColor = NeonCyan
        ),
        WelcomePage(
            title = "游戏化成长",
            description = "XP 经验值系统\n6 大段位晋升\n每日任务 + 成就徽章\n让学习像打游戏一样上瘾",
            icon = Icons.Outlined.EmojiEvents,
            accentColor = NeonYellow
        ),
        WelcomePage(
            title = "独家变量快照",
            description = "运行后立即看到所有变量\n名字、类型、值一目了然\n理解代码执行过程\n其他 App 没有的独家功能",
            icon = Icons.Outlined.Visibility,
            accentColor = NeonGreen
        ),
        WelcomePage(
            title = "多语言课程体系",
            description = "多门语言由浅入深\n从基础语法到生产实战\n每讲配实战判题\n完成后独立写出可用工具",
            icon = Icons.Outlined.Code,
            accentColor = NeonMagenta
        )
    )

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg0)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer
            Spacer(Modifier.height(60.dp))

            // Title
            Text(
                "欢迎使用 CodeLearning",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan
            )
            Text(
                "码上编程学院",
                style = MaterialTheme.typography.bodyMedium,
                color = NeonMagenta,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                WelcomePageContent(pages[page])
            }

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .size(if (isSelected) 12.dp else 8.dp)
                            .background(
                                color = if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }

            // Navigation buttons
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    NeonButton(
                        label = "下一步",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        label = "跳过",
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                } else {
                    NeonButton(
                        label = "开始学习",
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomePageContent(page: WelcomePage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = page.accentColor,
            modifier = Modifier.size(96.dp)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = page.accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextHi,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
        )
    }
}

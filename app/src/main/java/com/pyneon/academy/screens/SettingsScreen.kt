package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.FontScale
import com.pyneon.academy.ui.theme.VisualStyle
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper
import com.pyneon.academy.utils.ThemePreference
import com.pyneon.academy.utils.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClearHistory by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var pendingRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    var currentStyle by remember { mutableStateOf(ThemePreference.getVisualStyle(context)) }
    var currentFontScale by remember { mutableStateOf(ThemePreference.getFontScale(context)) }

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
            title = "系统设置",
            subtitle = "外观 · 数据 · 更新 · 关于",
            accent = NeonGreen,
            onBack = onBack
        )

        ThemePickerSection()

        SectionHeader("外观风格", accent = NeonCyan)
        NeonCard(accent = NeonCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "配色与动效互相独立：喜欢霓虹配色但不想看故障字抖动，可以只把风格切到「简洁」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                VisualStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemePreference.saveVisualStyle(context, style)
                                currentStyle = style
                            }
                            .border(
                                width = if (currentStyle == style) 2.dp else 1.dp,
                                color = if (currentStyle == style) NeonCyan else TextDim.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentStyle == style) NeonCyan else TextHi
                            )
                            Text(
                                style.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim
                            )
                        }
                        if (currentStyle == style) {
                            Text("✓", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Text(
                    "配色主题见上方「配色主题」区块，7 套预设 + 自定义主色可自由搭配。",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
        }

        SectionHeader("字号", accent = NeonYellow)
        NeonCard(accent = NeonYellow) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "只放大课程正文与说明文字，代码块保持原字号——避免缩进和对齐被破坏。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScale.entries.forEach { scale ->
                        Box(
                            Modifier
                                .weight(1f)
                                .background(
                                    if (currentFontScale == scale) NeonYellow.copy(alpha = 0.16f)
                                    else SurfaceDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (currentFontScale == scale) 2.dp else 1.dp,
                                    color = if (currentFontScale == scale) NeonYellow
                                    else TextDim.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    ThemePreference.saveFontScale(context, scale)
                                    currentFontScale = scale
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                scale.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (currentFontScale == scale) NeonYellow else TextHi
                            )
                        }
                    }
                }
                // 实时预览：让用户当场看到效果再决定
                Text(
                    "预览：这是课程正文的显示效果，16sp × ${currentFontScale.factor}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextHi
                )
            }
        }

        SectionHeader("数据记录", accent = NeonGreen)
        NeonCard(accent = NeonGreen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton(
                    label = "清除学习历史",
                    accent = NeonMagenta,
                    leadingIcon = Icons.Outlined.Delete,
                    onClick = { confirmClearHistory = true }
                )
            }
        }

        SectionHeader("应用更新", accent = NeonYellow)
        NeonCard(accent = NeonYellow) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("当前版本", "v${AppConstants.VERSION_NAME} (code ${AppConstants.VERSION_CODE})")
                Text(
                    "检查更新只读取 GitHub Releases 的公开版本号（不发送设备信息、不自动下载）。新版本从 Releases 下载覆盖安装，学习数据不丢失。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                NeonButton(
                    label = when {
                        checkingUpdate -> "检查中…"
                        else -> "检查更新"
                    },
                    accent = NeonYellow,
                    enabled = !checkingUpdate,
                    onClick = {
                        checkingUpdate = true
                        scope.launch {
                            val result = UpdateChecker.fetchLatest()
                            checkingUpdate = false
                            updateMessage = result.fold(
                                onSuccess = { info ->
                                    if (UpdateChecker.isNewer(info.version, AppConstants.VERSION_NAME)) {
                                        pendingRelease = info
                                        "发现新版本 v${info.version}（当前 v${AppConstants.VERSION_NAME}）"
                                    } else {
                                        "已是最新版本（v${AppConstants.VERSION_NAME}）"
                                    }
                                },
                                onFailure = { "检查失败：网络不可用或无发布记录（不影响离线学习）" }
                            )
                        }
                    }
                )
                updateMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                }
                pendingRelease?.let { info ->
                    NeonButton(
                        label = "打开发布页",
                        accent = NeonGreen,
                        onClick = { ShareHelper.openUrl(context, info.url) }
                    )
                }
            }
        }

        SectionHeader("关于", accent = NeonCyan)
        NeonCard(accent = NeonCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("应用", "${AppConstants.APP_NAME}（${AppConstants.APP_NAME_CN}）")
                InfoRow("版本", "v${AppConstants.VERSION_NAME} (code ${AppConstants.VERSION_CODE})")
                InfoRow("开发者", AppConstants.DEV_NAME)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        label = "隐私政策",
                        accent = NeonCyan,
                        leadingIcon = Icons.Outlined.Lock,
                        onClick = { ShareHelper.openUrl(context, AppConstants.PRIVACY_POLICY_URL) },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        label = "用户协议",
                        accent = NeonCyan,
                        leadingIcon = Icons.Outlined.Article,
                        onClick = { ShareHelper.openUrl(context, AppConstants.TERMS_URL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SectionHeader("开发者", accent = TextDim)
        NeonCard(accent = TextDim) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "遇到问题时，先把诊断日志复制或导出，能大幅缩短定位时间。日志只留在本机，不上传。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                NeonButton(
                    label = "开发者诊断",
                    accent = NeonCyan,
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader("联系与开源", accent = NeonMagenta)
        NeonCard(accent = NeonMagenta) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        label = "联系开发者",
                        accent = NeonGreen,
                        leadingIcon = Icons.Outlined.Email,
                        onClick = { ShareHelper.sendEmail(context, AppConstants.CONTACT_EMAIL, "关于 CodeLearning 的建议") },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        label = "开源仓库",
                        accent = NeonYellow,
                        leadingIcon = Icons.Outlined.Code,
                        onClick = { ShareHelper.openUrl(context, AppConstants.REPO_URL) },
                        modifier = Modifier.weight(1f)
                    )
                }
                InfoRow("开发者邮箱", AppConstants.CONTACT_EMAIL)
                InfoRow("开源性质", "MIT 开源许可（允许商用，保留许可声明）")
            }
        }

        Spacer(Modifier.size(8.dp))
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("清除学习历史？", color = NeonYellow, style = MaterialTheme.typography.titleLarge) },
            text = { Text("将删除全部活动时间线记录（不影响 XP、课程进度与成就）。", color = TextMid) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { ProgressStore.clearActivityLog(context) }
                    confirmClearHistory = false
                }) { Text("清除", color = NeonMagenta) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("取消", color = NeonCyan) }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$key：", style = MaterialTheme.typography.bodySmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextMid)
    }
}

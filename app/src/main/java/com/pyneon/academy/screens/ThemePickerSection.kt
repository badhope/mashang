package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.AppThemes
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.ThemePreference

// 配色主题选择器。设置页和「我的」页都用这一份，省得两边各写一套。
@Composable
fun ThemePickerSection(
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    var currentThemeId by remember { mutableStateOf(ThemePreference.getCurrentThemeId(context)) }
    var customColorInput by remember { mutableStateOf(ThemePreference.getCustomColor(context)) }
    var customError by remember { mutableStateOf<String?>(null) }

    Column(modifier) {
        if (showHeader) {
            SectionHeader("配色主题", accent = NeonYellow)
        }
        NeonCard(accent = NeonYellow) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "配色与「外观风格」「字号」互相独立，可自由组合。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                Text("选择你喜欢的颜色主题", style = MaterialTheme.typography.bodySmall, color = TextMid)
                AppThemes.allThemes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemePreference.saveTheme(context, theme.id)
                                currentThemeId = theme.id
                            }
                            .border(
                                width = if (currentThemeId == theme.id) 2.dp else 1.dp,
                                color = if (currentThemeId == theme.id) theme.primary else TextDim.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(theme.primary, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(theme.secondary, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(theme.accent, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentThemeId == theme.id) theme.primary else TextHi
                            )
                        }
                        if (currentThemeId == theme.id) {
                            Text("已选", color = theme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 自定义颜色
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentThemeId = AppThemes.CUSTOM_THEME_ID
                            // 输入合法就直接生效，不用再点应用。
                            if (AppThemes.parseHex(customColorInput) != null) {
                                ThemePreference.saveCustomColor(context, customColorInput.trim())
                                ThemePreference.saveTheme(context, AppThemes.CUSTOM_THEME_ID)
                            }
                        }
                        .border(
                            width = if (currentThemeId == AppThemes.CUSTOM_THEME_ID) 2.dp else 1.dp,
                            color = if (currentThemeId == AppThemes.CUSTOM_THEME_ID)
                                AppThemes.parseHex(customColorInput) ?: TextDim
                            else TextDim.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(AppThemes.parseHex(customColorInput) ?: TextDim, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "自定义颜色",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (currentThemeId == AppThemes.CUSTOM_THEME_ID)
                            AppThemes.parseHex(customColorInput) ?: TextHi
                        else TextHi,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentThemeId == AppThemes.CUSTOM_THEME_ID) {
                        Text("已选", color = AppThemes.parseHex(customColorInput) ?: TextDim, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (currentThemeId == AppThemes.CUSTOM_THEME_ID) {
                    Text(
                        "点下面的色板，或者输入 #RRGGBB。整套界面会立即跟着变；主色太暗会自动换亮底，保证看得清。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(4.dp))
                    val presets = listOf(
                        "#00E5FF", "#FF2D78", "#00E676", "#BB86FC", "#FFAB40",
                        "#FFD54F", "#4DD0E1", "#FF5252", "#FFFFFF", "#7C4DFF"
                    )
                    presets.chunked(5).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            rowColors.forEach { hex ->
                                val c = AppThemes.parseHex(hex) ?: TextDim
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(c, CircleShape)
                                        .border(
                                            width = if (customColorInput.trim() == hex) 3.dp else 1.dp,
                                            color = if (customColorInput.trim() == hex) SurfaceDark else TextDim.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            customColorInput = hex
                                            customError = null
                                            ThemePreference.saveCustomColor(context, hex)
                                            ThemePreference.saveTheme(context, AppThemes.CUSTOM_THEME_ID)
                                            currentThemeId = AppThemes.CUSTOM_THEME_ID
                                        }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customColorInput,
                        onValueChange = { customColorInput = it; customError = null },
                        singleLine = true,
                        label = { Text("主色（#RRGGBB）", style = MaterialTheme.typography.labelMedium) },
                        isError = customError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                            autoCorrect = false
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    customError?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = NeonMagenta)
                    }
                    Spacer(Modifier.height(6.dp))
                    NeonButton(
                        label = "应用此色",
                        accent = NeonCyan,
                        onClick = {
                            if (AppThemes.parseHex(customColorInput) == null) {
                                customError = "颜色格式应为 #RRGGBB，例如 #00E5FF"
                            } else {
                                customError = null
                                ThemePreference.saveCustomColor(context, customColorInput.trim())
                                ThemePreference.saveTheme(context, AppThemes.CUSTOM_THEME_ID)
                                currentThemeId = AppThemes.CUSTOM_THEME_ID
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

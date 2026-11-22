package com.pyneon.academy.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.theme.AppTypography
import com.pyneon.academy.ui.theme.LocalNeonTokens

/**
 * 带 Glitch 标题的 Material3 顶栏。
 *
 * 2026-11 去重：错题本与连击台曾各写一份 TopAppBar + navigationIcon + colors，
 * 返回按钮的 contentDescription 与触摸热区容易再次漂移；统一收敛到这里。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = {
            GlitchText(
                title,
                style = AppTypography.titleLarge,
                color = LocalNeonTokens.current.primary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = LocalNeonTokens.current.primary
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LocalNeonTokens.current.bg0,
            titleContentColor = LocalNeonTokens.current.primary
        )
    )
}

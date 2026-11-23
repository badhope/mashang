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

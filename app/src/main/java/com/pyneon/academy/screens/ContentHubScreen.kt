package com.pyneon.academy.screens

import androidx.compose.runtime.Composable

/**
 * 旧路由 contenthub 还指向语言中心。
 * 课程包下载只留 LanguageCenterScreen 一套，这里只做转发，免得两边状态对不上。
 */
@Composable
fun ContentHubScreen(onBack: () -> Unit) = LanguageCenterScreen(onBack = onBack)

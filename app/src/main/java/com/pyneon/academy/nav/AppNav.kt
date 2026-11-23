package com.pyneon.academy.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pyneon.academy.screens.ArenaScreen
import com.pyneon.academy.screens.BootScreen
import com.pyneon.academy.screens.CertificateScreen
import com.pyneon.academy.screens.ChallengeDetailScreen
import com.pyneon.academy.screens.ContentHubScreen
import com.pyneon.academy.screens.DiagnosticsScreen
import com.pyneon.academy.screens.HomeScreen
import com.pyneon.academy.screens.LessonDetailScreen
import com.pyneon.academy.screens.LanguageCenterScreen
import com.pyneon.academy.screens.LessonsScreen
import com.pyneon.academy.screens.MistakeScreen
import com.pyneon.academy.screens.ProfileScreen
import com.pyneon.academy.screens.StreakScreen
import com.pyneon.academy.screens.TerminalScreen
import com.pyneon.academy.screens.TracksScreen
import com.pyneon.academy.screens.AboutScreen
import com.pyneon.academy.screens.BackupScreen
import com.pyneon.academy.screens.HelpScreen
import com.pyneon.academy.screens.HistoryScreen
import com.pyneon.academy.screens.LicensesScreen
import com.pyneon.academy.screens.ReviewScreen
import com.pyneon.academy.screens.SettingsScreen
import com.pyneon.academy.screens.PrivacyConsentDialog
import com.pyneon.academy.screens.PrivacyPolicyScreen
import com.pyneon.academy.screens.finishApp
import com.pyneon.academy.ui.theme.Bg1
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.components.WelcomeTutorial
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.utils.AppPrefs

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val TOP_DESTS = listOf(
    TopDest("home", "首页", Icons.Outlined.SpaceDashboard),
    TopDest("lessons", "课程", Icons.AutoMirrored.Outlined.MenuBook),
    TopDest("terminal", "终端", Icons.Outlined.Terminal),
    TopDest("arena", "挑战", Icons.Outlined.LocalFireDepartment),
    TopDest("profile", "我的", Icons.Outlined.Person)
)

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "boot"
    val showBar = TOP_DESTS.any { it.route == currentRoute }
    
    // 记录这次启动是不是第一次，欢迎页只看一次。
    val context = LocalContext.current
    val showWelcome = remember { mutableStateOf(AppPrefs.isFirstLaunch(context)) }
    // 首次启动要先拿到隐私同意。
    var showConsent by remember { mutableStateOf(!AppPrefs.isPrivacyConsented(context)) }
    // 同意后要让 BootScreen 用 consented=true 重走一遍。只把 showConsent 置 false
    // 没用，原来那次 LaunchedEffect 已经跑完离开了，运行时就再也不会初始化。
    // 所以同意时清回 boot，再用 consentEpoch 强制重建。
    var consentEpoch by remember { mutableIntStateOf(0) }

    // 崩溃后首次启动提示：CrashHandler 在崩溃时已把记录写入本机，
    // 但普通用户不会进「开发者诊断」，所以这里主动问一次（一次性，处理后即清除标记）。
    var showCrashNotice by remember {
        mutableStateOf(com.pyneon.academy.debug.CrashHandler.hasPendingCrash(context))
    }

    Scaffold(
        containerColor = Bg0,
        bottomBar = {
            // 键盘弹出时藏起底栏，不然会被键盘盖住，还挤占输入区。
            if (showBar && !WindowInsets.isImeVisible) {
                NavigationBar(containerColor = Bg1) {
                    TOP_DESTS.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label, tint = if (currentRoute == dest.route) NeonCyan else TextDim) },
                            label = { Text(dest.label, style = MaterialTheme.typography.labelSmall, color = if (currentRoute == dest.route) NeonCyan else TextDim) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "boot",
            modifier = Modifier
                .padding(padding)
                .background(Bg0),
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(260),
                    initialOffsetX = { it / 4 }
                ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260))
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(240),
                    targetOffsetX = { it / 4 }
                ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(240))
            }
        ) {
            composable("boot") {
                // 没同意就不初始化、不写数据，BootScreen 自己会跳过。
                androidx.compose.runtime.key(consentEpoch) {
                    BootScreen(
                        consented = !showConsent,
                        onDone = {
                            if (showWelcome.value) {
                                navController.navigate("welcome") {
                                    popUpTo("boot") { inclusive = true }
                                }
                            } else {
                                navController.navigate("home") {
                                    popUpTo("boot") { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
            
            composable("welcome") {
                // 进欢迎页就标记看过，按返回也不会再弹。
                LaunchedEffect(Unit) {
                    AppPrefs.markFirstLaunchComplete(context)
                }
                
                WelcomeTutorial(onComplete = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                })
            }
            composable("home") {
                HomeScreen(
                    onOpenLesson = { id -> navController.navigate("lesson/$id") },
                    onOpenTerminal = { navController.navigate("terminal") },
                    onOpenArena = { navController.navigate("arena") },
                    onOpenLessons = { navController.navigate("lessons") },
                    onOpenTracks = { navController.navigate("tracks") },
                    onOpenReview = { navController.navigate("review") }
                )
            }
            composable("tracks") {
                TracksScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLessons = { navController.navigate("lessons") },
                    onOpenContentHub = { navController.navigate("contenthub") }
                )
            }
            composable("lessons") {
                LessonsScreen(openLesson = { id -> navController.navigate("lesson/$id") })
            }
            composable("lesson/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LessonDetailScreen(
                    lessonId = id,
                    onBack = { navController.popBackStack() },
                    // 学完直接进下一课，替换当前页，别让回退栈一直长。
                    onNextLesson = { nextId ->
                        navController.navigate("lesson/$nextId") {
                            popUpTo("lesson/$id") { inclusive = true }
                        }
                    }
                )
            }
            composable("terminal") { TerminalScreen() }
            composable("arena") {
                ArenaScreen(openChallenge = { id -> navController.navigate("challenge/$id") })
            }
            composable("challenge/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ChallengeDetailScreen(challengeId = id, onBack = { navController.popBackStack() })
            }
            composable("profile") { ProfileScreen(
                onOpenContentHub = { navController.navigate("contenthub") },
                onOpenLanguageCenter = { navController.navigate("langcenter") },
                onOpenCertificate = { navController.navigate("certificate") },
                onOpenStreak = { navController.navigate("streak") },
                onOpenMistakes = { navController.navigate("mistakes") },
                onOpenPrivacy = { navController.navigate("privacy") },
                onOpenHelp = { navController.navigate("help") },
                onOpenAbout = { navController.navigate("about") },
                onOpenBackup = { navController.navigate("backup") },
                onOpenHistory = { navController.navigate("history") },
                onOpenReview = { navController.navigate("review") },
                onOpenSettings = { navController.navigate("settings") }
            ) }
            composable("contenthub") { ContentHubScreen(onBack = { navController.popBackStack() }) }
            composable("langcenter") { LanguageCenterScreen(onBack = { navController.popBackStack() }) }
            // 备份入口之前没注册，点了会崩，这里补上。
            composable("backup") { BackupScreen(onBack = { navController.popBackStack() }) }
            composable("certificate") { CertificateScreen(onBack = { navController.popBackStack() }) }
            composable("streak") {
                StreakScreen(
                    onBack = { navController.popBackStack() },
                    onOpenReview = { navController.navigate("review") }
                )
            }
            composable("review") { ReviewScreen(onBack = { navController.popBackStack() }) }
            composable("history") { HistoryScreen(onBack = { navController.popBackStack() }) }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDiagnostics = { navController.navigate("diagnostics") }
                )
            }
            composable("diagnostics") { DiagnosticsScreen(onBack = { navController.popBackStack() }) }
            composable("mistakes") {
                MistakeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLesson = { id -> navController.navigate("lesson/$id") }
                )
            }
            composable("mistakes/{lessonId}") { entry ->
                val id = entry.arguments?.getString("lessonId").orEmpty()
                MistakeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLesson = { lid -> navController.navigate("lesson/$lid") },
                    lessonId = id
                )
            }
            composable("privacy") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
            composable("help") { HelpScreen(onBack = { navController.popBackStack() }) }
            composable("about") {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLicenses = { navController.navigate("licenses") }
                )
            }
            composable("licenses") { LicensesScreen(onBack = { navController.popBackStack() }) }
        }

        if (showConsent) {
            PrivacyConsentDialog(
                onConsent = {
                    AppPrefs.setPrivacyConsented(context, true)
                    showConsent = false
                    // 关键：把导航栈清回 boot，并递增 key 重建 BootScreen，
                    // 让它这次以 consented=true 真正初始化运行时。
                    consentEpoch += 1
                    navController.navigate("boot") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onDecline = { finishApp(context) }
            )
        }

        // 崩溃后首次启动的一次性提示：优先级低于隐私同意（首次安装两弹连出会很吵）
        if (showCrashNotice && !showConsent) {
            val crash = com.pyneon.academy.debug.CrashHandler.lastCrash(context)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCrashNotice = false },
                title = {
                    Text(
                        "上次未正常退出",
                        color = com.pyneon.academy.ui.theme.NeonYellow,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column {
                        Text(
                            "应用上次异常退出（${crash?.type ?: "未知"}）。你的学习数据保存在本机，不受影响。",
                            color = com.pyneon.academy.ui.theme.TextMid
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "如果反复出现，欢迎把诊断日志发给开发者定位原因。",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.pyneon.academy.ui.theme.TextDim
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        com.pyneon.academy.debug.CrashHandler.clear(context)
                        showCrashNotice = false
                    }) { Text("知道了", color = com.pyneon.academy.ui.theme.NeonCyan) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCrashNotice = false
                        // 不清除记录，让用户能到诊断页里导出/发送
                        navController.navigate("diagnostics")
                    }) { Text("查看诊断", color = com.pyneon.academy.ui.theme.TextMid) }
                },
                containerColor = com.pyneon.academy.ui.theme.SurfaceDark
            )
        }
    }
}

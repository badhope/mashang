package com.pyneon.academy.screens

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.cyberGrid
import com.pyneon.academy.ui.effects.scanlines
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.delay

@Composable
fun BootScreen(
    onDone: () -> Unit,
    // 没同意隐私政策前不能处理数据，所以只播动画，不预热运行时、不写连击、不记日志。
    consented: Boolean = true
) {
    val context = LocalContext.current
    val streakVm: com.pyneon.academy.data.StreakViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    var pyStatus by remember { mutableStateOf("RUN") }
    // 预热失败就停在恢复页，别硬着头皮进首页。
    var bootFailed by remember { mutableStateOf(false) }
    // 重试得真的重跑。LaunchedEffect(Unit) 只跑一次，光改状态界面会一直停在进度条。
    var retryKey by remember { mutableIntStateOf(0) }
    // 失败原因留给用户反馈，不打印敏感数据。
    var bootError by remember { mutableStateOf<String?>(null) }
    // 本页是否已经放行过（避免切页后回调重复触发）
    var finished by remember { mutableStateOf(false) }
    
    // 版本号从构建配置来。
    val appVersion = "v${com.pyneon.academy.utils.AppConstants.VERSION_NAME}"
    
    val bootLines = remember(pyStatus) {
        listOf(
            "码上 $appVersion .............. OK",
            "初始化执行引擎 ............ OK",
            when (pyStatus) {
                "RUN" -> "加载语言运行时 ............. 校验中"
                "OK" -> "加载语言运行时 ............. OK"
                else -> "加载语言运行时 ............. FAIL"
            },
            "加载本地课程数据 .............. OK",
            "准备离线学习环境 .............. 完成",
            "",
            "> 欢迎回来，编程学徒_"
        )
    }
    var shown by remember { mutableIntStateOf(0) }

    LaunchedEffect(consented, retryKey) {
        if (finished) return@LaunchedEffect
        shown = 1
        delay(170L)
        shown = 2
        delay(170L)

        if (consented) {
            // 同意了才初始化运行时、写连击。
            val runtime = LanguageRegistry.current(context)
            // ensureStarted 必须放 IO 线程。Chaquopy 的 Python.start 会同步加载
            // 5MB 的 libpython 和标准库，跑主线程会白屏。
            //
            // 原生库缺失、标准库解压失败都会抛 Throwable，不接住会直接闪退，
            // 连恢复页都看不到。
            val ok = runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runtime.ensureStarted(context)
                    runtime.warmup()
                }
            }.onFailure { e ->
                bootError = "${e.javaClass.simpleName}: ${e.message?.take(120) ?: ""}"
            }.getOrDefault(false)
            pyStatus = if (ok) "OK" else "FAIL"
            if (ok) {
                streakVm.onAppOpen()
            } else {
                // 预热失败不能装作没事进首页，不然用户只看到「就绪」，一运行就报错。
                bootFailed = true
                return@LaunchedEffect
            }
        } else {
            // 没同意就跳过数据处理，交给上层弹框。动画播完 onDone 照常走，
            // 用户点同意后 AppNav 会重建本页，不重建运行时就永远不初始化。
            pyStatus = "SKIP"
        }

        shown = 3
        for (i in 3 until bootLines.size) {
            shown = i + 1
            delay(190L)
        }
        delay(650)
        finished = true
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .cyberGrid(NeonCyan.copy(alpha = 0.05f), 44.dp)
            .scanlines()
            .padding(28.dp)
    ) {
        Column(Modifier.align(Alignment.Center)) {
            GlitchText("码上", style = MaterialTheme.typography.headlineLarge, color = NeonCyan)
            Text(
                "编程学院",
                style = MaterialTheme.typography.labelMedium,
                color = NeonMagenta,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bootLines.take(shown).forEachIndexed { idx, line ->
                    val failLine = idx == 2 && pyStatus == "FAIL"
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (failLine) NeonMagenta else NeonGreen.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (bootFailed) {
                // 初始化失败要给个出口，不能只显示一行 FAIL 就放人进去。
                Text(
                    "运行时初始化失败，无法执行代码。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonMagenta
                )
                bootError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    "可尝试：重启应用 / 查看诊断日志 / 反馈给开发者",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                com.pyneon.academy.ui.components.NeonButton(
                    label = "重试",
                    accent = NeonCyan,
                    onClick = {
                        // retryKey 变化会重启 LaunchedEffect，真正重新初始化
                        bootFailed = false
                        pyStatus = "RUN"
                        retryKey += 1
                    }
                )
                Spacer(Modifier.height(8.dp))
                com.pyneon.academy.ui.components.NeonButton(
                    label = "仍要继续（可能无法运行代码）",
                    accent = TextDim,
                    onClick = { onDone() }
                )
            } else {
                val fraction = if (bootLines.isEmpty()) 0f else shown.toFloat() / bootLines.size
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    trackColor = NeonCyan.copy(alpha = 0.12f)
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Text("完全离线，不用网络，随时能学", style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
    }
}

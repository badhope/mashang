package com.pyneon.academy.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.BackupUtil
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            confirmImport = true
        }
    }

    fun runExport() {
        scope.launch {
            status = BackupUtil.exportBackup(context).fold(
                onSuccess = { _ ->
                    true to "备份已导出到 下载/CodeLearning 文件夹。"
                },
                onFailure = { e -> false to (e.message ?: "导出失败") }
            )
        }
    }

    fun runImport(uri: Uri) {
        scope.launch {
            status = BackupUtil.importBackup(context, uri).fold(
                onSuccess = {
                    LessonRepository.invalidateCache()
                    true to "导入成功：课程进度、经验值、勋章已恢复。"
                },
                onFailure = { e -> false to (e.message ?: "导入失败") }
            )
        }
    }

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
            title = "备份中心 // BACKUP",
            subtitle = "本地进度导出与恢复",
            accent = NeonCyan,
            onBack = onBack
        )

        NeonCard(accent = NeonCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("备份内容", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                Text(
                    "包含：经验值、通关课程、已做练习、连击统计、徽章、错题本与复习卡片。\n" +
                        "不包含：学习历史时间线（本地轻量记录，随重置清除）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                label = "导出备份",
                accent = NeonGreen,
                leadingIcon = Icons.Outlined.CloudUpload,
                onClick = { runExport() },
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                label = "导入备份",
                accent = NeonYellow,
                leadingIcon = Icons.Outlined.CloudDownload,
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            )
        }

        status?.let { (ok, msg) ->
            NeonCard(accent = if (ok) NeonGreen else NeonMagenta, filled = !ok) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = if (ok) TextMid else NeonMagenta)
            }
        }

        Spacer(Modifier.size(4.dp))
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("确认导入？", color = NeonYellow, style = MaterialTheme.typography.titleLarge) },
            text = { Text("导入将覆盖当前本地进度，此操作不可撤销。", color = TextMid) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    pendingUri?.let { runImport(it) }
                }) { Text("导入", color = NeonGreen) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("取消", color = NeonCyan) }
            },
            containerColor = SurfaceDark
        )
    }
}

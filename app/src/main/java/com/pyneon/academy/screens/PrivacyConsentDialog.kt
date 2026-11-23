package com.pyneon.academy.screens

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.theme.DangerRed
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

// 首次启动弹隐私同意。处理数据前要拿到明示同意，
// 不能点外面糊弄过去，拒绝就退出。
@Composable
fun PrivacyConsentDialog(onConsent: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* 必须明确选一个，点外面不算。 */ },
        containerColor = SurfaceDark,
        title = { Text("隐私政策同意", color = NeonCyan, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "欢迎使用 ${AppConstants.APP_NAME}（${AppConstants.APP_NAME_CN}）。开始前请阅读我们的《隐私政策》与《用户协议》。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "1. 不收集、不上传任何个人信息。\n" +
                        "2. 学习数据只存在本设备上。\n" +
                        "3. 联网权限只用于手动下载课程拓展包。\n" +
                        "4. 同意后才能使用全部功能。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "点击下方「同意并继续」即表示你已阅读并同意上述条款。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { ShareHelper.openUrl(context, AppConstants.PRIVACY_POLICY_URL) }
                ) { Text("查看完整隐私政策", color = NeonCyan) }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { ShareHelper.openUrl(context, AppConstants.TERMS_URL) }
                ) { Text("查看用户协议", color = NeonCyan) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConsent) { Text("同意并继续", color = NeonCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("不同意并退出", color = DangerRed) }
        }
    )
}

fun finishApp(context: android.content.Context) {
    (context as? Activity)?.finish()
}

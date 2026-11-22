package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit = {}
) {
    val context = LocalContext.current
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
            title = "关于 // ABOUT",
            accent = NeonMagenta,
            onBack = onBack
        )
        Text(
            "CodeLearning · 码上\n版本 v${AppConstants.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid
        )

        SectionHeader("开源与授权", accent = NeonCyan)
        Text(
            "本应用以 MIT 协议开源：允许自由使用、修改与再分发（含个人、教育及商业用途），只需保留版权与许可声明。" +
                "按「原样」提供，不附带任何担保。\"CodeLearning\"、\"码上\" 名称与标识为保留商标。",
            style = MaterialTheme.typography.bodySmall,
            color = TextMid
        )

        SectionHeader("技术致谢", accent = NeonGreen)
        Text(
            "· CPython 3.13 — 设备端嵌入的 Python 运行时\n" +
                "· Mozilla Rhino 1.7.15 — 设备端嵌入的 JavaScript 运行时\n" +
                "· Chaquopy — Android 上的 Python 集成\n" +
                "· Jetpack Compose / AndroidX — UI 与架构\n" +
                "· 课程设计与 CodeLearning 视觉系统由开发团队原创",
            style = MaterialTheme.typography.bodySmall,
            color = TextMid
        )

        NeonButton(
            label = "开源许可与第三方组件",
            accent = NeonCyan,
            onClick = onOpenLicenses,
            modifier = Modifier.fillMaxWidth()
        )

        SectionHeader("开源社区", accent = NeonGreen)
        Text(
            "如果这个项目帮到了你，欢迎在仓库点一个 Star。Star 是独立开发最直接的鼓励，也能让更多人看到这个离线学习项目。",
            style = MaterialTheme.typography.bodySmall,
            color = TextMid
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                label = "GitCode · 点 Star",
                accent = NeonGreen,
                onClick = { ShareHelper.openUrl(context, AppConstants.REPO_URL) },
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                label = "GitHub 镜像",
                accent = NeonCyan,
                onClick = { ShareHelper.openUrl(context, AppConstants.REPO_URL_GITHUB) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                label = "Gitee 镜像",
                accent = NeonCyan,
                onClick = { ShareHelper.openUrl(context, AppConstants.REPO_URL_GITEE) },
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                label = "完整用户手册",
                accent = NeonYellow,
                onClick = { ShareHelper.openUrl(context, AppConstants.USER_GUIDE_URL) },
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader("链接", accent = NeonMagenta)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                label = "隐私政策",
                accent = NeonCyan,
                onClick = { ShareHelper.openUrl(context, AppConstants.PRIVACY_POLICY_URL) },
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                label = "用户协议",
                accent = NeonCyan,
                onClick = { ShareHelper.openUrl(context, AppConstants.TERMS_URL) },
                modifier = Modifier.weight(1f)
            )
        }
        NeonButton(
            label = "联系我们",
            accent = NeonCyan,
            onClick = { ShareHelper.sendEmail(context, AppConstants.CONTACT_EMAIL, "CodeLearning 联系") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "© 2026 ${AppConstants.DEV_NAME}. 保留一切权利。",
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        )
        NeonButton(label = "返回", accent = NeonMagenta, onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

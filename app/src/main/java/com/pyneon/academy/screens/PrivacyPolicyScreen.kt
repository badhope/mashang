package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "隐私政策",
            accent = NeonCyan,
            useGlitch = false,
            onBack = onBack
        )
        Text(
            "开发者/运营者：${AppConstants.DEV_NAME}\n隐私联系：${AppConstants.CONTACT_EMAIL}",
            style = MaterialTheme.typography.bodySmall,
            color = TextMid
        )

        PolicyBlock("我们收集什么", "我们不收集任何个人信息，不要求注册账号，也不集成任何第三方统计、广告或追踪 SDK。")
        PolicyBlock("本地存储的数据", "你的学习进度、错题、偏好仅保存在你自己的设备本地（Android DataStore/SharedPreferences），不会上传到任何服务器。你可随时在「我的」页「重置全部进度」清除，或直接卸载应用。")
        PolicyBlock("网络权限说明", "应用仅声明了联网（INTERNET）权限，且只用于你在「语言中心」手动点击下载课程拓展包，该过程不附带传输任何个人信息。无网络时全部内置课程仍可完整运行。")
        PolicyBlock("嵌入的语言运行时", "内置六门语言的运行时：Chaquopy 内嵌 CPython 3.13（Python）、Mozilla Rhino 1.7.15（JavaScript）、类型擦除后复用 Rhino（TypeScript）、自研教学子集解释器（C）、LuaJ 3.0.1（Lua）、SQLite 内存库（SQL）。所有代码均在你的手机本地执行，不会发送到外部服务器。")
        PolicyBlock("诊断日志", "应用会在本地记录启动、运行时初始化、代码执行失败等事件的类型与错误摘要（不含你的代码内容、不含设备标识），用于你主动反馈问题时定位。日志仅存本机（上限约 25 万字符），不会自动上传；你可在「设置」里的开发者诊断查看、导出或清空。")
        PolicyBlock("第三方组件", "未集成任何广告、统计、追踪或推送 SDK。第三方组件（Chaquopy、Rhino、LuaJ、SQLite、sora-editor、AndroidX、ZXing）均为本地功能依赖，不采集数据。完整许可见「关于」里的开源许可。")
        PolicyBlock("未成年人", "本应用面向全年龄段，内容为编程学习，不含面向未成年人的定向收集或商业推送。")
        PolicyBlock("你的权利", "你有权了解、更正、删除设备上的本地数据。因我们不持有任何服务器端个人信息，无需额外的数据导出或注销账户流程。")

        NeonButton(
            label = "查看完整隐私政策",
            accent = NeonCyan,
            onClick = { ShareHelper.openUrl(context, AppConstants.PRIVACY_POLICY_URL) },
            modifier = Modifier.fillMaxWidth()
        )
        NeonButton(
            label = "隐私咨询",
            accent = NeonCyan,
            onClick = { ShareHelper.sendEmail(context, AppConstants.CONTACT_EMAIL, "CodeLearning 隐私咨询") },
            modifier = Modifier.fillMaxWidth()
        )
        NeonButton(label = "返回", accent = NeonCyan, onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PolicyBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = NeonCyan)
        Text(body, style = MaterialTheme.typography.bodySmall, color = TextMid)
    }
}

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper
import org.json.JSONArray

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // 毕业只算 Python 主线。下载的 DSA、彩蛋不算，不然这里和「我的」页对不上。
    val totalLessons = LessonRepository.mainline(context).size
    val totalChallenges = remember {
        runCatching {
            context.assets.open("challenges.json").bufferedReader(Charsets.UTF_8).use { reader ->
                JSONArray(reader.readText()).length()
            }
        }.getOrDefault(0)
    }

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
            title = "帮助中心",
            accent = NeonYellow,
            useGlitch = false,
            onBack = onBack
        )

        HelpSection("快速上手")
        FaqItem("从哪开始？", "打开「课程」，从第一幕第一课往下学。上一课过了才开下一课，通关后点「下一课」就行。")
        FaqItem("怎么练习？", "每课最后有实战演练。在编辑器里写代码，点「运行判题」自动批改。答对拿 XP，答错会进错题本。")
        FaqItem("想随手试代码？", "底部「终端」就是 REPL，输入一行跑一行，适合试小片段。")
        FaqItem("学完有什么？", "通关全部 $totalLessons 讲后，「我的」页可以领毕业证书。「挑战」页还有 $totalChallenges 道题，做完加 XP。")

        HelpSection("常用功能")
        FaqItem("变量快照是什么？", "每次运行后能看到当前变量的名字、类型和值。比只看输出更直观，排查问题时很有用。")
        FaqItem("错题本和复习台怎么用？", "判题错了会自动进错题本，在「我的」里能找到。复习台会按间隔重复排复习卡，隔几天再做一次，记得更牢。")
        FaqItem("主题能换吗？", "「我的」里有主题切换，7 套预置配色，也可以自己填主色。改完立即生效，下次打开还在。")
        FaqItem("语言中心是干什么的？", "「我的」里的语言中心会列出六个内置运行时：Python、JavaScript、TypeScript、C、Lua、SQL，课程包可以按需下载。Java、C++、Go、Rust 还需要编译器，当前版本跑不了，也不会给下载入口。已经装好的课程断网也能学。")
        FaqItem("进度安全吗？", "进度都存在本机。「我的」里的备份恢复可以导出 JSON，换机后再导入。")

        HelpSection("常见问题")
        FaqItem("完全离线吗？", "是。语言运行时直接打在 APK 里，$totalLessons 讲内置课程没网也能学。只有去语言中心下载额外课程包时才需要联网。")
        FaqItem("报错看不懂怎么办？", "判题结果里点「问教练」，它会解释错误类型、指出大概哪一行有问题，再给排查步骤，不直接给答案。")
        FaqItem("重置后还能找回吗？", "之前导出过备份就能导入找回。没备份的话重置后找不回来，所以重置前会再确认一次。")
        FaqItem("课程为什么锁着？", "上一课过了才开下一课，避免跳着学后面看不懂。")
        FaqItem("终端怎么退出？", "终端不用专门退出，按返回键回主界面就行。")
        FaqItem("判题一直不动？", "代码里如果有死循环，超时保护会中断并提示。先检查循环条件，再运行。")
        FaqItem("怎么更新？", "「我的」里的系统设置里有检查更新，会读取发布页的公开版本号。有新版本时点「打开发布页」下载覆盖安装。应用不会自动下载，也不申请安装权限。升级前建议先备份。")
        FaqItem("怎么联系开发者？", "点下面的「联系我们」发邮件，或者去开源仓库提 issue，入口在「我的」里的系统设置。")

        NeonButton(
            label = "完整用户手册",
            accent = NeonCyan,
            onClick = { ShareHelper.openUrl(context, AppConstants.USER_GUIDE_URL) },
            modifier = Modifier.fillMaxWidth()
        )
        NeonButton(
            label = "联系我们",
            accent = NeonGreen,
            onClick = { ShareHelper.sendEmail(context, AppConstants.CONTACT_EMAIL, "CodeLearning 使用咨询") },
            modifier = Modifier.fillMaxWidth()
        )
        NeonButton(
            label = "返回",
            accent = NeonYellow,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = NeonCyan,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Q. $question", style = MaterialTheme.typography.titleSmall, color = NeonYellow)
        Text(answer, style = MaterialTheme.typography.bodySmall, color = TextMid)
    }
}

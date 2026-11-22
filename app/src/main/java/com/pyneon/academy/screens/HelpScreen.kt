package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.ScreenHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.TextMid
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.utils.AppConstants
import com.pyneon.academy.utils.ShareHelper

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // 毕业口径 = 内置 Python 主线 39 讲（与「我的」页、证书页、毕业徽章保持同一事实源）。
    // 不能用 lessonsByLang("python")——下载 DSA/彩蛋后它会变成 49，导致帮助页说
    // 「通关 49 讲才毕业」而「我的」页按 38 计（2026-11 实测口径冲突）。
    val totalLessons = LessonRepository.mainline(context).size
    val totalChallenges = remember {
        runCatching {
            context.assets.open("challenges.json").bufferedReader(Charsets.UTF_8).use { r ->
                org.json.JSONArray(r.readText()).length()
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

        // ===== 快速上手 =====
        HelpSection("快速上手（60 秒）")
        FaqItem("1 · 从哪开始？", "进入「课程」页，从第一幕第一课开始。课程按章节解锁，完成一课才能解锁下一课；通关后按「下一课」直达。")
        FaqItem("2 · 怎么练习？", "每课末尾有「实战演练」：在代码编辑器写代码，点「运行判题」自动批改。答对得 XP 并解锁下一课；答错进错题本。")
        FaqItem("3 · 代码在哪随手试？", "底部「终端」页是 REPL 终端，像语言的交互式命令行一样随时输入、立即运行，适合验证小片段。")
        FaqItem("4 · 学完能拿什么？", "通关全部 $totalLessons 讲课程后，「我的」页解锁毕业证书；「挑战」页有 $totalChallenges 个实战闯关拿额外 XP。")

        // ===== 常用功能 =====
        HelpSection("常用功能")
        FaqItem("变量快照是什么？", "每次运行后，应用展示当前内存中的变量名、类型与值——直观看到代码执行过程，是学「代码到底在干什么」的独家利器。")
        FaqItem("错题本 / 复习台怎么用？", "判题失败的练习自动收入错题本（我的 → 错题本）；完成课程后系统按间隔重复生成复习卡（我的 → 复习台），对抗遗忘。")
        FaqItem("主题能换吗？", "我的 → 主题切换：7 套预置色调 + 自定义主色（#RRGGBB 或色板点选），即时生效并自动保存。")
        FaqItem("语言中心是什么？", "「我的 → 语言中心」列出六个内置离线运行时（Python / JavaScript / TypeScript / C / Lua / SQL）与可下载课程包。Java / C++ / Go / Rust 需要编译器，暂不支持，也不提供下载入口。断网不影响已安装课程。")
        FaqItem("进度安全吗？", "进度存本机。我的 → 备份/恢复 可导出为 JSON 文件（含 XP、通关、错题、复习卡），换机后可导入。")

        // ===== 常见问题 =====
        HelpSection("常见问题")
        FaqItem("完全离线吗？", "是。语言运行时就绪内嵌在应用里，${totalLessons} 讲内置课程无网络也能学。联网仅用于语言中心下载额外课程包。")
        FaqItem("代码报错看不懂怎么办？", "判题结果里点「问教练 · 只提示不代写」，智能教练会解释错误类型、定位疑似行号并给排查步骤，不直接给答案。")
        FaqItem("不小心重置了进度能找回吗？", "先到备份/恢复导出过备份就能导入找回；未备份的重置不可撤销，重置前有二次确认。")
        FaqItem("课程为什么是锁着的？", "课程按章节渐进解锁：必须先通过上一课，才能进入下一课——保证基础扎实再进阶。")
        FaqItem("终端里怎么退出？", "终端是实时交互环境，无需退出；清空输入或按返回回到其他页面即可。")
        FaqItem("判题卡住不动？", "代码若包含死循环，判题有超时保护会自动中断并提示。尽量先想清楚逻辑再运行。")
        FaqItem("更新怎么获取？", "到「我的 → 系统设置 → 检查更新」可读取 GitHub Releases 的公开版本号；有新版时点「打开发布页」下载覆盖安装。应用不自动下载、不申请安装权限，学习数据不丢失（建议先备份）。")
        FaqItem("如何联系开发者？", "点下方「联系我们」发邮件，或到开源仓库提 issue（见「我的 → 系统设置 → 开源仓库」）。")

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
private fun FaqItem(q: String, a: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Q. $q", style = MaterialTheme.typography.titleSmall, color = NeonYellow)
        Text(a, style = MaterialTheme.typography.bodySmall, color = TextMid)
    }
}

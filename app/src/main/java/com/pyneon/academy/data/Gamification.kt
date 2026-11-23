package com.pyneon.academy.data

import androidx.compose.ui.graphics.Color
import com.pyneon.academy.ui.theme.DarkTokens

data class Rank(val index: Int, val name: String, val minXp: Int, val color: Color)

object Ranks {
    // 段位色固定取品牌深色常量：两种主题下都保持辨识度（Ranks 在非组合上下文初始化）
    val all = listOf(
        Rank(0, "脚本小子", 0, DarkTokens.textMid),
        Rank(1, "数据幽灵", 150, DarkTokens.primary),
        Rank(2, "网络浪人", 400, DarkTokens.success),
        Rank(3, "义体黑客", 900, DarkTokens.gold),
        Rank(4, "街头传奇", 1600, DarkTokens.secondary),
        Rank(5, "系统架构师", 2600, DarkTokens.purple)
    )

    fun forXp(xp: Int): Rank = all.last { xp >= it.minXp }

    fun next(xp: Int): Rank? = all.firstOrNull { it.minXp > xp }

    fun fraction(xp: Int): Float {
        val current = forXp(xp)
        val next = next(xp) ?: return 1f
        val span = next.minXp - current.minXp
        return ((xp - current.minXp).toFloat() / span).coerceIn(0f, 1f)
    }
}

enum class Achievement(val title: String, val desc: String) {
    CODE_RUNNER("执行者", "第一次成功运行代码"),
    FIRST_BLOOD("初次连接", "完成第 1 节课程"),
    NIGHT_OWL("暗夜行者", "凌晨 0-5 点运行过代码"),
    STREAK_3("三连脉冲", "连续 3 天保持学习连击"),
    EXERCISE_5("调试专家", "累计通过 5 个练习或挑战"),
    GRADUATE("毕业典礼", "完成全部课程"),
    TERMINAL_HEAD("神经直连", "在终端中完成一次多行定义"),
    ARENA_WINNER("角斗士", "通过任意一个挑战")
}

// 算已解锁的成就。totalLessons 只传 Python 主线的 39 讲，
// 传全部课程数的话，学 30 讲 Python 加 8 讲 JS 就提前毕业了。
fun unlockedAchievements(progress: Progress, totalLessons: Int): Set<Achievement> {
    val unlocked = mutableSetOf<Achievement>()
    if (progress.firstRunDone) unlocked += Achievement.CODE_RUNNER
    if ("l01" in progress.completedLessons) unlocked += Achievement.FIRST_BLOOD
    if (progress.nightOwl) unlocked += Achievement.NIGHT_OWL
    if (progress.streakDays >= 3) unlocked += Achievement.STREAK_3
    if (progress.solvedKeys.size >= 5) unlocked += Achievement.EXERCISE_5
    // 毕业徽章：只统计「主线内」的完成数，避免学了语言包就提前毕业。
    // 调用方应传 mainline 的 id 集合大小；这里用 completedLessons 的交集再判一次，
    // 保证即使调用方传错也不会出现「学 8 讲 JS 就毕业」。
    if (totalLessons > 0) {
        val doneInMainline = progress.completedLessons.count { it.matches(Regex("""^l\d{2}$""")) }
        if (doneInMainline >= totalLessons) unlocked += Achievement.GRADUATE
    }
    if (progress.terminalHead) unlocked += Achievement.TERMINAL_HEAD
    if (progress.solvedKeys.any { it.startsWith("chal_") }) unlocked += Achievement.ARENA_WINNER
    return unlocked
}

// 每日任务完成没有。日期也得对，只看 dailyCount 会让昨天的任务今天还打勾。
// todayEpochDay 默认取今天，测试可以传固定日期。
fun dailyMissionDone(progress: Progress, todayEpochDay: Long = Clock.todayEpochDay()): Boolean =
    progress.dailyCount > 0 && progress.dailyDate == todayEpochDay

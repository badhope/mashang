package com.pyneon.academy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 加速压力测试：把半个月里约 120 次练习的写入压缩到一次单测里。
 *
 * 不模拟真实点击，只验证最容易出问题的那条路径：同一份 DataStore 连续写入、
 * 读取、再恢复，key 和 XP 不能少。真正的手感还要靠真机长跑，这里先把数据层兜住。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStressTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        ProgressStore.reset(context)
    }

    @Test
    fun `one hundred twenty exercise writes are not lost`() = runBlocking {
        repeat(120) { index ->
            ProgressStore.markExerciseSolved(
                context = context,
                key = "stress_$index",
                xp = 5,
                todayEpochDay = 100L + index / 10
            )
        }

        val progress = ProgressStore.snapshot(context)
        assertEquals(120, progress.solvedKeys.count { it.startsWith("stress_") })
        // 120 次练习 600 XP，加 12 天每日首练奖励 360 XP。
        assertEquals(960, progress.xpTotal)
        assertEquals(50, progress.recentActivities.size)
        assertEquals(10, progress.dailyCount)
    }

    @Test
    fun `backup restore keeps all solved keys`() = runBlocking {
        val keys = (0 until 120).map { "restore_$it" }.toSet()
        ProgressStore.restoreFromBackup(
            context = context,
            xpTotal = 2400,
            completedLessons = setOf("l01", "l02"),
            solvedKeys = keys,
            streakDays = 15,
            lastActiveEpochDay = 114L,
            dailyDate = 114L,
            dailyCount = 8,
            firstRunDone = true,
            nightOwl = true,
            terminalHead = false,
            badges = setOf("first_step")
        )

        val progress = ProgressStore.snapshot(context)
        assertEquals(keys, progress.solvedKeys)
        assertEquals(2400, progress.xpTotal)
        assertEquals(15, progress.streakDays)
        assertTrue(progress.firstRunDone)
        assertTrue(progress.nightOwl)
    }
}

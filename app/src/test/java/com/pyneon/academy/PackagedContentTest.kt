package com.pyneon.academy

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pyneon.academy.data.LessonRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 打包内容完整性：APK 内置的课程必须能被 LessonRepository 完整解析。
 *
 * 这是「APK 装到手机后到底能不能上第一课」的最近似验证——Robolectric 读的是
 * 真实的 app/src/main/assets。任何字段缺失导致某讲被静默丢弃，这里会立刻暴露。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PackagedContentTest {

    @Test
    fun mainlineHas39Lessons() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mainline = LessonRepository.mainline(ctx)
        assertEquals("内置 Python 主线应为 39 讲", 39, mainline.size)
        assertEquals("l01", mainline.first().id)
        assertEquals("l39", mainline.last().id)
    }

    @Test
    fun everyMainlineLessonIsPlayable() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (lesson in LessonRepository.mainline(ctx)) {
            assertTrue("${lesson.id} 标题为空", lesson.title.isNotBlank())
            assertTrue("${lesson.id} 无内容块", lesson.blocks.isNotEmpty())
            val ex = lesson.exercise
            assertTrue("${lesson.id} 无练习", ex != null)
            assertTrue("${lesson.id} 无判题用例", ex!!.tests.isNotEmpty())
            assertTrue("${lesson.id} 起始代码为空", ex.starterCode.isNotBlank())
        }
    }

    @Test
    fun lessonIdsAreUniqueAcrossInstalledPacks() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ids = LessonRepository.lessons(ctx).map { it.id }
        assertEquals("课程 id 必须全局唯一", ids.size, ids.distinct().size)
    }

    @Test
    fun challengesAreParsable() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val challenges = LessonRepository.challenges(ctx)
        assertTrue("至少有 1 道挑战", challenges.isNotEmpty())
        for (c in challenges) {
            assertTrue("${c.id} 缺判题", c.tests.isNotEmpty())
            assertTrue("${c.id} 缺起始代码", c.starterCode.isNotEmpty())
        }
    }
}

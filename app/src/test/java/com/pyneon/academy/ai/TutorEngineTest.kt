package com.pyneon.academy.ai

import com.pyneon.academy.data.Block
import com.pyneon.academy.data.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorEngineTest {

    private fun request(
        question: String,
        code: String = "def solve(x):\n    return x",
        sources: List<TutorSource> = emptyList()
    ) = TutorRequest(
        question = question,
        lessonId = "l01",
        lessonTitle = "变量与输出",
        lessonBrief = "把输入加一后返回",
        starterCode = "def solve(x):\n    # 在这里写代码\n    pass",
        currentCode = code,
        tests = listOf("assert solve(1) == 2"),
        nextLessonTitle = "条件判断",
        sources = sources
    )

    @Test
    fun `blank question starts with thinking plan`() {
        val reply = TutorEngine.reply(request(""))
        assertEquals(TutorIntent.THINKING, reply.intent)
        assertTrue(reply.steps.size >= 4)
        assertEquals("先拆题，再写代码", reply.title)
    }

    @Test
    fun `thinking plan does not hand over the answer`() {
        val reply = TutorEngine.reply(request("这题怎么开始"))
        assertEquals(TutorIntent.THINKING, reply.intent)
        assertFalse("引导示例不能直接给出答案", reply.example?.contains("return") == true)
    }

    @Test
    fun `error question routes to diagnosis`() {
        val req = request("帮我读报错").copy(
            errorType = "NameError",
            errorMessage = "name 'x' is not defined",
            traceback = "Traceback\n  File \"<pyneon>\", line 2, in <module>"
        )
        val reply = TutorEngine.reply(req)
        assertEquals(TutorIntent.ERROR, reply.intent)
        assertTrue(reply.title.contains("报错诊断"))
        assertTrue(reply.steps.isNotEmpty())
    }

    @Test
    fun `concept question returns lesson source`() {
        val source = TutorSource(
            lessonId = "l20",
            title = "第20讲 闭包",
            text = "闭包是函数和它捕获的外部变量一起形成的整体。"
        )
        val reply = TutorEngine.reply(request("什么是闭包", sources = listOf(source)))
        assertEquals(TutorIntent.CONCEPT, reply.intent)
        assertNotNull(reply.sourceText)
        assertTrue(reply.sourceText!!.contains("闭包"))
    }

    @Test
    fun `next step uses next lesson title`() {
        val reply = TutorEngine.reply(request("下一步学什么"))
        assertEquals(TutorIntent.NEXT_STEP, reply.intent)
        assertTrue(reply.title.contains("条件判断"))
    }

    @Test
    fun `unmatched concept falls back without inventing`() {
        val reply = TutorEngine.reply(request("什么是量子香蕉"))
        assertEquals(TutorIntent.CONCEPT, reply.intent)
        assertTrue(reply.lead, reply.lead.contains("没找到"))
        assertNull(reply.sourceTitle)
        assertNull(reply.sourceText)
        assertNull("没找到时不能编造示例", reply.example)
    }

    @Test
    fun `knowledge builder keeps readable lesson text`() {
        val lesson = Lesson(
            id = "l01",
            order = 1,
            chapter = 1,
            title = "变量",
            subtitle = "把值存起来",
            xp = 10,
            blocks = listOf(
                Block.Paragraph("变量是给值起名字。"),
                Block.Quiz("下面哪个是合法变量名？", listOf("1a", "name"), 1, "不能以数字开头")
            ),
            exercise = null
        )
        val source = TutorKnowledge.fromLessons(listOf(lesson)).single()
        assertTrue(source.text.contains("变量是给值起名字"))
        assertTrue(source.text.contains("不能以数字开头"))
    }
}

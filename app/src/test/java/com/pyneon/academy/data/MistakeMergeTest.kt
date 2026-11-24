package com.pyneon.academy.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MistakeMergeTest {

    private fun mistake(id: Int, block: Int, code: String = "print(1)") = MistakeRecord(
        id = id,
        lessonId = "l01",
        blockType = "exercise",
        blockIndex = block,
        userCode = code,
        expectedOutput = "2",
        actualOutput = "1",
        errorMessage = "断言失败"
    )

    @Test
    fun `same exercise keeps one record and refreshes the code`() {
        val merged = mergeMistake(listOf(mistake(7, -1, "print(0)")), mistake(99, -1, "print(1)"))
        assertEquals(1, merged.size)
        assertEquals(7, merged.single().id)
        assertEquals("print(1)", merged.single().userCode)
    }

    @Test
    fun `different exercises stay separate`() {
        val merged = mergeMistake(listOf(mistake(1, 0)), mistake(2, 1))
        assertEquals(2, merged.size)
    }

    @Test
    fun `mistake book is capped at three hundred`() {
        val existing = (1..305).map { mistake(it, it) }
        val merged = mergeMistake(existing, mistake(306, 306))
        assertEquals(300, merged.size)
        assertEquals(7, merged.first().id)
        assertEquals(306, merged.last().id)
    }
}

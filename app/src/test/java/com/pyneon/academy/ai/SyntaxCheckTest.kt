package com.pyneon.academy.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxCheckTest {

    @Test
    fun `balanced code has no hints`() {
        val hints = SyntaxCheck.hints("def add(a, b):\n    return a + b\n", "python")
        assertTrue(hints.isEmpty())
    }

    @Test
    fun `missing closing bracket is reported`() {
        val hints = SyntaxCheck.hints("print(add(1, 2)\n", "python")
        assertTrue(hints.any { it.contains("没有闭合") })
    }

    @Test
    fun `unclosed quote is reported`() {
        val hints = SyntaxCheck.hints("name = 'neo\n", "python")
        assertTrue(hints.any { it.contains("引号") })
    }

    @Test
    fun `python if without colon is reported`() {
        val hints = SyntaxCheck.hints("if score > 0\n    print(score)\n", "python")
        assertTrue(hints.any { it.contains("冒号") })
    }

    @Test
    fun `chinese punctuation outside string is reported`() {
        val hints = SyntaxCheck.hints("name = 'neo'，\nprint(name)\n", "python")
        assertTrue(hints.any { it.contains("中文标点") })
    }

    @Test
    fun `chinese punctuation inside comment and string is ignored`() {
        val code = """
            # 这里用中文逗号，注释里没问题。
            text = "你好，世界"
            print(text)
        """.trimIndent()
        assertTrue(SyntaxCheck.hints(code, "python").isEmpty())
    }

    @Test
    fun `triple quoted docstring does not trigger quote warning`() {
        val code = "def f():\n    \"\"\"这里是说明，跨了两行。\n    第二行。\n    \"\"\"\n    return 1"
        assertFalse(SyntaxCheck.hints(code, "python").any { it.contains("引号") })
    }

    @Test
    fun `lua length operator is not treated as a comment`() {
        assertTrue(SyntaxCheck.hints("local n = #items\n", "lua").isEmpty())
    }

    @Test
    fun `sql doubled single quote is an escape not a new string`() {
        val code = "SELECT 'it''s fine' AS note FROM demo;\n"
        assertTrue(SyntaxCheck.hints(code, "sql").isEmpty())
    }

    @Test
    fun `sql doubled double quote in identifier is ignored`() {
        val code = "SELECT \"say\"\"hi\" FROM demo;\n"
        assertTrue(SyntaxCheck.hints(code, "sql").isEmpty())
    }
}

package com.pyneon.academy.ai

/**
 * 编辑器下方的轻量语法检查。只看括号、引号、中文标点和 Python 冒号，
 * 不做完整解析——真正的语法错误还是要跑一次才知道。
 */
object SyntaxCheck {

    private val PYTHON_BLOCK_WORDS = listOf(
        "if ", "elif ", "else", "for ", "while ", "def ", "class ",
        "try", "except", "finally", "with ", "match ", "case "
    )

    fun hints(code: String, language: String): List<String> {
        if (code.isBlank()) return emptyList()
        val scan = scan(code, language)
        val hints = scan.hints.toMutableList()

        val lines = scan.cleaned.lines()
        if (language == "python") {
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                val looksLikeBlock = PYTHON_BLOCK_WORDS.any { trimmed.startsWith(it) } ||
                    trimmed.endsWith("else") || trimmed.endsWith("try") || trimmed.endsWith("finally")
                if (looksLikeBlock && !trimmed.endsWith(":") && !trimmed.contains("#")) {
                    hints += "第 ${index + 1} 行像是代码块开头，末尾少了冒号"
                }
            }
        }

        val chinese = Regex("[，。；：、“”‘’！？]")
        chinese.find(scan.cleaned)?.let { match ->
            val line = scan.cleaned.take(match.range.first).count { it == '\n' } + 1
            hints += "第 $line 行有中文标点，代码里要用半角符号"
        }

        return hints.distinct().take(3)
    }

    private data class ScanResult(
        val cleaned: String,
        val hints: List<String>
    )

    private data class OpenBracket(val char: Char, val line: Int)

    private fun scan(code: String, language: String): ScanResult {
        val out = StringBuilder(code.length)
        val hints = mutableListOf<String>()
        val stack = ArrayDeque<OpenBracket>()
        var inSingle = false
        var inDouble = false
        var inBacktick = false
        var stringLine = 0
        var inLineComment = false
        var inBlockComment = false
        var line = 1
        var i = 0

        fun appendMasked(c: Char) {
            out.append(if (c == '\n') '\n' else ' ')
        }

        while (i < code.length) {
            val c = code[i]
            val next = code.getOrNull(i + 1)

            if (c == '\n') {
                if (inSingle || inDouble || inBacktick) {
                    hints += "第 $stringLine 行引号没闭合"
                    inSingle = false
                    inDouble = false
                    inBacktick = false
                }
                inLineComment = false
                out.append('\n')
                line += 1
                i += 1
                continue
            }

            if (inLineComment) {
                appendMasked(c)
                i += 1
                continue
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    appendMasked(c)
                    appendMasked(next)
                    inBlockComment = false
                    i += 2
                } else {
                    appendMasked(c)
                    i += 1
                }
                continue
            }

            if (inSingle || inDouble || inBacktick) {
                if (c == '\\' && i + 1 < code.length) {
                    appendMasked(c)
                    appendMasked(code[i + 1])
                    i += 2
                    continue
                }
                // SQL 用两个连续引号表示字符串里的一个引号，不能当成收尾再重开。
                if (language == "sql" && next != null &&
                    ((inSingle && c == '\'' && next == '\'') ||
                        (inDouble && c == '"' && next == '"'))
                ) {
                    appendMasked(c)
                    appendMasked(next)
                    i += 2
                    continue
                }
                val closes = (inSingle && c == '\'') || (inDouble && c == '"') || (inBacktick && c == '`')
                if (closes) {
                    inSingle = false
                    inDouble = false
                    inBacktick = false
                }
                appendMasked(c)
                i += 1
                continue
            }

            when {
                c == '#' && language == "python" -> {
                    inLineComment = true
                    appendMasked(c)
                }
                c == '-' && next == '-' && language in setOf("lua", "sql") -> {
                    inLineComment = true
                    appendMasked(c)
                    appendMasked(next)
                    i += 2
                    continue
                }
                c == '/' && next == '/' && language in setOf("c", "javascript", "typescript") -> {
                    inLineComment = true
                    appendMasked(c)
                    appendMasked(next)
                    i += 2
                    continue
                }
                c == '/' && next == '*' && language in setOf("c", "javascript", "typescript") -> {
                    inBlockComment = true
                    appendMasked(c)
                    appendMasked(next)
                    i += 2
                    continue
                }
                c == '"' && next == '"' && code.getOrNull(i + 2) == '"' -> {
                    val end = code.indexOf("\"\"\"", i + 3)
                    if (end < 0) {
                        hints += "第 $line 行的三引号没有闭合"
                        i = code.length
                    } else {
                        for (j in i until end + 3) {
                            if (code[j] == '\n') {
                                out.append('\n')
                                line += 1
                            } else {
                                out.append(' ')
                            }
                        }
                        i = end + 3
                        continue
                    }
                }
                c == '\'' && next == '\'' && code.getOrNull(i + 2) == '\'' -> {
                    val end = code.indexOf("'''", i + 3)
                    if (end < 0) {
                        hints += "第 $line 行的三引号没有闭合"
                        i = code.length
                    } else {
                        for (j in i until end + 3) {
                            if (code[j] == '\n') {
                                out.append('\n')
                                line += 1
                            } else {
                                out.append(' ')
                            }
                        }
                        i = end + 3
                        continue
                    }
                }
                c == '\'' -> {
                    inSingle = true
                    stringLine = line
                    appendMasked(c)
                }
                c == '"' -> {
                    inDouble = true
                    stringLine = line
                    appendMasked(c)
                }
                c == '`' && language in setOf("javascript", "typescript") -> {
                    inBacktick = true
                    stringLine = line
                    appendMasked(c)
                }
                c == '(' || c == '[' || c == '{' -> {
                    stack.addLast(OpenBracket(c, line))
                    out.append(c)
                }
                c == ')' || c == ']' || c == '}' -> {
                    val expected = when (c) {
                        ')' -> '('
                        ']' -> '['
                        else -> '{'
                    }
                    val last = stack.removeLastOrNull()
                    if (last == null) {
                        hints += "第 $line 行多了一个 ${c}，前面没有对应的开括号"
                    } else if (last.char != expected) {
                        hints += "第 $line 行的 ${c} 和第 ${last.line} 行的 ${last.char} 对不上"
                    }
                    out.append(c)
                }
                else -> out.append(c)
            }
            i += 1
        }

        if (inSingle || inDouble || inBacktick) {
            hints += "第 $stringLine 行引号没闭合"
        }
        if (inBlockComment) {
            hints += "块注释没有闭合"
        }
        if (stack.isNotEmpty()) {
            val last = stack.last()
            hints += "第 ${last.line} 行的 ${last.char} 没有闭合"
        }
        return ScanResult(out.toString(), hints.distinct())
    }
}

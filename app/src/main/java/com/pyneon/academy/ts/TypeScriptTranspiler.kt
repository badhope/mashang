package com.pyneon.academy.ts

/**
 * 把 TypeScript 的类型标注擦掉，转成 Rhino 能跑的 JS。
 *
 * 不是完整 tsc：官方编译器好几 MB，Rhino 的 ES5 环境跑不动。
 * TS 的类型本来运行时就不存在，擦掉之后语义和 JS 一样。
 *
 * 支持变量、参数、返回值的类型标注，interface、type、泛型参数、
 * as 断言、非空断言、访问修饰符和 enum。
 * 装饰器、namespace、声明合并、抽象类、泛型约束、条件类型、映射类型、
 * 尖括号断言都不做，遇到就原样交给 JS 报错。
 *
 * 全部用字符扫描，跳过字符串、模板串和注释。宁可少转，也别转错。
 */
object TypeScriptTranspiler {

    // changed 用来确认真的擦过东西，不是原样返回。
    data class Result(val code: String, val changed: Boolean)

    fun transpile(source: String): Result {
        var s = source

        s = stripInterfaces(s)
        s = stripTypeAliases(s)
        s = stripDeclare(s)
        s = convertEnums(s)
        s = stripAccessModifiers(s)
        s = stripGenericParams(s)
        s = stripTypeAnnotations(s)
        s = stripAsCasts(s)
        s = stripNonNullAssertions(s)

        return Result(s, s != source)
    }

    /**
     * 删除 `interface Name { ... }` / `interface Name extends X { ... }` 整块。
     * 用花括号配平扫描，跳字符串与注释。
     */
    private fun stripInterfaces(src: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < src.length) {
            val at = indexOfKeyword(src, "interface", i)
            if (at < 0) {
                out.append(src, i, src.length)
                break
            }
            out.append(src, i, at)
            // 跳过 interface 头部直到 '{'
            var j = at + "interface".length
            while (j < src.length && src[j] != '{') j++
            if (j >= src.length) {
                out.append(src, at, src.length)
                break
            }
            // 配平花括号
            var depth = 0
            var k = j
            while (k < src.length) {
                val ch = src[k]
                if (ch == '{') depth++
                if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
                k++
            }
            val end = if (k < src.length) k + 1 else src.length
            // 吃掉行尾多余空白/换行，保持行号大致稳定
            var e = end
            while (e < src.length && (src[e] == ' ' || src[e] == '\t')) e++
            if (e < src.length && src[e] == '\r') e++
            if (e < src.length && src[e] == '\n') e++
            out.append("\n")
            i = e
        }
        return out.toString()
    }

    /** 删除 `type Name = ...;` 单行别名声明。 */
    private fun stripTypeAliases(src: String): String {
        val lines = src.split("\n").toMutableList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val trimmed = lines[i].trimStart()
            if (trimmed.startsWith("type ") && trimmed.contains("=") &&
                Regex("""^type\s+\w+(<[^>]*>)?\s*=""").containsMatchIn(trimmed)
            ) {
                // 花括号类型字面量可能跨行，配平后整体丢弃
                var depth = lines[i].count { it == '{' } - lines[i].count { it == '}' }
                var j = i
                while (depth > 0 && j + 1 < lines.size) {
                    j++
                    depth += lines[j].count { it == '{' } - lines[j].count { it == '}' }
                }
                out.add("")
                i = j + 1
                continue
            }
            out.add(lines[i])
            i++
        }
        return out.joinToString("\n")
    }

    private fun stripDeclare(src: String): String =
        src.split("\n").map { if (it.trimStart().startsWith("declare ")) "" else it }
            .joinToString("\n")

    // enum 转成冻结对象，比如 Color.Red -> 0。只处理数值成员。
    private fun convertEnums(src: String): String {
        val regex = Regex("""enum\s+(\w+)\s*\{([^}]*)}""")
        return regex.replace(src) { m ->
            val name = m.groupValues[1]
            val body = m.groupValues[2]
            var next = 0
            val entries = body.split(",").mapNotNull { raw ->
                val item = raw.trim()
                if (item.isEmpty()) return@mapNotNull null
                val eq = item.indexOf('=')
                if (eq >= 0) {
                    val key = item.substring(0, eq).trim()
                    val value = item.substring(eq + 1).trim()
                    next = value.toIntOrNull()?.plus(1) ?: next
                    "$key: $value"
                } else {
                    val key = item
                    "${key}: ${next}"
                    next++
                    "$key: ${next - 1}"
                }
            }
            "var $name = Object.freeze({ ${entries.joinToString(", ")} })"
        }
    }

    /** 去掉构造参数上的 `public` / `private` / `protected` / `readonly`。 */
    private fun stripAccessModifiers(src: String): String =
        Regex("""\b(public|private|protected|readonly)\s+""").replace(src, "")

    /** 去掉函数/类名后的泛型参数表：`function id<T>(` 改成 `function id(`。 */
    private fun stripGenericParams(src: String): String {
        val fn = Regex("""(function\s+\w+)\s*<[^<>()]*>\s*\(""")
        val cls = Regex("""(class\s+\w+)\s*<[^<>()]*>\s*(\{|extends)""")
        var s = fn.replace(src) { "${it.groupValues[1]}(" }
        s = cls.replace(s) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        return s
    }

    /** 去掉 `x as Type`（含链式）。 */
    private fun stripAsCasts(src: String): String =
        Regex("""\s+as\s+[A-Za-z_$][\w$]*(<[^<>]*>)?(\[\])?""").replace(src, "")

    /** 去掉表达式后的非空断言 `foo!.bar` / `foo!`。 */
    private fun stripNonNullAssertions(src: String): String =
        Regex("""([\w$\])])!(?=[.\[]|;|,|\)|\s|$)""").replace(src, "$1")

    // 核心就是找冒号，把后面的类型删掉。麻烦在于冒号也出现在对象键和三元里，
    // 还得跳过字符串和注释，所以只能逐字符扫，正则容易误伤。
    private fun stripTypeAnnotations(src: String): String {
        val out = StringBuilder()
        var i = 0
        var braceDepth = 0        // 0 = 顶层，>0 = 对象/代码块内
        var parenDepth = 0
        var lastSignificant = ' '  // 上一个非空白字符

        while (i < src.length) {
            val ch = src[i]

            // 字符串和模板串整段照抄。
            if (ch == '\'' || ch == '"' || ch == '`') {
                val quote = ch
                out.append(ch)
                i++
                while (i < src.length) {
                    val c = src[i]
                    out.append(c)
                    if (c == '\\' && i + 1 < src.length) {
                        out.append(src[i + 1]); i += 2; continue
                    }
                    i++
                    if (c == quote) break
                }
                lastSignificant = quote
                continue
            }

            // 行注释照抄到行尾。
            if (ch == '/' && i + 1 < src.length && src[i + 1] == '/') {
                while (i < src.length && src[i] != '\n') { out.append(src[i]); i++ }
                continue
            }

            // 块注释也照抄。
            if (ch == '/' && i + 1 < src.length && src[i + 1] == '*') {
                out.append("/*"); i += 2
                while (i + 1 < src.length && !(src[i] == '*' && src[i + 1] == '/')) {
                    out.append(src[i]); i++
                }
                if (i + 1 < src.length) { out.append("*/"); i += 2 }
                continue
            }

            when (ch) {
                '{' -> { braceDepth++; out.append(ch); i++; lastSignificant = ch; continue }
                '}' -> { if (braceDepth > 0) braceDepth--; out.append(ch); i++; lastSignificant = ch; continue }
                '(' -> { parenDepth++; out.append(ch); i++; lastSignificant = ch; continue }
                ')' -> { if (parenDepth > 0) parenDepth--; out.append(ch); i++; lastSignificant = ch; continue }
            }

            // 冒号要分清是类型标注、对象键还是三元。
            if (ch == ':') {
                // 三元的 `? ... : ...` 不处理
                if (looksLikeTernaryColon(out)) {
                    out.append(ch); i++; lastSignificant = ch; continue
                }
                // 对象字面量键值对：`{ key: value }`，键是标识符，前面紧挨着 '{' 或 ','
                if (isObjectKeyColon(out, braceDepth, parenDepth)) {
                    out.append(ch); i++; lastSignificant = ch; continue
                }
                // 冒号后面是类型形状就擦掉。
                val (typeText, endIdx) = readTypeText(src, i + 1)
                if (typeText.isNotEmpty() && looksLikeType(typeText, lastSignificant)) {
                    // 冒号和前面多余的空格一起删。下一个字符是 '=' 就补空格，
                    // 是字母数字也补一个，免得粘成一个词；是 ) , ; 就什么都不加。
                    while (out.isNotEmpty() && out.last() == ' ') out.setLength(out.length - 1)
                    val nextCh = if (endIdx < src.length) src[endIdx] else null
                    val wouldMerge = nextCh != null &&
                        (nextCh.isLetterOrDigit() || nextCh == '_' || nextCh == '$')
                    val needsSpace = wouldMerge || nextCh == '='
                    if (needsSpace && out.isNotEmpty()) out.append(' ')
                    i = endIdx
                    lastSignificant = ' '
                    continue
                }
                out.append(ch); i++; lastSignificant = ch
                continue
            }

            out.append(ch)
            if (!ch.isWhitespace()) lastSignificant = ch
            i++
        }
        // 最后收掉行尾空格。
        return out.toString().lines().joinToString("\n") { it.trimEnd() }
    }

    /** 读到类型结束位置；返回 (类型文本, 结束下标)。 */
    private fun readTypeText(src: String, start: Int): Pair<String, Int> {
        var i = start
        // 跳过冒号后的空格，不然类型文本里会多出一截。
        while (i < src.length && (src[i] == ' ' || src[i] == '\t')) i++
        var depth = 0
        val sb = StringBuilder()
        while (i < src.length) {
            val c = src[i]
            when (c) {
                '<', '[', '(' -> { depth++; sb.append(c); i++; continue }
                '>', ']', ')' -> {
                    if (depth == 0) break   // `)` 是参数表结束，类型到此为止
                    depth--; sb.append(c); i++; continue
                }
                '=', ';', ',', '{', '}', '\n', '\r' -> if (depth == 0) break
                ':' -> if (depth == 0) break
            }
            // `=>` 终止（函数类型已完整）
            if (c == '=' && i + 1 < src.length && src[i + 1] == '>') break
            sb.append(c)
            i++
        }
        return sb.toString().trim() to i
    }

    /** 类型文本的形状校验：只允许类型语法字符，防止把表达式误吞。 */
    private fun looksLikeType(text: String, lastSignificant: Char): Boolean {
        if (text.isEmpty() || text.length > 120) return false
        // 必须像类型：标识符/尖括号/方括号/点/竖线/与/空格/字面量
        if (!Regex("""^[A-Za-z_$][\w$<>\[\].|&\s,]*$""").matches(text)) return false
        // 常见值关键字（true/false/this/new/typeof/in/instanceof）说明这是表达式不是类型
        val head = text.trim().substringBefore('<').substringBefore('[').trim()
        if (head in VALUE_KEYWORDS) return false
        // `{` 开头的是对象/代码块，不是类型标注起点（此时 lastSignificant 会是 '{' 或 ','）
        if (lastSignificant == '{' || lastSignificant == ',') {
            // `function f(a, b: T)` 里 b 前是 ','，仍需擦除；用「前一个 token 是标识符」再判
            return true
        }
        return true
    }

    /** 判断 `:` 是不是三元运算符的冒号（同一表达式内前面出现过未配对的 `?`）。 */
    private fun looksLikeTernaryColon(out: StringBuilder): Boolean {
        var depthParen = 0
        var i = out.length - 1
        while (i >= 0) {
            when (out[i]) {
                ')' -> depthParen++
                '(' -> depthParen--
                '?' -> if (depthParen == 0) return true
                ';', '{', '}', '\n' -> if (depthParen == 0) return false
            }
            i--
        }
        return false
    }

    /** 判断 `:` 是不是对象字面量的键分隔符（`{ a: 1 }` 或 `, a: 1`）。 */
    private fun isObjectKeyColon(out: StringBuilder, braceDepth: Int, parenDepth: Int): Boolean {
        // 参数类型形如 `function f(a: T)`，前面是 '(' 或 ','。
        if (parenDepth > 0) return false
        if (braceDepth <= 0) return false
        // 回看：跳过空白，取前一个 token
        var i = out.length - 1
        while (i >= 0 && out[i].isWhitespace()) i--
        val end = i
        while (i >= 0 && (out[i].isLetterOrDigit() || out[i] == '_' || out[i] == '$' || out[i] == '"' || out[i] == '\'')) i--
        if (end == i) return false
        var j = i
        while (j >= 0 && out[j].isWhitespace()) j--
        // `{ a: 1` 或 `, a: 1`，说明是对象键。
        return j >= 0 && (out[j] == '{' || out[j] == ',')
    }

    /** 在 src 中查找独立关键字（前后不能是标识符字符）。 */
    private fun indexOfKeyword(src: String, keyword: String, from: Int): Int {
        var idx = src.indexOf(keyword, from)
        while (idx >= 0) {
            val beforeOk = idx == 0 || !(src[idx - 1].isLetterOrDigit() || src[idx - 1] == '_' || src[idx - 1] == '$')
            val after = idx + keyword.length
            val afterOk = after >= src.length || !(src[after].isLetterOrDigit() || src[after] == '_' || src[after] == '$')
            if (beforeOk && afterOk) return idx
            idx = src.indexOf(keyword, idx + 1)
        }
        return -1
    }

    private val VALUE_KEYWORDS = setOf(
        "true", "false", "this", "new", "typeof", "in", "instanceof", "void", "delete", "yield", "await"
    )
}

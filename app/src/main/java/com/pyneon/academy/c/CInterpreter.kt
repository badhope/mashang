package com.pyneon.academy.c

/**
 * 给课程用的 C 子集解释器，纯 Kotlin，不依赖 NDK。
 *
 * Android 上装 clang 要两三百 MB，还链不上系统的 bionic，学习场景不划算，
 * 所以这里直接解释执行源码。gcc 是怎么算的就跟着怎么算，没实现的语法直接报错，
 * 不猜一个看起来对的结果。
 *
 * 类型只有 int、double、char、void；unsigned/short/long 当 int，const 收下但不管。
 * 数组最多二维，结构体支持 typedef 和一层嵌套。
 * 语句支持声明、赋值、分支、循环、switch、break/continue/return。
 * 运算符、单级指针、函数、递归、printf/puts/putchar 和常用字符串数学函数都在。
 * goto、宏、函数指针、二级指针、union/enum、scanf 不支持。
 *
 * sizeof 按教学模型算，不是宿主机的 ABI：int 4、double 8、char 1、指针 8，
 * 数组是元素大小乘长度，结构体成员大小求和后补到 8 的倍数。
 * 整数统一用 Long，char 存码点，浮点用 Double；运算时有 double 就按浮点走。
 */

object CInterpreter {

    // 值模型

    private sealed class Value {
        data class Num(val v: Long) : Value()
        data class Real(val v: Double) : Value()
        data class Str(val v: String) : Value()
        data class Arr(val cells: List<Cell>, val elem: CType, val dims: List<Int>) : Value()
        data class Struct(val def: StructDef, val fields: LinkedHashMap<String, Cell>) : Value()
        data class Ptr(val cells: List<Cell>, val index: Int) : Value()
        object Void : Value()

        fun asDouble(): Double = when (this) {
            is Num -> v.toDouble()
            is Real -> v
            is Str -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        fun asLong(): Long = when (this) {
            is Num -> v
            is Real -> v.toLong()
            is Str -> v.toLongOrNull() ?: 0L
            else -> 0L
        }

        fun truthy(): Boolean = when (this) {
            is Num -> v != 0L
            is Real -> v != 0.0
            is Str -> v.isNotEmpty()
            is Arr -> true
            is Struct -> true
            is Ptr -> cells.isNotEmpty()
            Void -> false
        }

        fun display(): String = when (this) {
            is Num -> v.toString()
            is Real -> formatDouble(v)
            is Str -> v
            is Arr -> if (elem === CType.TChar) {
                val sb = StringBuilder()
                for (c in cells) {
                    val n = c.value.asLong()
                    if (n == 0L) break
                    sb.append(n.toInt().toChar())
                }
                sb.toString()
            } else {
                cells.joinToString(" ") { it.value.display() }
            }
            is Struct -> fields.entries.joinToString(", ", "{", "}") {
                "${it.key}=${it.value.value.display()}"
            }
            is Ptr -> if (cells.isEmpty()) "NULL"
            else "0x" + System.identityHashCode(cells).toString(16)
            Void -> ""
        }
    }

    /** 内存单元：指针指向它，赋值写它。type 决定赋值时的转换与 sizeof。 */
    private class Cell(var value: Value, val type: CType, val readonly: Boolean = false)

    private class StructDef(
        val tag: String?,
        val fields: MutableList<Pair<String, CType>> = mutableListOf()
    )

    private sealed class CType {
        object TInt : CType()
        object TDouble : CType()
        object TChar : CType()
        object TVoid : CType()
        data class TPtr(val to: CType) : CType()
        data class TArray(val of: CType, val dims: List<Int>) : CType()
        data class TStruct(val def: StructDef) : CType()

        fun display(): String = when (this) {
            TInt -> "int"
            TDouble -> "double"
            TChar -> "char"
            TVoid -> "void"
            is TPtr -> to.display() + "*"
            is TArray -> of.display() + dims.joinToString("") { "[$it]" }
            is TStruct -> "struct " + (def.tag ?: "?")
        }
    }

    /** 与 C 的 %g 接近的默认浮点显示：整数值不带小数点，否则最多 6 位有效小数。 */
    private fun formatDouble(d: Double): String {
        if (d.isNaN()) return "nan"
        if (d.isInfinite()) return if (d > 0) "inf" else "-inf"
        if (d == d.toLong().toDouble() && kotlin.math.abs(d) < 1e15) return d.toLong().toString()
        val s = String.format(java.util.Locale.US, "%.6f", d).trimEnd('0').trimEnd('.')
        return s
    }

    // 词法

    private enum class T { NUM, REAL, STR, CHARLIT, IDENT, KEYWORD, OP, PUNCT, EOF }

    private data class Token(val type: T, val text: String, val line: Int)

    private class Lexer(private val src: String) {
        private var i = 0
        private var line = 1

        fun tokenize(): List<Token> {
            val out = mutableListOf<Token>()
            while (i < src.length) {
                val c = src[i]
                when {
                    c == '\n' -> { line++; i++ }
                    c.isWhitespace() -> i++
                    c == '/' && i + 1 < src.length && src[i + 1] == '/' -> {
                        while (i < src.length && src[i] != '\n') i++
                    }
                    c == '/' && i + 1 < src.length && src[i + 1] == '*' -> {
                        i += 2
                        while (i + 1 < src.length && !(src[i] == '*' && src[i + 1] == '/')) {
                            if (src[i] == '\n') line++
                            i++
                        }
                        i += 2
                    }
                    c == '"' -> out.add(readString())
                    c == '\'' -> out.add(readChar())
                    c.isDigit() || (c == '.' && i + 1 < src.length && src[i + 1].isDigit()) ->
                        out.add(readNumber())
                    c.isLetter() || c == '_' -> out.add(readIdent())
                    else -> out.add(readOp())
                }
            }
            out.add(Token(T.EOF, "", line))
            return out
        }

        private fun readString(): Token {
            val startLine = line
            i++ // skip "
            val sb = StringBuilder()
            while (i < src.length && src[i] != '"') {
                if (src[i] == '\\' && i + 1 < src.length) {
                    sb.append(unescape(src[i + 1])); i += 2
                } else {
                    if (src[i] == '\n') line++
                    sb.append(src[i]); i++
                }
            }
            i++ // skip closing "
            return Token(T.STR, sb.toString(), startLine)
        }

        private fun readChar(): Token {
            val startLine = line
            i++
            val ch: Char = if (src[i] == '\\' && i + 1 < src.length) {
                val c = unescape(src[i + 1]); i += 2; c
            } else {
                val c = src[i]; i++; c
            }
            if (i < src.length && src[i] == '\'') i++
            return Token(T.CHARLIT, ch.code.toString(), startLine)
        }

        private fun unescape(c: Char): Char = when (c) {
            'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; '0' -> '\u0000'
            '\\' -> '\\'; '"' -> '"'; '\'' -> '\''
            else -> c
        }

        private fun readNumber(): Token {
            val startLine = line
            // 十六进制：位运算课程会用 0xFF 这类写法
            if (src[i] == '0' && i + 1 < src.length && (src[i + 1] == 'x' || src[i + 1] == 'X')) {
                val start = i
                i += 2
                while (i < src.length && (src[i].isDigit() || src[i] in 'a'..'f' || src[i] in 'A'..'F')) i++
                val hex = src.substring(start + 2, i)
                val v = hex.toLongOrNull(16) ?: 0L
                while (i < src.length && (src[i] == 'l' || src[i] == 'L' || src[i] == 'u' || src[i] == 'U')) i++
                return Token(T.NUM, v.toString(), startLine)
            }
            // 八进制字面量：0 开头且全是 0-7（C 里 077 == 63）
            if (src[i] == '0' && i + 1 < src.length && src[i + 1] in '0'..'7') {
                val start0 = i
                i++
                while (i < src.length && src[i] in '0'..'7') i++
                if (i >= src.length || src[i] !in '8'..'9') {
                    val text = src.substring(start0 + 1, i)
                    while (i < src.length && (src[i] == 'l' || src[i] == 'L' || src[i] == 'u' || src[i] == 'U')) i++
                    return Token(T.NUM, text.toLongOrNull(8)?.toString() ?: "0", startLine)
                }
                i = start0
            }
            val start = i
            var isReal = false
            while (i < src.length && (src[i].isDigit() || src[i] == '.')) {
                if (src[i] == '.') isReal = true
                i++
            }
            // 科学计数法
            if (i < src.length && (src[i] == 'e' || src[i] == 'E')) {
                isReal = true; i++
                if (i < src.length && (src[i] == '+' || src[i] == '-')) i++
                while (i < src.length && src[i].isDigit()) i++
            }
            // 整型后缀
            while (i < src.length && (src[i] == 'l' || src[i] == 'L' || src[i] == 'u' || src[i] == 'U')) i++
            val text = src.substring(start, i).filter { !it.isLetter() }
            return Token(if (isReal) T.REAL else T.NUM, text.ifEmpty { "0" }, startLine)
        }

        private fun readIdent(): Token {
            val start = i
            while (i < src.length && (src[i].isLetterOrDigit() || src[i] == '_')) i++
            val text = src.substring(start, i)
            return Token(if (text in KEYWORDS) T.KEYWORD else T.IDENT, text, line)
        }

        private fun readOp(): Token {
            val three = if (i + 2 < src.length) src.substring(i, i + 3) else ""
            if (three in THREE_CHAR_OPS) { i += 3; return Token(T.OP, three, line) }
            val two = if (i + 1 < src.length) src.substring(i, i + 2) else ""
            if (two in TWO_CHAR_OPS) { i += 2; return Token(T.OP, two, line) }
            val c = src[i]; i++
            return if (c in PUNCT_CHARS) Token(T.PUNCT, c.toString(), line)
            else Token(T.OP, c.toString(), line)
        }
    }

    // 语法树

    private sealed class Expr {
        data class NumLit(val v: Long) : Expr()
        data class RealLit(val v: Double) : Expr()
        data class StrLit(val v: String) : Expr()
        data class Var(val name: String, val line: Int) : Expr()
        data class Bin(val op: String, val l: Expr, val r: Expr, val line: Int) : Expr()
        data class Un(val op: String, val e: Expr, val line: Int) : Expr()
        data class Assign(val op: String, val target: Expr, val value: Expr, val line: Int) : Expr()
        data class Call(val name: String, val args: List<Expr>, val line: Int) : Expr()
        data class Index(val arr: Expr, val idx: Expr, val line: Int) : Expr()
        data class Ternary(val cond: Expr, val a: Expr, val b: Expr) : Expr()
        data class PreInc(val op: String, val target: Expr, val line: Int) : Expr()
        data class PostInc(val op: String, val target: Expr, val line: Int) : Expr()
        data class Addr(val e: Expr, val line: Int) : Expr()
        data class Deref(val e: Expr, val line: Int) : Expr()
        data class Member(val obj: Expr, val name: String, val line: Int) : Expr()
        data class Arrow(val ptr: Expr, val name: String, val line: Int) : Expr()
        data class InitList(val items: List<Expr>) : Expr()
        data class SizeofType(val type: CType) : Expr()
        data class SizeofExpr(val e: Expr) : Expr()
    }

    private sealed class Stmt {
        data class Block(val body: List<Stmt>) : Stmt()
        data class Decl(val items: List<DeclItem>) : Stmt()
        data class If(val cond: Expr, val then: Stmt, val otherwise: Stmt?) : Stmt()
        data class While(val cond: Expr, val body: Stmt) : Stmt()
        data class DoWhile(val body: Stmt, val cond: Expr) : Stmt()
        data class For(val init: Stmt?, val cond: Expr?, val step: Expr?, val body: Stmt) : Stmt()
        data class Switch(val expr: Expr, val cases: List<SwitchCase>) : Stmt()
        data class Return(val value: Expr?) : Stmt()
        object Break : Stmt()
        object Continue : Stmt()
        data class ExprStmt(val e: Expr) : Stmt()
        object Empty : Stmt()
    }

    private data class SwitchCase(val label: Expr?, val body: List<Stmt>)
    private data class DeclItem(val name: String, val type: CType, val init: Expr?)

    private data class FuncDef(
        val name: String,
        val returnType: CType,
        val params: List<Pair<String, CType>>,
        val body: Stmt.Block
    )

    private data class Program(val funcs: Map<String, FuncDef>, val globals: List<DeclItem>)

    class CError(val type: String, message: String) : RuntimeException(message)

    // 解析器

    private class Parser(
        private val tokens: List<Token>,
        private val typedefs: MutableMap<String, CType> = linkedMapOf(),
        private val structDefs: MutableMap<String, StructDef> = linkedMapOf()
    ) {
        private var p = 0
        private val funcs = linkedMapOf<String, FuncDef>()
        private val globals = mutableListOf<DeclItem>()

        fun parseProgram(): Program {
            while (!atEnd()) {
                if (checkKeyword("typedef")) parseTypedef() else parseExternal()
            }
            return Program(funcs.toMap(), globals.toList())
        }

        /** 会话模式解析结果：函数定义与顶层语句分开返回 */
        class SessionParse(val funcs: Map<String, FuncDef>, val statements: List<Stmt>)

        // 会话模式下函数定义、typedef、struct 和普通语句可能混在一起。
        // 类型开头、名字后面跟 `{` 的算函数，其余按语句走。类型留在 Session 里，下一行还能用。
        fun parseProgramInSession(): SessionParse {
            val stmts = mutableListOf<Stmt>()
            while (!atEnd()) {
                when {
                    checkKeyword("typedef") -> parseTypedef()
                    isTypeStart() && looksLikeFunctionDecl() -> parseSessionFunction()
                    isTypeStart() -> stmts.add(parseDecl())
                    else -> stmts.add(parseStmt())
                }
            }
            return SessionParse(funcs.toMap(), stmts)
        }

        // 看一段是不是函数声明。原型和定义只差结尾，共用一套扫描，别写两份。
        private fun looksLikeFunctionDecl(): Boolean {
            var i = p
            var sawType = false
            while (i < tokens.size) {
                val t = tokens[i]
                if (t.type == T.KEYWORD) {
                    when (t.text) {
                        "const", "static", "unsigned", "signed", "short", "long",
                        "extern", "register", "volatile" -> { i++; continue }
                        "struct" -> {
                            i++; sawType = true
                            if (i < tokens.size && tokens[i].type == T.IDENT) i++
                            continue
                        }
                        "int", "double", "float", "char", "void" -> { i++; sawType = true; continue }
                        else -> break
                    }
                } else if (t.type == T.IDENT && typedefs.containsKey(t.text)) {
                    i++; sawType = true; continue
                }
                break
            }
            if (!sawType) return false
            while (i < tokens.size && tokens[i].text == "*") i++
            if (i >= tokens.size || tokens[i].type != T.IDENT) return false
            i++
            if (i >= tokens.size || tokens[i].text != "(") return false
            var depth = 0
            while (i < tokens.size) {
                if (tokens[i].text == "(") depth++
                if (tokens[i].text == ")") {
                    depth--
                    if (depth == 0) { i++; break }
                }
                i++
            }
            return i < tokens.size && (tokens[i].text == "{" || tokens[i].text == ";")
        }

        private fun parseExternal() {
            val base = parseDeclSpec()
            if (matchPunct(";")) return
            var first = true
            while (true) {
                val t = parsePointerStars(base, peek().line)
                val nameTok = expect(T.IDENT, "声明符")
                if (first && checkPunct("(")) {
                    parseFunction(nameTok, t)
                    return
                }
                first = false
                var full = t
                val dims = parseArrayDims()
                if (dims.isNotEmpty()) full = CType.TArray(full, dims)
                val init = if (matchOp("=")) parseInitializer() else null
                globals.add(DeclItem(nameTok.text, full, init))
                if (!matchPunct(",")) break
            }
            expectPunct(";")
        }

        private fun parseSessionFunction() {
            val base = parseDeclSpec()
            val t = parsePointerStars(base, peek().line)
            val nameTok = expect(T.IDENT, "函数名")
            parseFunction(nameTok, t)
        }

        private fun parseFunction(nameTok: Token, ret: CType) {
            expectPunct("(")
            val params = mutableListOf<Pair<String, CType>>()
            if (!checkPunct(")")) {
                if (checkKeyword("void") && p + 1 < tokens.size && tokens[p + 1].text == ")") {
                    advance()
                } else {
                    do {
                        val spec = parseDeclSpec()
                        var pt = parsePointerStars(spec, peek().line)
                        // 原型声明允许省略参数名：int add(int, int);
                        val pn = if (peek().type == T.IDENT) advance().text else ""
                        val dims = parseArrayDims()
                        // 一维数组参数在 C 里退化为指针：int a[] 等价 int *a。
                        // 二维数组参数保留二维视图（int m[][3]），否则 m[i][j] 无法按下标解释；
                        // 这是教学模型对真实 ABI 的简化，课程里只要求能读能写。
                        if (dims.size == 1) pt = CType.TPtr(pt)
                        else if (dims.size > 1) pt = CType.TArray(pt, dims)
                        if (pt is CType.TPtr && pt.to is CType.TPtr) {
                            throw CError("不支持", "暂不支持二级指针参数（第 ${peek().line} 行），例如 char *argv[]")
                        }
                        params.add(pn to pt)
                    } while (matchPunct(","))
                }
            }
            expectPunct(")")
            // 函数原型声明（以 ';' 结尾）：参数与返回类型已记录，函数体稍后定义即可。
            // 解析阶段已收集完整个翻译单元，调用不依赖声明顺序。
            if (matchPunct(";")) return
            val body = parseBlock()
            if (funcs.containsKey(nameTok.text)) {
                // 同一翻译单元里不能重定义。用户写个空 main、测试也带 main 时，
                // 覆盖会让测试的 main 顶掉用户代码，判题就成了假通过。
                throw CError("语法错误", "函数 '${nameTok.text}' 重复定义（第 ${nameTok.line} 行）")
            }
            funcs[nameTok.text] = FuncDef(nameTok.text, ret, params, body)
        }

        private fun parseTypedef() {
            advance() // typedef
            val base = parseDeclSpec()
            do {
                var t = parsePointerStars(base, peek().line)
                val name = expect(T.IDENT, "类型别名").text
                val dims = parseArrayDims()
                if (dims.isNotEmpty()) t = CType.TArray(t, dims)
                typedefs[name] = t
            } while (matchPunct(","))
            expectPunct(";")
        }

        /** 结构体说明符：struct Tag { ... } 或 struct Tag */
        private fun parseStructSpec(): CType.TStruct {
            advance() // struct
            val tag = if (peek().type == T.IDENT) advance().text else null
            if (checkPunct("{")) {
                advance()
                // 先登记空定义，支持 struct Node { struct Node *next; } 自引用。
                // 判题会把用户代码与测试拼成一个翻译单元，两边常各自声明同一个 struct：
                // 已有同名字段表时把新字段先收进 temp，再比对是否一致，一致就复用旧定义。
                val existing = tag?.let { structDefs[it] }
                val reuse = existing != null && existing.fields.isNotEmpty()
                val def = if (reuse) existing ?: StructDef(tag) else StructDef(tag)
                if (tag != null) structDefs[tag] = def
                val target = if (reuse) StructDef(tag) else def
                while (!checkPunct("}") && !atEnd()) {
                    val ft = parseDeclSpec()
                    do {
                        var ct = parsePointerStars(ft, peek().line)
                        val fn = expect(T.IDENT, "成员名").text
                        val dims = parseArrayDims()
                        if (dims.isNotEmpty()) ct = CType.TArray(ct, dims)
                        target.fields.add(fn to ct)
                    } while (matchPunct(","))
                    expectPunct(";")
                }
                expectPunct("}")
                if (target !== def) {
                    // 字段不一致说明真的是两份不同定义，明确报错而不是悄悄覆盖
                    val same = target.fields.size == def.fields.size &&
                        target.fields.zip(def.fields).all { (a, b) ->
                            a.first == b.first && a.second.display() == b.second.display()
                        }
                    if (!same) throw CError("语法错误", "结构体 '$tag' 重复定义且字段不一致")
                }
                return CType.TStruct(def)
            }
            if (tag == null) throw CError("语法错误", "匿名结构体必须直接定义（第 ${peek().line} 行）")
            // struct Point; 是前置声明：先登记空定义占位，真正使用时若仍无字段就报
            // 「不完整类型」，让错误发生在使用点而不是声明点。
            val def = structDefs.getOrPut(tag) { StructDef(tag) }
            return CType.TStruct(def)
        }

        private fun parseDeclSpec(): CType {
            var base: CType? = null
            var saw = false
            var modifier = false
            while (true) {
                val t = peek()
                if (t.type == T.KEYWORD) {
                    when (t.text) {
                        "const", "static", "unsigned", "signed", "short", "long",
                        "extern", "register", "volatile" -> { advance(); modifier = true; continue }
                        "int" -> { advance(); base = CType.TInt; saw = true; continue }
                        "double" -> { advance(); base = CType.TDouble; saw = true; continue }
                        "float" -> { advance(); base = CType.TDouble; saw = true; continue }
                        "char" -> { advance(); base = CType.TChar; saw = true; continue }
                        "void" -> { advance(); base = CType.TVoid; saw = true; continue }
                        "struct" -> { base = parseStructSpec(); saw = true; continue }
                        else -> break
                    }
                } else if (t.type == T.IDENT && typedefs.containsKey(t.text)) {
                    base = typedefs[t.text]
                    advance(); saw = true; continue
                }
                break
            }
            if (base == null && modifier) {
                // unsigned / short / long 单用：教学模型一律按 int 处理
                return CType.TInt
            }
            if (!saw || base == null) {
                throw CError("语法错误", "期望类型说明符，实际 '${peek().text}'（第 ${peek().line} 行）")
            }
            return base
        }


        /** 解析声明符里的 * 链；二级及以上明确报「不支持」，而不是让类型系统悄悄接受。 */
        private fun parsePointerStars(base: CType, line: Int): CType {
            var t = base
            var stars = 0
            while (matchOp("*")) { t = CType.TPtr(t); stars++ }
            if (stars > 0 && base is CType.TPtr) stars = 2
            if (stars > 1) {
                throw CError("不支持", "暂不支持二级指针（第 $line 行）。课程只讲单级指针。")
            }
            return t
        }

        private fun parseArrayDims(): List<Int> {
            val dims = mutableListOf<Int>()
            while (matchPunct("[")) {
                if (checkPunct("]")) {
                    dims.add(-1)
                    advance()
                } else {
                    val n = expect(T.NUM, "数组长度").text.toIntOrNull() ?: 0
                    expectPunct("]")
                    dims.add(n)
                }
            }
            return dims
        }

        private fun parseBlock(): Stmt.Block {
            expectPunct("{")
            val body = mutableListOf<Stmt>()
            while (!checkPunct("}") && !atEnd()) body.add(parseStmt())
            expectPunct("}")
            return Stmt.Block(body)
        }

        private fun parseStmt(): Stmt {
            if (checkPunct("{")) return parseBlock()
            if (checkPunct(";")) { advance(); return Stmt.Empty }
            if (checkKeyword("if")) {
                advance(); expectPunct("(")
                val cond = parseExpr(); expectPunct(")")
                val then = parseStmt()
                var other: Stmt? = null
                if (checkKeyword("else")) { advance(); other = parseStmt() }
                return Stmt.If(cond, then, other)
            }
            if (checkKeyword("while")) {
                advance(); expectPunct("(")
                val cond = parseExpr(); expectPunct(")")
                return Stmt.While(cond, parseStmt())
            }
            if (checkKeyword("do")) {
                advance()
                val body = parseStmt()
                if (!checkKeyword("while")) {
                    throw CError("语法错误", "do 语句后缺少 while（第 ${peek().line} 行）")
                }
                advance(); expectPunct("(")
                val cond = parseExpr(); expectPunct(")"); expectPunct(";")
                return Stmt.DoWhile(body, cond)
            }
            if (checkKeyword("for")) {
                advance(); expectPunct("(")
                val init: Stmt? = when {
                    checkPunct(";") -> { advance(); null }
                    isTypeStart() -> parseDecl()
                    else -> { val e = parseExpr(); expectPunct(";"); Stmt.ExprStmt(e) }
                }
                val cond: Expr? = if (checkPunct(";")) null else parseExpr()
                expectPunct(";")
                val step: Expr? = if (checkPunct(")")) null else parseExpr()
                expectPunct(")")
                return Stmt.For(init, cond, step, parseStmt())
            }
            if (checkKeyword("switch")) return parseSwitch()
            if (checkKeyword("return")) {
                advance()
                val v = if (checkPunct(";")) null else parseExpr()
                expectPunct(";")
                return Stmt.Return(v)
            }
            if (checkKeyword("break")) { advance(); expectPunct(";"); return Stmt.Break }
            if (checkKeyword("continue")) { advance(); expectPunct(";"); return Stmt.Continue }
            if (checkKeyword("typedef")) {
                parseTypedef()
                return Stmt.Empty
            }
            if (isTypeStart()) return parseDecl()
            val e = parseExpr()
            expectPunct(";")
            return Stmt.ExprStmt(e)
        }

        private fun parseSwitch(): Stmt {
            advance(); expectPunct("(")
            val selector = parseExpr()
            expectPunct(")"); expectPunct("{")
            val cases = mutableListOf<SwitchCase>()
            var currentLabel: Expr? = null
            var started = false
            var current = mutableListOf<Stmt>()

            fun flush() {
                if (started) cases.add(SwitchCase(currentLabel, current.toList()))
                current = mutableListOf()
                currentLabel = null
                started = false
            }

            while (!checkPunct("}") && !atEnd()) {
                when {
                    checkKeyword("case") -> {
                        flush(); advance()
                        currentLabel = parseExpr()
                        expectOp(":")
                        started = true
                    }
                    checkKeyword("default") -> {
                        flush(); advance()
                        expectOp(":")
                        currentLabel = null
                        started = true
                    }
                    else -> current.add(parseStmt())
                }
            }
            flush()
            expectPunct("}")
            return Stmt.Switch(selector, cases)
        }

        private fun parseDecl(): Stmt {
            val base = parseDeclSpec()
            if (matchPunct(";")) return Stmt.Empty
            val items = mutableListOf<DeclItem>()
            do {
                var t = parsePointerStars(base, peek().line)
                val name = expect(T.IDENT, "变量名").text
                val dims = parseArrayDims()
                if (dims.isNotEmpty()) t = CType.TArray(t, dims)
                val init = if (matchOp("=")) parseInitializer() else null
                items.add(DeclItem(name, t, init))
            } while (matchPunct(","))
            expectPunct(";")
            return Stmt.Decl(items)
        }

        // ---- 表达式（优先级与 C 一致，自上而下） ----

        fun parseExpr(): Expr = parseAssignment()

        private fun parseAssignment(): Expr {
            val left = parseTernary()
            val op = ASSIGN_OPS.firstOrNull { checkOp(it) }
            if (op != null) {
                val line = peek().line
                advance()
                return Expr.Assign(op, left, parseAssignment(), line)
            }
            return left
        }

        private fun parseTernary(): Expr {
            val cond = parseLogicalOr()
            if (checkOp("?")) {
                advance()
                val a = parseExpr()
                expectOp(":")
                val b = parseTernary()
                return Expr.Ternary(cond, a, b)
            }
            return cond
        }

        private fun parseLogicalOr(): Expr {
            var e = parseLogicalAnd()
            while (checkOp("||")) {
                val line = peek().line; advance()
                e = Expr.Bin("||", e, parseLogicalAnd(), line)
            }
            return e
        }

        private fun parseLogicalAnd(): Expr {
            var e = parseBitOr()
            while (checkOp("&&")) {
                val line = peek().line; advance()
                e = Expr.Bin("&&", e, parseBitOr(), line)
            }
            return e
        }

        private fun parseBitOr(): Expr {
            var e = parseBitXor()
            while (checkOp("|")) {
                val line = peek().line; advance()
                e = Expr.Bin("|", e, parseBitXor(), line)
            }
            return e
        }

        private fun parseBitXor(): Expr {
            var e = parseBitAnd()
            while (checkOp("^")) {
                val line = peek().line; advance()
                e = Expr.Bin("^", e, parseBitAnd(), line)
            }
            return e
        }

        private fun parseBitAnd(): Expr {
            var e = parseEquality()
            while (checkOp("&")) {
                val line = peek().line; advance()
                e = Expr.Bin("&", e, parseEquality(), line)
            }
            return e
        }

        private fun parseEquality(): Expr {
            var e = parseRelational()
            while (checkOp("==") || checkOp("!=")) {
                val op = peek().text; val line = peek().line; advance()
                e = Expr.Bin(op, e, parseRelational(), line)
            }
            return e
        }

        private fun parseRelational(): Expr {
            var e = parseShift()
            while (checkOp("<") || checkOp(">") || checkOp("<=") || checkOp(">=")) {
                val op = peek().text; val line = peek().line; advance()
                e = Expr.Bin(op, e, parseShift(), line)
            }
            return e
        }

        private fun parseShift(): Expr {
            var e = parseAdditive()
            while (checkOp("<<") || checkOp(">>")) {
                val op = peek().text; val line = peek().line; advance()
                e = Expr.Bin(op, e, parseAdditive(), line)
            }
            return e
        }

        private fun parseAdditive(): Expr {
            var e = parseMultiplicative()
            while (checkOp("+") || checkOp("-")) {
                val op = peek().text; val line = peek().line; advance()
                e = Expr.Bin(op, e, parseMultiplicative(), line)
            }
            return e
        }

        private fun parseMultiplicative(): Expr {
            var e = parseUnary()
            while (checkOp("*") || checkOp("/") || checkOp("%")) {
                val op = peek().text; val line = peek().line; advance()
                e = Expr.Bin(op, e, parseUnary(), line)
            }
            return e
        }

        private fun parseUnary(): Expr {
            val line = peek().line
            if (checkOp("-")) { advance(); return Expr.Un("-", parseUnary(), line) }
            if (checkOp("+")) { advance(); return Expr.Un("+", parseUnary(), line) }
            if (checkOp("!")) { advance(); return Expr.Un("!", parseUnary(), line) }
            if (checkOp("~")) { advance(); return Expr.Un("~", parseUnary(), line) }
            if (checkOp("&")) { advance(); return Expr.Addr(parseUnary(), line) }
            if (checkOp("*")) { advance(); return Expr.Deref(parseUnary(), line) }
            if (checkOp("++")) { advance(); return Expr.PreInc("++", parseUnary(), line) }
            if (checkOp("--")) { advance(); return Expr.PreInc("--", parseUnary(), line) }
            if (checkKeyword("sizeof")) {
                advance()
                if (checkPunct("(") && isTypeStartAt(p + 1)) {
                    advance()
                    var t = parsePointerStars(parseDeclSpec(), peek().line)
                    val dims = parseArrayDims()
                    if (dims.isNotEmpty()) t = CType.TArray(t, dims)
                    expectPunct(")")
                    return Expr.SizeofType(t)
                }
                return Expr.SizeofExpr(parseUnary())
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Expr {
            var e = parsePrimary()
            while (true) {
                when {
                    checkPunct("[") -> {
                        val line = peek().line
                        advance()
                        val idx = parseExpr()
                        expectPunct("]")
                        e = Expr.Index(e, idx, line)
                    }
                    checkOp(".") -> {
                        val line = peek().line
                        advance()
                        val name = expect(T.IDENT, "成员名").text
                        e = Expr.Member(e, name, line)
                    }
                    checkOp("->") -> {
                        val line = peek().line
                        advance()
                        val name = expect(T.IDENT, "成员名").text
                        e = Expr.Arrow(e, name, line)
                    }
                    checkOp("++") -> { val line = peek().line; advance(); e = Expr.PostInc("++", e, line) }
                    checkOp("--") -> { val line = peek().line; advance(); e = Expr.PostInc("--", e, line) }
                    else -> return e
                }
            }
        }

        private fun parsePrimary(): Expr {
            val t = peek()
            return when (t.type) {
                T.NUM -> { advance(); Expr.NumLit(t.text.toLongOrNull() ?: 0L) }
                T.REAL -> { advance(); Expr.RealLit(t.text.toDoubleOrNull() ?: 0.0) }
                T.STR -> { advance(); Expr.StrLit(t.text) }
                T.CHARLIT -> { advance(); Expr.NumLit(t.text.toLongOrNull() ?: 0L) }
                T.IDENT -> {
                    advance()
                    if (checkPunct("(")) {
                        advance()
                        val args = mutableListOf<Expr>()
                        if (!checkPunct(")")) {
                            do { args.add(parseExpr()) } while (matchPunct(","))
                        }
                        expectPunct(")")
                        Expr.Call(t.text, args, t.line)
                    } else if (t.text == "NULL") {
                        // C 的 NULL 通常来自宏；教学子集直接内置成 0，不要求 include
                        Expr.NumLit(0)
                    } else {
                        Expr.Var(t.text, t.line)
                    }
                }
                T.PUNCT -> {
                    if (t.text == "(") {
                        advance(); val e = parseExpr(); expectPunct(")"); e
                    } else {
                        throw CError("语法错误", "意外的符号 '${t.text}'（第 ${t.line} 行）")
                    }
                }
                else -> throw CError("语法错误", "意外的标记 '${t.text}'（第 ${t.line} 行）")
            }
        }

        private fun parseInitializer(): Expr {
            if (!checkPunct("{")) return parseAssignment()
            advance()
            val items = mutableListOf<Expr>()
            if (!checkPunct("}")) {
                do {
                    items.add(parseInitializer())
                    if (!matchPunct(",")) break
                } while (!checkPunct("}"))
            }
            expectPunct("}")
            return Expr.InitList(items)
        }

        // ---- 辅助 ----

        private fun peek() = tokens[p]
        private fun atEnd() = peek().type == T.EOF
        private fun advance(): Token = tokens[p].also { if (!atEnd()) p++ }

        private fun checkKeyword(k: String) = peek().type == T.KEYWORD && peek().text == k
        private fun checkPunct(s: String) = peek().type == T.PUNCT && peek().text == s
        private fun checkOp(s: String) = peek().type == T.OP && peek().text == s
        private fun matchPunct(s: String): Boolean { if (checkPunct(s)) { advance(); return true }; return false }
        private fun matchOp(s: String): Boolean { if (checkOp(s)) { advance(); return true }; return false }

        private fun expectPunct(s: String) {
            if (!checkPunct(s)) throw CError("语法错误", "期望 '$s'，实际 '${peek().text}'（第 ${peek().line} 行）")
            advance()
        }

        private fun expectOp(s: String) {
            if (!checkOp(s)) throw CError("语法错误", "期望 '$s'，实际 '${peek().text}'（第 ${peek().line} 行）")
            advance()
        }

        private fun expect(t: T, what: String): Token {
            if (peek().type != t) throw CError("语法错误", "期望$what，实际 '${peek().text}'（第 ${peek().line} 行）")
            return advance()
        }

        private fun isTypeStart(): Boolean = isTypeStartAt(p)

        private fun isTypeStartAt(index: Int): Boolean {
            if (index >= tokens.size) return false
            val t = tokens[index]
            return (t.type == T.KEYWORD && t.text in TYPE_KEYWORDS) ||
                (t.type == T.IDENT && typedefs.containsKey(t.text))
        }
    }

    // 运行时

    private class BreakSignal : RuntimeException()
    private class ContinueSignal : RuntimeException()
    private class ReturnSignal(val value: Value) : RuntimeException()

    private class Scope(val parent: Scope?) {
        val vars = linkedMapOf<String, Cell>()
        fun lookup(name: String): Cell? = vars[name] ?: parent?.lookup(name)
        fun find(name: String): Scope? = if (vars.containsKey(name)) this else parent?.find(name)
    }

    private class Loc(val cells: List<Cell>, val index: Int)

    private class Interpreter(
        private val funcs: Map<String, FuncDef>,
        private val out: StringBuilder,
        private val stdin: ArrayDeque<String>,
        private val globalDecls: List<DeclItem> = emptyList()
    ) {
        private var scopes = mutableListOf(Scope(null))
        private var globalRoot: Scope = scopes.first()
        private var steps = 0
        private val maxSteps = 2_000_000

        init {
            // 全局变量在入口函数之前初始化，作用域挂在全局 scope 上
            for (d in globalDecls) exec(Stmt.Decl(listOf(d)))
        }

        /** 会话模式：接管外部传入的作用域，使变量跨 push 保留 */
        fun adoptScope(shared: Scope) {
            scopes = mutableListOf(shared)
            globalRoot = shared
        }

        fun callEntry(entry: String): Value {
            val fn = funcs[entry] ?: throw CError("运行时错误", "缺少 $entry 函数")
            return invoke(fn, emptyList())
        }

        /**
         * 会话式执行：把一段语句（不是完整程序）当作 main 的函数体跑。
         *
         * REPL 需要这个入口。用户在终端里敲 `int x = 5;` 时，不该被迫写
         * `int main() { ... }`，也不该每行都重新开始（变量必须跨行保留）。
         */
        fun runStatements(stmts: List<Stmt>) {
            for (s in stmts) exec(s)
        }

        /** 会话中的全局变量快照（供 REPL 展示） */
        fun globalVars(): List<Triple<String, String, String>> =
            scopes.firstOrNull()?.vars?.map { (k, cell) ->
                Triple(k, cell.type.display(), cell.value.display())
            } ?: emptyList()

        /**
         * 判题入口：先跑用户代码的 main（产出真实输出），再跑测试入口。
         *
         * 两步必须共用一个输出缓冲区，这样测试里的 assertOutput 才能看到
         * 用户 main 打印了什么。若用户没有 main，则只跑测试（允许练习
         * 只实现函数、由测试自己调用的情况）。
         */
        fun callMainThenTest(testEntry: String) {
            funcs["main"]?.let { invoke(it, emptyList()) }
            val test = funcs[testEntry] ?: throw CError("运行时错误", "缺少测试入口 $testEntry")
            invoke(test, emptyList())
        }

        /** 输出上限：防止死循环 printf 把内存打爆（1 MB 足够课程所有场景）。 */
        private fun emit(text: String) {
            if (out.length + text.length > MAX_OUTPUT_CHARS) {
                throw CError("Timeout", "输出超过 ${MAX_OUTPUT_CHARS / 1024} KB 上限（疑似死循环打印）")
            }
            out.append(text)
        }

        private fun tick() {
            if (++steps > maxSteps) throw CError("Timeout", "执行步数超过上限（疑似死循环）")
        }

        private fun invoke(fn: FuncDef, args: List<Value>): Value {
            if (args.size != fn.params.size) {
                throw CError("运行时错误", "函数 ${fn.name} 期望 ${fn.params.size} 个参数，实际 ${args.size} 个")
            }
            // 函数作用域的父级必须是全局作用域，不是调用点：否则被调函数能看见
            // 调用者的局部变量，等于动态作用域，与 C 的词法作用域不符。
            val scope = Scope(globalRoot)
            fn.params.forEachIndexed { i, (name, ty) ->
                val v = coerce(args[i], ty)
                // 二维数组参数写的是 int m[][3]，第一维长度由实参决定；
                // 用实参的真实维度补全，函数内 sizeof 才不会算出负数。
                val actual = if (ty is CType.TArray && v is Value.Arr && ty.dims.firstOrNull() == -1) {
                    CType.TArray(ty.of, v.dims)
                } else ty
                scope.vars[name] = Cell(v, actual)
            }
            val saved = scopes
            scopes = mutableListOf(scope)
            val result = try {
                execBlock(fn.body)
                Value.Void
            } catch (r: ReturnSignal) {
                r.value
            } finally {
                scopes = saved
            }
            return coerce(result, fn.returnType)
        }

        private fun coerce(v: Value, ty: CType): Value {
            // 函数没写 return 就落到末尾时值为 Void，按 C 的「未定义」给类型的零值，
            // 不要抛出「不能把空值赋给 int」这种对学习者没有意义的错误。
            if (v is Value.Void) return defaultValue(ty)
            return coerceNonNull(v, ty)
        }

        private fun coerceNonNull(v: Value, ty: CType): Value = when (ty) {
            CType.TDouble -> when (v) {
                is Value.Real -> v
                is Value.Num -> Value.Real(v.v.toDouble())
                else -> throw CError("运行时错误", "不能把 ${v.display()} 赋给数值变量")
            }
            CType.TChar -> when (v) {
                is Value.Num -> Value.Num(v.v)
                is Value.Real -> Value.Num(v.v.toLong())
                else -> throw CError("运行时错误", "不能把 ${v.display()} 赋给 char")
            }
            CType.TInt -> when (v) {
                is Value.Real -> Value.Num(v.v.toLong())
                is Value.Num -> Value.Num(v.v)
                else -> throw CError("运行时错误", "不能把 ${v.display()} 赋给 int")
            }
            CType.TVoid -> v
            is CType.TPtr -> when (v) {
                is Value.Ptr -> v
                is Value.Arr -> Value.Ptr(v.cells, 0)
                // 字符串字面量转成可寻址的 char 单元，这样 p[0]、p++、*(p+1) 都成立
                is Value.Str -> if (ty.to === CType.TChar) stringToPtr(v.v)
                    else throw CError("运行时错误", "不能把字符串赋给非 char 指针")
                is Value.Num -> if (v.v == 0L) Value.Ptr(emptyList(), 0)
                else throw CError("运行时错误", "不能把 ${v.display()} 赋给指针")
                else -> throw CError("运行时错误", "不能把该值赋给指针")
            }
            is CType.TStruct -> when (v) {
                is Value.Struct -> copyStruct(v)
                else -> throw CError("运行时错误", "类型不匹配：需要结构体 ${ty.def.tag ?: ""}")
            }
            is CType.TArray -> when (v) {
                // 二维数组作参数时按视图传递（教学简化），元素数必须匹配
                is Value.Arr -> if (ty.dims.first() < 0 || ty.dims.first() * ty.dims.drop(1).fold(1) { a, b -> a * b } == v.cells.size) v
                    else throw CError("运行时错误", "二维数组实参尺寸不匹配")
                else -> throw CError("运行时错误", "不能整体赋值给数组")
            }
        }

        private fun defaultValue(t: CType): Value = when (t) {
            CType.TInt -> Value.Num(0)
            CType.TChar -> Value.Num(0)
            CType.TDouble -> Value.Real(0.0)
            CType.TVoid -> Value.Void
            is CType.TPtr -> Value.Ptr(emptyList(), 0)
            is CType.TStruct -> newStruct(t.def)
            is CType.TArray -> {
                if (t.dims.any { it < 0 }) throw CError("运行时错误", "数组长度未知")
                val total = checkedArraySize(t.dims)
                Value.Arr(MutableList(total) { Cell(defaultValue(t.of), t.of) }, t.of, t.dims)
            }
        }

        /** 数组元素数上限：教学场景不可能需要 10 万以上元素，超过直接拒绝以挡住 OOM。 */
        private fun checkedArraySize(dims: List<Int>): Int {
            var total = 1L
            for (d in dims) {
                if (d <= 0) throw CError("语法错误", "数组长度必须是正数")
                total *= d
                if (total > MAX_ARRAY_ELEMENTS) {
                    throw CError("运行时错误", "数组过大：${dims.joinToString("x")} 超过 $MAX_ARRAY_ELEMENTS 个元素")
                }
            }
            return total.toInt()
        }

        private fun stringToPtr(text: String): Value.Ptr {
            // 字面量放在只读单元里：char *s = "abc"; s[0] = 'x' 在 C 里是未定义行为，
            // 这里明确报错，避免课程里养成改字面量的坏习惯。
            val cells = MutableList(text.length + 1) {
                Cell(Value.Num(if (it < text.length) text[it].code.toLong() else 0), CType.TChar, readonly = true)
            }
            return Value.Ptr(cells, 0)
        }

        private fun newStruct(def: StructDef): Value.Struct {
            if (def.fields.isEmpty()) {
                throw CError("运行时错误", "结构体 '${def.tag ?: "?"}' 是不完整类型（只声明未定义成员）")
            }
            val fields = linkedMapOf<String, Cell>()
            for ((name, ty) in def.fields) fields[name] = Cell(defaultValue(ty), ty)
            return Value.Struct(def, fields)
        }

        private fun copyStruct(v: Value.Struct): Value.Struct {
            val fields = linkedMapOf<String, Cell>()
            for ((name, cell) in v.fields) fields[name] = Cell(copyValue(cell.value), cell.type)
            return Value.Struct(v.def, fields)
        }

        private fun copyValue(v: Value): Value = when (v) {
            is Value.Struct -> copyStruct(v)
            is Value.Arr -> Value.Arr(v.cells.map { Cell(copyValue(it.value), it.type) }, v.elem, v.dims)
            else -> v
        }

        private fun execBlock(b: Stmt.Block) {
            val saved = scopes
            scopes = mutableListOf(Scope(saved.first()))
            try {
                for (s in b.body) exec(s)
            } finally {
                scopes = saved
            }
        }

        private fun exec(s: Stmt) {
            tick()
            when (s) {
                is Stmt.Block -> execBlock(s)
                is Stmt.Empty -> Unit
                is Stmt.Decl -> for (d in s.items) {
                    val t = d.type
                    if (t is CType.TArray && t.dims.any { it < 0 } && d.init == null) {
                        throw CError("运行时错误", "数组 '${d.name}' 长度未知，声明时必须初始化")
                    }
                    val v = d.init?.let { initValue(it, t) } ?: defaultValue(t)
                    // 长度省略的 char s[] = "abc" 在运行时才知道长度，把推断结果写回类型，
                    // 这样 sizeof(s) 才能拿到真实长度而不是 -1。
                    val actualType = if (t is CType.TArray && t.dims.any { it < 0 } && v is Value.Arr) {
                        CType.TArray(t.of, v.dims)
                    } else t
                    scopes.first().vars[d.name] = Cell(v, actualType)
                }
                is Stmt.If -> if (eval(s.cond).truthy()) exec(s.then) else s.otherwise?.let { exec(it) }
                is Stmt.While -> {
                    var guard = 0
                    while (eval(s.cond).truthy()) {
                        tick()
                        if (++guard > maxSteps) throw CError("Timeout", "循环次数超过上限（疑似死循环）")
                        try { exec(s.body) }
                        catch (_: BreakSignal) { break }
                        catch (_: ContinueSignal) { continue }
                    }
                }
                is Stmt.DoWhile -> {
                    var guard = 0
                    while (true) {
                        tick()
                        if (++guard > maxSteps) throw CError("Timeout", "循环次数超过上限（疑似死循环）")
                        try { exec(s.body) }
                        catch (_: BreakSignal) { break }
                        catch (_: ContinueSignal) { /* 跳到条件判断 */ }
                        if (!eval(s.cond).truthy()) break
                    }
                }
                is Stmt.For -> {
                    val saved = scopes
                    scopes = mutableListOf(Scope(saved.first()))
                    try {
                        s.init?.let { exec(it) }
                        var guard = 0
                        while (s.cond == null || eval(s.cond).truthy()) {
                            tick()
                            if (++guard > maxSteps) throw CError("Timeout", "循环次数超过上限（疑似死循环）")
                            try { exec(s.body) }
                            catch (_: BreakSignal) { break }
                            catch (_: ContinueSignal) { /* 继续执行 step */ }
                            s.step?.let { eval(it) }
                        }
                    } finally {
                        scopes = saved
                    }
                }
                is Stmt.Switch -> execSwitch(s)
                is Stmt.Return -> throw ReturnSignal(s.value?.let { eval(it) } ?: Value.Void)
                Stmt.Break -> throw BreakSignal()
                Stmt.Continue -> throw ContinueSignal()
                is Stmt.ExprStmt -> eval(s.e)
            }
        }

        /**
         * switch 语义：从命中的 case 开始顺序执行，不写 break 就 fallthrough；
         * break 只跳出 switch（被这里捕获），continue 继续向上抛给外层循环。
         */
        private fun execSwitch(s: Stmt.Switch) {
            val v = eval(s.expr).asLong()
            var start = -1
            for (i in s.cases.indices) {
                val label = s.cases[i].label ?: continue
                if (eval(label).asLong() == v) { start = i; break }
            }
            if (start < 0) start = s.cases.indexOfFirst { it.label == null }
            if (start < 0) return
            val saved = scopes
            scopes = mutableListOf(Scope(saved.first()))
            try {
                var i = start
                while (i < s.cases.size) {
                    try {
                        for (st in s.cases[i].body) exec(st)
                    } catch (_: BreakSignal) {
                        break
                    }
                    i++
                }
            } finally {
                scopes = saved
            }
        }

        private fun eval(e: Expr): Value {
            tick()
            return when (e) {
                is Expr.NumLit -> Value.Num(e.v)
                is Expr.RealLit -> Value.Real(e.v)
                is Expr.StrLit -> Value.Str(e.v)
                is Expr.Var -> scopes.first().lookup(e.name)?.value
                    ?: throw CError("运行时错误", "未声明的变量 '${e.name}'（第 ${e.line} 行）")
                is Expr.Bin -> binop(e)
                is Expr.Un -> unop(e)
                is Expr.Assign -> assign(e)
                is Expr.Call -> callBuiltinOrUser(e)
                is Expr.Index -> evalIndex(e)
                is Expr.Ternary -> if (eval(e.cond).truthy()) eval(e.a) else eval(e.b)
                is Expr.PreInc -> {
                    val loc = locate(e.target)
                    val delta = if (e.op == "++") 1L else -1L
                    val nv = incValue(loc.cells[loc.index].value, delta)
                    writeLoc(loc, nv, e.line)
                    nv
                }
                is Expr.PostInc -> {
                    val loc = locate(e.target)
                    val delta = if (e.op == "++") 1L else -1L
                    val cur = loc.cells[loc.index].value
                    writeLoc(loc, incValue(cur, delta), e.line)
                    cur
                }
                is Expr.Addr -> {
                    val loc = locate(e.e)
                    Value.Ptr(loc.cells, loc.index)
                }
                is Expr.Deref -> readPointer(e)
                is Expr.Member -> readMember(e)
                is Expr.Arrow -> readArrow(e)
                is Expr.InitList -> throw CError("语法错误", "初始化列表只能出现在声明里")
                is Expr.SizeofType -> Value.Num(sizeOf(e.type).toLong())
                is Expr.SizeofExpr -> Value.Num(sizeOf(typeOf(e.e)).toLong())
            }
        }

        private fun evalIndex(e: Expr.Index): Value {
            val base = eval(e.arr)
            val idx = eval(e.idx).asLong().toInt()
            return when (base) {
                is Value.Arr -> {
                    if (base.dims.size > 1) {
                        if (idx < 0 || idx >= base.dims[0]) {
                            throw CError("运行时错误", "数组下标越界：$idx（长度 ${base.dims[0]}，第 ${e.line} 行）")
                        }
                        val rowSize = base.cells.size / base.dims[0]
                        Value.Arr(
                            base.cells.subList(idx * rowSize, (idx + 1) * rowSize),
                            base.elem, base.dims.drop(1)
                        )
                    } else {
                        if (idx < 0 || idx >= base.cells.size) {
                            throw CError("运行时错误", "数组下标越界：$idx（长度 ${base.cells.size}，第 ${e.line} 行）")
                        }
                        base.cells[idx].value
                    }
                }
                is Value.Ptr -> {
                    val at = base.index + idx
                    if (base.cells.isEmpty()) throw CError("运行时错误", "空指针解引用（第 ${e.line} 行）")
                    if (at < 0 || at >= base.cells.size) {
                        throw CError("运行时错误", "指针下标越界：$idx（第 ${e.line} 行）")
                    }
                    base.cells[at].value
                }
                is Value.Str -> {
                    if (idx < 0 || idx > base.v.length) {
                        throw CError("运行时错误", "字符串下标越界：$idx（第 ${e.line} 行）")
                    }
                    Value.Num(if (idx == base.v.length) 0 else base.v[idx].code.toLong())
                }
                else -> throw CError("运行时错误", "对非数组/非指针使用下标（第 ${e.line} 行）")
            }
        }

        private fun readPointer(e: Expr.Deref): Value {
            val p = eval(e.e)
            if (p !is Value.Ptr) throw CError("运行时错误", "解引用目标不是指针（第 ${e.line} 行）")
            if (p.cells.isEmpty()) throw CError("运行时错误", "空指针解引用（第 ${e.line} 行）")
            if (p.index < 0 || p.index >= p.cells.size) {
                throw CError("运行时错误", "指针越界（第 ${e.line} 行）")
            }
            return p.cells[p.index].value
        }

        private fun readMember(e: Expr.Member): Value {
            val obj = eval(e.obj)
            if (obj !is Value.Struct) {
                throw CError("运行时错误", "对非结构体使用 '.'（第 ${e.line} 行）")
            }
            val cell = obj.fields[e.name]
                ?: throw CError("运行时错误", "结构体没有成员 '${e.name}'（第 ${e.line} 行）")
            return cell.value
        }

        private fun readArrow(e: Expr.Arrow): Value {
            val p = eval(e.ptr)
            if (p !is Value.Ptr) throw CError("运行时错误", "'->' 左边不是指针（第 ${e.line} 行）")
            if (p.cells.isEmpty()) throw CError("运行时错误", "空指针解引用（第 ${e.line} 行）")
            if (p.index < 0 || p.index >= p.cells.size) {
                throw CError("运行时错误", "指针越界（第 ${e.line} 行）")
            }
            val obj = p.cells[p.index].value
            if (obj !is Value.Struct) {
                throw CError("运行时错误", "'->' 左边指向的不是结构体（第 ${e.line} 行）")
            }
            return obj.fields[e.name]?.value
                ?: throw CError("运行时错误", "结构体没有成员 '${e.name}'（第 ${e.line} 行）")
        }

        private fun locate(t: Expr): Loc = when (t) {
            is Expr.Var -> {
                val cell = scopes.first().lookup(t.name)
                    ?: throw CError("运行时错误", "未声明的变量 '${t.name}'（第 ${t.line} 行）")
                Loc(listOf(cell), 0)
            }
            is Expr.Index -> {
                val base = eval(t.arr)
                val idx = eval(t.idx).asLong().toInt()
                when (base) {
                    is Value.Arr -> {
                        if (base.dims.size > 1) {
                            throw CError("运行时错误", "不能整体给二维数组的一行赋值（第 ${t.line} 行）")
                        }
                        if (idx < 0 || idx >= base.cells.size) {
                            throw CError("运行时错误", "数组下标越界：$idx（长度 ${base.cells.size}，第 ${t.line} 行）")
                        }
                        Loc(base.cells, idx)
                    }
                    is Value.Ptr -> {
                        if (base.cells.isEmpty()) {
                            throw CError("运行时错误", "空指针解引用（第 ${t.line} 行）")
                        }
                        val at = base.index + idx
                        if (at < 0 || at >= base.cells.size) {
                            throw CError("运行时错误", "指针下标越界：$idx（第 ${t.line} 行）")
                        }
                        Loc(base.cells, at)
                    }
                    is Value.Str -> throw CError("运行时错误", "不能修改字符串字面量（第 ${t.line} 行）")
                    else -> throw CError("运行时错误", "对非数组/非指针使用下标（第 ${t.line} 行）")
                }
            }
            is Expr.Deref -> {
                val p = eval(t.e)
                if (p !is Value.Ptr) throw CError("运行时错误", "解引用目标不是指针（第 ${t.line} 行）")
                if (p.cells.isEmpty()) throw CError("运行时错误", "空指针解引用（第 ${t.line} 行）")
                if (p.index < 0 || p.index >= p.cells.size) {
                    throw CError("运行时错误", "指针越界（第 ${t.line} 行）")
                }
                Loc(p.cells, p.index)
            }
            is Expr.Member -> {
                val obj = eval(t.obj)
                if (obj !is Value.Struct) {
                    throw CError("运行时错误", "对非结构体使用 '.'（第 ${t.line} 行）")
                }
                val cell = obj.fields[t.name]
                    ?: throw CError("运行时错误", "结构体没有成员 '${t.name}'（第 ${t.line} 行）")
                Loc(listOf(cell), 0)
            }
            is Expr.Arrow -> {
                val p = eval(t.ptr)
                if (p !is Value.Ptr) throw CError("运行时错误", "'->' 左边不是指针（第 ${t.line} 行）")
                if (p.cells.isEmpty()) throw CError("运行时错误", "空指针解引用（第 ${t.line} 行）")
                if (p.index < 0 || p.index >= p.cells.size) {
                    throw CError("运行时错误", "指针越界（第 ${t.line} 行）")
                }
                val obj = p.cells[p.index].value
                if (obj !is Value.Struct) {
                    throw CError("运行时错误", "'->' 左边指向的不是结构体（第 ${t.line} 行）")
                }
                val cell = obj.fields[t.name]
                    ?: throw CError("运行时错误", "结构体没有成员 '${t.name}'（第 ${t.line} 行）")
                Loc(listOf(cell), 0)
            }
            else -> throw CError("运行时错误", "赋值目标不是变量（第 ${t.line()} 行）")
        }

        private fun Expr.line(): Int = when (this) {
            is Expr.Var -> line
            is Expr.Bin -> line
            is Expr.Un -> line
            is Expr.Assign -> line
            is Expr.Call -> line
            is Expr.Index -> line
            is Expr.PreInc -> line
            is Expr.PostInc -> line
            is Expr.Addr -> line
            is Expr.Deref -> line
            is Expr.Member -> line
            is Expr.Arrow -> line
            else -> 0
        }

        private fun writeLoc(loc: Loc, v: Value, line: Int) {
            val cell = loc.cells[loc.index]
            if (cell.readonly) {
                throw CError("运行时错误", "不能修改字符串字面量（第 $line 行）")
            }
            if (cell.type is CType.TArray) {
                throw CError("运行时错误", "不能给数组整体赋值（第 $line 行）")
            }
            cell.value = coerce(v, cell.type)
        }

        private fun assign(e: Expr.Assign): Value {
            val loc = locate(e.target)
            val old = loc.cells[loc.index].value
            if (old is Value.Ptr && e.op != "=" && e.op != "+=" && e.op != "-=") {
                throw CError("运行时错误", "指针只支持 += / -=（第 ${e.line} 行）")
            }
            val v = if (e.op == "=") {
                eval(e.value)
            } else {
                applyBinary(e.op.dropLast(1), old, eval(e.value), e.line)
            }
            writeLoc(loc, v, e.line)
            return loc.cells[loc.index].value
        }

        private fun incValue(v: Value, delta: Long): Value = when (v) {
            is Value.Ptr -> Value.Ptr(v.cells, v.index + delta.toInt())
            is Value.Real -> Value.Real(v.v + delta)
            else -> Value.Num(v.asLong() + delta)
        }

        private fun binop(e: Expr.Bin): Value {
            if (e.op == "&&") {
                if (!eval(e.l).truthy()) return Value.Num(0)
                return Value.Num(if (eval(e.r).truthy()) 1 else 0)
            }
            if (e.op == "||") {
                if (eval(e.l).truthy()) return Value.Num(1)
                return Value.Num(if (eval(e.r).truthy()) 1 else 0)
            }
            return applyBinary(e.op, eval(e.l), eval(e.r), e.line)
        }

        private fun applyBinary(op0: String, l0: Value, r0: Value, line: Int): Value {
            // 内容比较必须用 strcmp：字面量直接 == 比较的是地址，课程里最容易写错
            if ((op0 == "==" || op0 == "!=") && (l0 is Value.Str || r0 is Value.Str)) {
                throw CError("不支持", "字符串内容比较请用 strcmp(a, b) == 0（第 $line 行）")
            }
            // 数组与字符串字面量参与运算时退化为指向首元素的指针（C 的 array-to-pointer decay）。
            // 注意不再提供 "a" + "b" 这种非 C 的字符串拼接扩展：课程要教的是指针语义。
            val l = when (l0) {
                is Value.Arr -> Value.Ptr(l0.cells, 0)
                is Value.Str -> stringToPtr(l0.v)
                else -> l0
            }
            val r = when (r0) {
                is Value.Arr -> Value.Ptr(r0.cells, 0)
                is Value.Str -> stringToPtr(r0.v)
                else -> r0
            }
            val op = op0
            if (op == "==" || op == "!=") {
                if (l is Value.Ptr || r is Value.Ptr) {
                    val eq = valueEquals(l, r)
                    return Value.Num(if (op == "==" && eq || op == "!=" && !eq) 1 else 0)
                }
            }
            // 指针算术：p + n、n + p、p - n、p - q
            if (op == "+" && l is Value.Ptr && r is Value.Num) {
                return Value.Ptr(l.cells, l.index + r.v.toInt())
            }
            if (op == "+" && l is Value.Num && r is Value.Ptr) {
                return Value.Ptr(r.cells, r.index + l.v.toInt())
            }
            if (op == "-" && l is Value.Ptr && r is Value.Num) {
                return Value.Ptr(l.cells, l.index - r.v.toInt())
            }
            if (op == "-" && l is Value.Ptr && r is Value.Ptr) {
                if (l.cells !== r.cells) throw CError("运行时错误", "不同数组的指针不能相减（第 $line 行）")
                return Value.Num((l.index - r.index).toLong())
            }
            if (l is Value.Struct || r is Value.Struct) {
                throw CError("运行时错误", "结构体不能参与算术或逻辑运算（第 $line 行）")
            }
            if (l is Value.Ptr || r is Value.Ptr) {
                // 只有 p±n、p-q、指针比较是合法运算，其余明确报错而不是算出垃圾值
                val ok = op in setOf("==", "!=", "<", ">", "<=", ">=")
                if (!ok) throw CError("运行时错误", "指针不支持运算符 $op（第 $line 行）")
                if (l is Value.Ptr && r is Value.Ptr) {
                    // 同一数组内的指针按位置比较；不同数组比较在 C 里是未定义行为
                    if (op in setOf("<", ">", "<=", ">=")) {
                        if (l.cells !== r.cells) {
                            throw CError("运行时错误", "不同数组的指针不能比较大小（第 $line 行）")
                        }
                        val a = l.index; val b = r.index
                        return when (op) {
                            "<" -> Value.Num(if (a < b) 1 else 0)
                            ">" -> Value.Num(if (a > b) 1 else 0)
                            "<=" -> Value.Num(if (a <= b) 1 else 0)
                            else -> Value.Num(if (a >= b) 1 else 0)
                        }
                    }
                }
            }
            val bothInt = l is Value.Num && r is Value.Num
            if (bothInt) {
                val a = l.v; val b = r.v
                return when (op) {
                    // 教学模型的 int 是 32 位：溢出按 C 的补码回绕，课程里讲溢出才有真实例子
                    "+" -> Value.Num((a.toInt() + b.toInt()).toLong())
                    "-" -> Value.Num((a.toInt() - b.toInt()).toLong())
                    "*" -> Value.Num((a.toInt() * b.toInt()).toLong())
                    "/" -> if (b == 0L) throw CError("运行时错误", "整数除以零（第 $line 行）")
                        else if (a == Int.MIN_VALUE.toLong() && b == -1L) Value.Num(Int.MIN_VALUE.toLong())
                        else Value.Num((a.toInt() / b.toInt()).toLong())
                    "%" -> if (b == 0L) throw CError("运行时错误", "整数取模零（第 $line 行）") else Value.Num((a.toInt() % b.toInt()).toLong())
                    // 位运算按教学模型的 32 位 int 截断：1 << 31 变负数、~0 为 -1，
                    // 与教材里的 C 行为一致（值本身仍用 Long 承载）。
                    "<<" -> Value.Num((a.toInt() shl (b.toInt() and 31)).toLong())
                    ">>" -> Value.Num((a.toInt() shr (b.toInt() and 31)).toLong())
                    "&" -> Value.Num((a.toInt() and b.toInt()).toLong())
                    "|" -> Value.Num((a.toInt() or b.toInt()).toLong())
                    "^" -> Value.Num((a.toInt() xor b.toInt()).toLong())
                    "==" -> Value.Num(if (a == b) 1 else 0)
                    "!=" -> Value.Num(if (a != b) 1 else 0)
                    "<" -> Value.Num(if (a < b) 1 else 0)
                    ">" -> Value.Num(if (a > b) 1 else 0)
                    "<=" -> Value.Num(if (a <= b) 1 else 0)
                    ">=" -> Value.Num(if (a >= b) 1 else 0)
                    else -> throw CError("运行时错误", "不支持的运算符 $op（第 $line 行）")
                }
            }
            if (op in setOf("&", "|", "^", "<<", ">>", "~")) {
                throw CError("运行时错误", "位运算只支持整数（第 $line 行）")
            }
            val a = l.asDouble(); val b = r.asDouble()
            return when (op) {
                "+" -> Value.Real(a + b)
                "-" -> Value.Real(a - b)
                "*" -> Value.Real(a * b)
                "/" -> Value.Real(a / b)
                "%" -> Value.Real(a % b)
                "==" -> Value.Num(if (a == b) 1 else 0)
                "!=" -> Value.Num(if (a != b) 1 else 0)
                "<" -> Value.Num(if (a < b) 1 else 0)
                ">" -> Value.Num(if (a > b) 1 else 0)
                "<=" -> Value.Num(if (a <= b) 1 else 0)
                ">=" -> Value.Num(if (a >= b) 1 else 0)
                else -> throw CError("运行时错误", "不支持的运算符 $op（第 $line 行）")
            }
        }

        private fun valueEquals(l: Value, r: Value): Boolean {
            if (l is Value.Ptr || r is Value.Ptr) {
                val lp = l as? Value.Ptr
                val rp = r as? Value.Ptr
                if (lp != null && rp != null) {
                    if (lp.cells.isEmpty() && rp.cells.isEmpty()) return true
                    return lp.cells === rp.cells && lp.index == rp.index
                }
                val p = lp ?: rp ?: return false
                val other = if (lp != null) r else l
                return p.cells.isEmpty() && other.asLong() == 0L
            }
            return l.display() == r.display()
        }

        private fun unop(e: Expr.Un): Value = when (e.op) {
            "-" -> when (val v = eval(e.e)) {
                is Value.Real -> Value.Real(-v.v)
                else -> Value.Num(-v.asLong())
            }
            "+" -> eval(e.e)
            "!" -> Value.Num(if (eval(e.e).truthy()) 0 else 1)
            "~" -> Value.Num(eval(e.e).asLong().toInt().inv().toLong())
            else -> throw CError("运行时错误", "不支持的运算符 ${e.op}（第 ${e.line} 行）")
        }

        private fun callBuiltinOrUser(e: Expr.Call): Value {
            when (e.name) {
                "printf" -> {
                    if (e.args.isEmpty()) throw CError("运行时错误", "printf 需要格式串（第 ${e.line} 行）")
                    val fmt = eval(e.args[0])
                    val args = e.args.drop(1).map { eval(it) }
                    emit(formatPrintf(fmt.display(), args))
                    return Value.Num(0)
                }
                "puts" -> {
                    val s = e.args.firstOrNull()?.let { asCString(eval(it)) } ?: ""
                    emit(s + "\n")
                    return Value.Num(0)
                }
                "putchar" -> {
                    val c = eval(e.args[0]).asLong().toInt().toChar()
                    emit(c.toString())
                    return Value.Num(0)
                }
                "scanf" -> throw CError(
                    "不支持",
                    "本课程暂不支持 scanf（教学子集不做输入流解析）。请把输入直接写在代码里"
                )
                "assert" -> {
                    val cond = eval(e.args[0]).truthy()
                    if (!cond) {
                        val msg = e.args.getOrNull(1)?.let { eval(it).display() } ?: "断言未通过"
                        throw CError("TestFailed", msg)
                    }
                    return Value.Num(0)
                }
                // 判题靠它检查用户真的打印了什么。没有它，用户什么都不写也能过。
                "assertOutput" -> {
                    if (e.args.size < 2) {
                        throw CError("运行时错误", "assertOutput(期望输出, 提示) 需要两个参数")
                    }
                    val expected = eval(e.args[0]).display()
                    val actual = out.toString()
                    if (!actual.contains(expected)) {
                        val msg = eval(e.args[1]).display()
                        throw CError(
                            "TestFailed",
                            "$msg（期望输出包含「$expected」，实际为「${actual.take(80)}」）"
                        )
                    }
                    return Value.Num(0)
                }
                "strlen" -> return Value.Num(asCString(eval(e.args[0])).length.toLong())
                "strcpy" -> {
                    val dest = eval(e.args[0])
                    val src = asCString(eval(e.args[1]))
                    writeCString(dest, src, e.line)
                    return dest
                }
                "strcat" -> {
                    val dest = eval(e.args[0])
                    val src = asCString(eval(e.args[1]))
                    val base = cStringLength(dest)
                    writeCStringAt(dest, base, src, e.line)
                    return dest
                }
                "strcmp" -> {
                    val a = asCString(eval(e.args[0]))
                    val b = asCString(eval(e.args[1]))
                    return Value.Num(a.compareTo(b).let { if (it < 0) -1L else if (it > 0) 1L else 0L })
                }
                "memset" -> {
                    val dest = eval(e.args[0])
                    val v = eval(e.args[1]).asLong()
                    val n = eval(e.args[2]).asLong().toInt()
                    fillMemory(dest, v, n, e.line)
                    return dest
                }
                "sprintf" -> {
                    val dest = eval(e.args[0])
                    val fmt = eval(e.args[1]).display()
                    val args = e.args.drop(2).map { eval(it) }
                    val text = formatPrintf(fmt, args)
                    writeCString(dest, text, e.line)
                    return Value.Num(text.length.toLong())
                }
                "atoi" -> {
                    val text = asCString(eval(e.args[0])).trim()
                    val m = Regex("""^[+-]?\d+""").find(text)
                    return Value.Num(m?.value?.toLongOrNull() ?: 0L)
                }
                "toupper" -> {
                    val c = eval(e.args[0]).asLong().toInt()
                    return Value.Num(if (c in 'a'.code..'z'.code) (c - 32).toLong() else c.toLong())
                }
                "tolower" -> {
                    val c = eval(e.args[0]).asLong().toInt()
                    return Value.Num(if (c in 'A'.code..'Z'.code) (c + 32).toLong() else c.toLong())
                }
                "isalpha" -> {
                    val c = eval(e.args[0]).asLong().toInt().toChar()
                    return Value.Num(if (c.isLetter()) 1 else 0)
                }
                "isdigit" -> {
                    val c = eval(e.args[0]).asLong().toInt().toChar()
                    return Value.Num(if (c.isDigit()) 1 else 0)
                }
                "abs" -> {
                    val v = eval(e.args[0])
                    return if (v is Value.Real) Value.Real(kotlin.math.abs(v.v))
                    else Value.Num(kotlin.math.abs(v.asLong()))
                }
                "fabs" -> return Value.Real(kotlin.math.abs(eval(e.args[0]).asDouble()))
                "floor" -> return Value.Real(kotlin.math.floor(eval(e.args[0]).asDouble()))
                "ceil" -> return Value.Real(kotlin.math.ceil(eval(e.args[0]).asDouble()))
                "sqrt" -> return Value.Real(kotlin.math.sqrt(eval(e.args[0]).asDouble()))
                "pow" -> return Value.Real(
                    Math.pow(eval(e.args[0]).asDouble(), eval(e.args[1]).asDouble())
                )
            }
            val fn = funcs[e.name] ?: throw CError(
                "运行时错误", "未定义的函数 '${e.name}'（第 ${e.line} 行）"
            )
            val args = e.args.map { eval(it) }
            return invoke(fn, args)
        }

        /** 把值转成 C 字符串视图（char 数组读到 '\0'，指针从当前位置读）。 */
        private fun asCString(v: Value): String = when (v) {
            is Value.Str -> v.v
            is Value.Arr -> {
                val sb = StringBuilder()
                for (cell in v.cells) {
                    val n = cell.value.asLong()
                    if (n == 0L) break
                    sb.append(n.toInt().toChar())
                }
                sb.toString()
            }
            is Value.Ptr -> {
                val sb = StringBuilder()
                var i = v.index
                while (i < v.cells.size) {
                    val n = v.cells[i].value.asLong()
                    if (n == 0L) break
                    sb.append(n.toInt().toChar())
                    i++
                }
                sb.toString()
            }
            else -> v.display()
        }

        private fun cStringLength(dest: Value): Int = when (dest) {
            is Value.Arr -> {
                var n = 0
                while (n < dest.cells.size && dest.cells[n].value.asLong() != 0L) n++
                n
            }
            is Value.Ptr -> {
                var n = 0
                var i = dest.index
                while (i < dest.cells.size && dest.cells[i].value.asLong() != 0L) { n++; i++ }
                n
            }
            else -> 0
        }

        private fun writeCString(dest: Value, text: String, line: Int) = writeCStringAt(dest, 0, text, line)

        private fun writeCStringAt(dest: Value, offset: Int, text: String, line: Int) {
            val remaining: Int = when (dest) {
                is Value.Arr -> dest.cells.size - offset
                is Value.Ptr -> dest.cells.size - dest.index - offset
                is Value.Str -> throw CError("运行时错误", "不能修改字符串字面量（第 $line 行）")
                else -> throw CError("运行时错误", "字符串目标不是字符数组或指针（第 $line 行）")
            }
            if (text.length + 1 > remaining) {
                throw CError("运行时错误", "字符串超出目标缓冲区（需要 ${text.length + 1} 字节，剩余 $remaining，第 $line 行）")
            }
            val base: Int = when (dest) {
                is Value.Arr -> offset
                is Value.Ptr -> dest.index + offset
                else -> 0
            }
            val cells = when (dest) {
                is Value.Arr -> dest.cells
                is Value.Ptr -> dest.cells
                else -> emptyList()
            }
            for (i in text.indices) {
                if (cells[base + i].readonly) throw CError("运行时错误", "不能修改字符串字面量（第 $line 行）")
                cells[base + i].value = Value.Num(text[i].code.toLong())
            }
            cells[base + text.length].value = Value.Num(0)
        }

        /**
         * memset 按字节算，不是按元素。教学模型里 int 是 4 字节，
         * 所以 memset(a, 0, sizeof(a)) 对 int a[5] 正好清 5 个元素。
         * 这是课程里最常见的写法，按元素数实现会误报越界。
         */
        private fun fillMemory(dest: Value, value: Long, n: Int, line: Int) {
            val cells = when (dest) {
                is Value.Arr -> dest.cells
                is Value.Ptr -> dest.cells
                else -> throw CError("运行时错误", "memset 目标不是数组或指针（第 $line 行）")
            }
            val base = if (dest is Value.Ptr) dest.index else 0
            if (cells.isEmpty() || base >= cells.size) throw CError("运行时错误", "memset 越界（第 $line 行）")
            val elemSize = sizeOf(cells[base].type)
            val totalBytes = (cells.size - base) * elemSize
            if (n < 0 || n > totalBytes) {
                throw CError("运行时错误", "memset 越界（第 $line 行）")
            }
            val count = n / elemSize
            for (i in 0 until count) {
                if (cells[base + i].readonly) throw CError("运行时错误", "不能修改字符串字面量（第 $line 行）")
                cells[base + i].value = Value.Num(value)
            }
        }

        /** %u 按 32 位无符号解释（C 的 int 在这个教学模型里是 4 字节）。 */
        private fun unsignedText(v: Long): String =
            (v.toInt().toLong() and 0xFFFF_FFFFL).toString()

        /** %x / %o 同样按无符号 32 位解释，负数不会输出成 16 位补码。 */
        private fun radixText(v: Long, radix: Int): String =
            java.lang.Long.toString(v.toInt().toLong() and 0xFFFF_FFFFL, radix)

        /** %f 的 C 风格输出：nan/inf 小写，其余按精度补零。 */
        private fun formatFixed(d: Double, prec: Int): String = when {
            d.isNaN() -> "nan"
            d.isInfinite() -> if (d > 0) "inf" else "-inf"
            else -> String.format(java.util.Locale.US, "%.${prec}f", d)
        }

        /** printf 格式串：支持 %d %i %ld %f %.Nf %c %s %% %x %o %p，含宽度/左对齐/补零 */
        private fun formatPrintf(fmt: String, args: List<Value>): String {
            val sb = StringBuilder()
            var ai = 0
            var i = 0
            while (i < fmt.length) {
                val c = fmt[i]
                if (c != '%') { sb.append(c); i++; continue }
                if (i + 1 < fmt.length && fmt[i + 1] == '%') { sb.append('%'); i += 2; continue }
                var j = i + 1
                val spec = StringBuilder()
                while (j < fmt.length && (fmt[j].isDigit() || fmt[j] == '.' || fmt[j] == '-' ||
                        fmt[j] == '+' || fmt[j] == ' ' || fmt[j] == 'l' || fmt[j] == 'h' ||
                        fmt[j] == 'z')
                ) { spec.append(fmt[j]); j++ }
                if (j >= fmt.length) { sb.append('%'); i++; continue }
                val conv = fmt[j]
                val arg = args.getOrNull(ai++) ?: Value.Num(0)
                // 宽度是小数点前的数字（'0' 前缀表示补零，'-' 表示左对齐）
                val width = spec.toString().substringBefore('.').filter { it.isDigit() }.toIntOrNull()
                val leftAlign = '-' in spec
                val zeroPad = spec.isNotEmpty() && spec[0] == '0' && !leftAlign
                fun padded(text: String): String = when {
                    width == null || text.length >= width -> text
                    leftAlign -> text.padEnd(width)
                    zeroPad -> text.padStart(width, '0')
                    else -> text.padStart(width)
                }
                when (conv) {
                    // %d 按 32 位有符号 int 输出，避免内部 Long 泄漏出不存在的范围
                    'd', 'i' -> sb.append(padded(
                        if ('l' in spec) arg.asLong().toString() else arg.asLong().toInt().toString()
                    ))
                    'u' -> sb.append(padded(unsignedText(arg.asLong())))
                    'o' -> sb.append(padded(radixText(arg.asLong(), 8)))
                    'x' -> sb.append(padded(radixText(arg.asLong(), 16)))
                    'X' -> sb.append(padded(radixText(arg.asLong(), 16).uppercase()))
                    'f', 'F' -> {
                        val prec = Regex("""\.(\d+)""").find(spec.toString())
                            ?.groupValues?.get(1)?.toIntOrNull() ?: 6
                        sb.append(padded(formatFixed(arg.asDouble(), prec)))
                    }
                    'g' -> sb.append(formatDouble(arg.asDouble()))
                    'c' -> sb.append(padded(arg.asLong().toInt().toChar().toString()))
                    's' -> sb.append(padded(asCString(arg)))
                    'p' -> sb.append(arg.display())
                    else -> sb.append('%').append(spec).append(conv)
                }
                i = j + 1
            }
            return sb.toString()
        }

        // ---- 初始化与类型工具 ----

        private fun initValue(init: Expr, ty: CType): Value = when (ty) {
            is CType.TArray -> initArray(ty, init)
            is CType.TStruct -> initStruct(ty.def, init)
            else -> {
                if (init is Expr.InitList) throw CError("语法错误", "标量不能用 { } 初始化")
                coerce(eval(init), ty)
            }
        }

        private fun initArray(t: CType.TArray, init: Expr): Value {
            val dims = t.dims.toMutableList()
            if (dims.isNotEmpty() && dims[0] < 0) {
                dims[0] = when (init) {
                    is Expr.StrLit -> init.v.length + 1
                    is Expr.InitList -> init.items.size
                    else -> throw CError("语法错误", "无法推断数组长度，请显式写出长度")
                }
            }
            val total = checkedArraySize(dims)
            val cells = MutableList(total) { Cell(defaultValue(t.of), t.of) }
            fillArray(cells, 0, dims, t.of, init)
            return Value.Arr(cells, t.of, dims)
        }

        private fun fillArray(cells: MutableList<Cell>, offset: Int, dims: List<Int>, elem: CType, init: Expr) {
            if (dims.size == 1) {
                when (init) {
                    is Expr.InitList -> init.items.forEachIndexed { i, item ->
                        val at = offset + i
                        if (at < cells.size) cells[at].value = initValue(item, elem)
                    }
                    is Expr.StrLit -> {
                        if (elem !== CType.TChar) {
                            throw CError("语法错误", "只有 char 数组可以用字符串初始化")
                        }
                        if (init.v.length + 1 > dims[0]) {
                            throw CError("运行时错误", "字符串初始化超出数组长度（需要 ${init.v.length + 1}，长度 ${dims[0]}）")
                        }
                        init.v.forEachIndexed { i, ch -> cells[offset + i].value = Value.Num(ch.code.toLong()) }
                        cells[offset + init.v.length].value = Value.Num(0)
                    }
                    else -> throw CError("语法错误", "数组初始化需要 { } 或字符串字面量")
                }
            } else {
                if (init !is Expr.InitList) throw CError("语法错误", "多维数组初始化需要嵌套 { }")
                val rowSize = dims.drop(1).fold(1) { a, b -> a * b }
                val flat = init.items.isNotEmpty() && init.items.none { it is Expr.InitList || it is Expr.StrLit }
                when {
                    // 扁平的 {1,2,3,4} 也是合法 C：按行优先顺序填
                    flat -> init.items.forEachIndexed { i, item ->
                        if (offset + i < cells.size) cells[offset + i].value = initValue(item, elem)
                    }
                    // char 二维数组可以直接用字符串行初始化：{"ab","cd"}
                    init.items.all { it is Expr.StrLit } && elem === CType.TChar && dims.size == 2 ->
                        init.items.forEachIndexed { i, row ->
                            if (i < dims[0]) fillArray(cells, offset + i * rowSize, dims.drop(1), elem, row)
                        }
                    else -> init.items.forEachIndexed { i, row ->
                        if (i < dims[0]) fillArray(cells, offset + i * rowSize, dims.drop(1), elem, row)
                    }
                }
            }
        }

        private fun initStruct(def: StructDef, init: Expr): Value.Struct {
            val obj = newStruct(def)
            if (init is Expr.InitList) {
                def.fields.forEachIndexed { i, (name, ft) ->
                    val item = init.items.getOrNull(i) ?: return@forEachIndexed
                    val v = initValue(item, ft)
                    // 省略长度的数组成员（极少见）在初始化后才确定长度，替换 cell 类型
                    if (ft is CType.TArray && ft.dims.any { it < 0 } && v is Value.Arr) {
                        obj.fields[name] = Cell(v, CType.TArray(ft.of, v.dims))
                    } else {
                        val cell = obj.fields[name] ?: return@forEachIndexed
                        cell.value = v
                    }
                }
            } else {
                val v = eval(init)
                if (v !is Value.Struct) {
                    throw CError("运行时错误", "结构体初始化需要 { } 或同类型结构体")
                }
                return copyStruct(v)
            }
            return obj
        }

        private fun typeOf(e: Expr): CType = when (e) {
            is Expr.Var -> scopes.first().lookup(e.name)?.type
                ?: throw CError("运行时错误", "未声明的变量 '${e.name}'（第 ${e.line} 行）")
            is Expr.NumLit -> CType.TInt
            is Expr.RealLit -> CType.TDouble
            is Expr.StrLit -> CType.TArray(CType.TChar, listOf(e.v.length + 1))
            is Expr.Index -> {
                when (val bt = typeOf(e.arr)) {
                    is CType.TArray -> if (bt.dims.size > 1) CType.TArray(bt.of, bt.dims.drop(1)) else bt.of
                    is CType.TPtr -> bt.to
                    else -> CType.TInt
                }
            }
            is Expr.Deref -> (typeOf(e.e) as? CType.TPtr)?.to ?: CType.TInt
            is Expr.Addr -> CType.TPtr(typeOf(e.e))
            is Expr.Assign -> typeOf(e.target)
            is Expr.PreInc, is Expr.PostInc -> CType.TInt
            is Expr.Bin -> if (typeOf(e.l) === CType.TDouble || typeOf(e.r) === CType.TDouble) CType.TDouble else CType.TInt
            is Expr.Un -> typeOf(e.e)
            is Expr.Ternary -> typeOf(e.a)
            is Expr.Call -> when (e.name) {
                "fabs", "floor", "ceil", "sqrt", "pow" -> CType.TDouble
                "printf", "puts", "putchar", "scanf", "strlen", "strcmp", "atoi",
                "toupper", "tolower", "isalpha", "isdigit", "abs", "sprintf",
                "strcpy", "strcat", "memset", "assert", "assertOutput" -> CType.TInt
                else -> funcs[e.name]?.returnType ?: CType.TInt
            }
            is Expr.Member -> {
                val bt = typeOf(e.obj)
                if (bt is CType.TStruct) {
                    bt.def.fields.firstOrNull { it.first == e.name }?.second ?: CType.TInt
                } else CType.TInt
            }
            is Expr.Arrow -> {
                val bt = typeOf(e.ptr)
                val st = (bt as? CType.TPtr)?.to
                if (st is CType.TStruct) {
                    st.def.fields.firstOrNull { it.first == e.name }?.second ?: CType.TInt
                } else CType.TInt
            }
            else -> CType.TInt
        }

        /** sizeof 教学模型：int 4、double 8、char 1、指针 8，结构体按成员和向上对齐到 8。 */
        private fun sizeOf(t: CType): Int = when (t) {
            CType.TInt -> 4
            CType.TChar -> 1
            CType.TDouble -> 8
            CType.TVoid -> 1
            is CType.TPtr -> 8
            is CType.TArray -> t.dims.fold(sizeOf(t.of)) { a, b -> a * b }
            is CType.TStruct -> {
                var sum = 0
                for ((_, ft) in t.def.fields) sum += sizeOf(ft)
                if (sum == 0) 0 else ((sum + 7) / 8) * 8
            }
        }
    }

    // 对外接口

    data class Result(
        val ok: Boolean,
        val stdout: String,
        val errorType: String?,
        val errorMessage: String?,
        val variables: List<Triple<String, String, String>>
    )

    /** 执行一段 C 源码（必须有 main）。 */
    fun run(
        source: String,
        stdinLines: List<String> = emptyList(),
        entry: String = "main",
        runMainFirst: Boolean = false
    ): Result {
        val out = StringBuilder()
        return try {
            // 先拦截明确不支持的语法，给出「不支持」而不是晦涩的语法错误
            rejectUnsupported(source)
            val tokens = Lexer(source).tokenize()
            val program = Parser(tokens).parseProgram()
            val interp = Interpreter(program.funcs, out, ArrayDeque(stdinLines), program.globals)
            if (runMainFirst) interp.callMainThenTest(entry) else interp.callEntry(entry)
            Result(true, out.toString(), null, null, emptyList())
        } catch (e: CError) {
            Result(false, out.toString(), e.type, e.message, emptyList())
        } catch (e: StackOverflowError) {
            Result(false, out.toString(), "StackOverflow", "递归层数过深", emptyList())
        } catch (e: BreakSignal) {
            Result(false, out.toString(), "语法错误", "break 不在循环或 switch 内", emptyList())
        } catch (e: ContinueSignal) {
            Result(false, out.toString(), "语法错误", "continue 不在循环内", emptyList())
        } catch (e: ReturnSignal) {
            Result(false, out.toString(), "语法错误", "return 不在函数内", emptyList())
        } catch (e: Throwable) {
            Result(false, out.toString(), "InternalError", "${e.javaClass.simpleName}: ${e.message}", emptyList())
        }
    }

    /**
     * 判题：先跑用户代码（必须有 main），再依次执行每条测试。
     *
     * 测试是独立片段，但可以调用用户函数，所以把两段拼成一个翻译单元再解析。
     * 这样符合 C 先定义后使用的规则，测试也能直接断言用户函数。
     */
    fun check(source: String, tests: List<String>): Result {
        val failures = mutableListOf<String>()
        var lastOut = ""
        for ((index, test) in tests.withIndex()) {
            // 把测试里的 main 改名为 __test_main：用户代码通常自己也有 main，
            // 同名会在同一翻译单元里冲突。改名后两者共存，入口指向测试那个。
            val renamed = test.replace(Regex("""\bmain\s*\(\s*(?:void\s*)?\)"""), "__test_main()")
            val combined = "$source\n$renamed"
            // 先跑用户 main（真实输出），再跑测试入口；两者共用输出缓冲
            val r = run(combined, entry = "__test_main", runMainFirst = true)
            lastOut = r.stdout
            if (!r.ok) {
                failures.add("用例 ${index + 1}：${r.errorMessage}")
                if (r.errorType == "TestFailed") continue
                // 语法/运行时错误直接返回，后续用例没有意义
                break
            }
        }
        return if (failures.isEmpty()) {
            Result(true, lastOut, null, null, emptyList())
        } else {
            Result(
                false, lastOut, "TestFailed",
                "共 ${tests.size} 个用例，${failures.size} 个未通过；首个失败：${failures.first()}",
                emptyList()
            )
        }
    }

    // 不支持的语法早点拦住，报清楚是哪一种，别让解析器吐一句「意外标记」。
    private fun rejectUnsupported(source: String) {
        val scan = stripCommentsAndLiterals(source)
        for ((pattern, label) in UNSUPPORTED_PATTERNS) {
            val hit = pattern.find(scan) ?: continue
            val line = source.substring(0, hit.range.first).count { it == '\n' } + 1
            throw CError("不支持", "暂不支持 $label（第 $line 行）。本课程是 C 教学子集，不是完整 gcc。")
        }
    }

    // 扫描不支持语法前，先把注释和字符串内容抹成空格，换行留着。
    // 不然注释里写一句「union 不在范围内」也会被当成代码。
    private fun stripCommentsAndLiterals(src: String): String {
        val sb = StringBuilder(src)
        fun blank(at: Int) { if (src[at] != '\n') sb.setCharAt(at, ' ') }
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c == '/' && i + 1 < src.length && src[i + 1] == '/' -> {
                    while (i < src.length && src[i] != '\n') { blank(i); i++ }
                }
                c == '/' && i + 1 < src.length && src[i + 1] == '*' -> {
                    blank(i); blank(i + 1); i += 2
                    while (i < src.length && !(src[i] == '*' && i + 1 < src.length && src[i + 1] == '/')) {
                        blank(i); i++
                    }
                    if (i + 1 < src.length) { blank(i); blank(i + 1); i += 2 }
                }
                c == '"' || c == '\'' -> {
                    val quote = c
                    blank(i); i++
                    while (i < src.length && src[i] != quote) {
                        if (src[i] == '\\' && i + 1 < src.length) { blank(i); blank(i + 1); i += 2 }
                        else { blank(i); i++ }
                    }
                    if (i < src.length) { blank(i); i++ }
                }
                else -> i++
            }
        }
        return sb.toString()
    }

    // REPL 用的会话。run 每次都是新程序、必须有 main；
    // 会话把每次输入接到当前程序后面，函数和全局变量都留着。
    class Session {
        private val out = StringBuilder()
        private var funcs = linkedMapOf<String, FuncDef>()
        private val typedefs = linkedMapOf<String, CType>()
        private val structDefs = linkedMapOf<String, StructDef>()
        private val scope = Scope(null)

        /** 推入一行/一段代码；返回 (是否继续等待, 输出文本) */
        fun push(source: String): Pair<Boolean, String> {
            out.setLength(0)
            val trimmed = source.trim()
            if (trimmed.isEmpty()) return false to ""

            // 花括号没闭合就等下一行。
            val open = trimmed.count { it == '{' } - trimmed.count { it == '}' }
            if (open > 0) return true to ""

            return try {
                rejectUnsupported(trimmed)
                val tokens = Lexer(trimmed).tokenize()
                val parser = Parser(tokens, typedefs, structDefs)
                val parsed = parser.parseProgramInSession()
                funcs.putAll(parsed.funcs)
                if (parsed.statements.isNotEmpty()) {
                    val interp = Interpreter(funcs, out, ArrayDeque())
                    interp.adoptScope(scope)
                    interp.runStatements(parsed.statements)
                }
                false to out.toString()
            } catch (e: CError) {
                false to "${e.type}: ${e.message}"
            } catch (e: BreakSignal) {
                false to "语法错误: break 不在循环或 switch 内"
            } catch (e: ContinueSignal) {
                false to "语法错误: continue 不在循环内"
            } catch (e: ReturnSignal) {
                false to "语法错误: return 不在函数内"
            } catch (e: Throwable) {
                false to "InternalError: ${e.message}"
            }
        }

        /** 当前会话的变量快照 */
        fun variables(): List<Triple<String, String, String>> =
            scope.vars.map { (k, cell) -> Triple(k, cell.type.display(), cell.value.display()) }

        fun reset() {
            funcs = linkedMapOf()
            typedefs.clear()
            structDefs.clear()
            scope.vars.clear()
            out.setLength(0)
        }
    }

    /** 单次运行的输出上限，防止死循环打印耗尽内存。 */
    private const val MAX_OUTPUT_CHARS = 1_000_000

    /** 单个数组的元素数上限（教学场景远用不到），超过直接报错而不是 OOM。 */
    private const val MAX_ARRAY_ELEMENTS = 100_000

    private val KEYWORDS = setOf(
        "int", "double", "char", "void", "long", "float", "short", "unsigned", "signed",
        "if", "else", "while", "for", "return", "break", "continue", "do", "switch",
        "case", "default", "const", "static", "struct", "typedef", "sizeof", "goto",
        "extern", "register", "volatile"
    )

    private val TYPE_KEYWORDS = setOf(
        "int", "double", "float", "char", "void", "long", "short", "unsigned", "signed",
        "const", "static", "struct", "extern", "register", "volatile"
    )

    private val ASSIGN_OPS = listOf(
        "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", "="
    )

    private val PUNCT_CHARS = setOf('(', ')', '{', '}', '[', ']', ',', ';')

    private val TWO_CHAR_OPS = setOf(
        "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=",
        "&=", "|=", "^=", "<<", ">>", "->"
    )

    private val THREE_CHAR_OPS = setOf("<<=", ">>=")

    /**
     * 明确不支持的语法前置检查：在解析前拦下，给出「不支持」而不是晦涩的
     * 语法错误，让学习者知道这不是 bug 而是课程范围。
     */
    private val UNSUPPORTED_PATTERNS = listOf(
        Regex("""\bgoto\b""") to "goto 语句",
        Regex("""#\s*include""") to "预处理指令 #include（课程内置常用函数，无需引入头文件）",
        Regex("""#\s*define""") to "预处理宏 #define",
        Regex("""(?m)^\s*#""") to "预处理指令",
        Regex("""\b(?:int|char|double|float|void|long|short|unsigned|signed|struct\s+\w+)\s*(?:const\s*)?\*\s*\*""") to "二级指针",
        Regex("""\(\s*\*\s*[A-Za-z_]\w*\s*\)\s*\(""") to "函数指针",
        Regex("""\{\s*\.\w+\s*=""") to "指定初始化器 .field =（请按成员顺序写 {1, 2}）",
        Regex("""\(\s*[^)]*\.\.\.[^)]*\)""") to "可变参数函数（...）",
        Regex("""\bunion\b""") to "union 联合体",
        Regex("""\benum\b""") to "enum 枚举"
    )
}

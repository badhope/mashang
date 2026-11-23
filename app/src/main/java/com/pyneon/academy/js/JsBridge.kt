package com.pyneon.academy.js

import android.content.Context
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRuntime
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.lang.VarInfo
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context as JsContext
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EcmaError
import org.mozilla.javascript.Function
import org.mozilla.javascript.JavaScriptException
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.WrappedException

/**
 * JavaScript 运行时，用 Mozilla Rhino 解释执行。
 *
 * 结果字段和 Python 侧一样：ok/stdout/stderr/error/duration_ms/variables/passed，
 * 上层不用管底下是哪门语言。
 *
 * 选 Rhino 是因为纯 Java，不用 NDK，也不用按 ABI 分包，APK 只多一点点。
 * optimizationLevel 必须设 -1，它编译出来的是 JVM 字节码，ART 跑不了。
 * 语言版本开 ES6，课程还是按 ES5 写，结果稳定。以后要箭头函数和 async 再换后端。
 * 超时靠指令计数中断，Python 那边用的是 settrace。Context 不能多线程共用，求值全串行。
 *
 * runner.py 判题时用例共享状态，但测试不能改快照。Rhino 没有 scope.copy，
 * 所以先拍快照再跑测试，测试和用户代码用同一个作用域。
 */
object JsBridge : LanguageRuntime {

    override val id: String = LanguageIds.JAVASCRIPT
    override val displayName: String = "JavaScript"
    override val fileExtension: String = "js"
    override val grammarScope: String = "source.js"

    override val version: String = "Rhino 1.7.15 (ES5/部分 ES6)"
    override fun isAvailable(): Boolean = true
    override fun ensureStarted(context: Context) = Unit

    // 限流参数，和 runner.py 保持一致。
    private const val DEFAULT_TIMEOUT = 8.0
    private const val TEST_TIMEOUT = 2.0
    private const val MAX_VARS = 40
    private const val MAX_REPR = 200
    private const val MAX_OUTPUT = 1_048_576

    private const val INPUT_EXHAUSTED_MSG =
        "运行台不支持交互式输入：请把测试数据直接写在代码里，或到「神经接口」终端中使用 input()"

    // 指令计数超了就中断。
    private class JsTimeoutException : RuntimeException("js sandbox timeout")

    private class InputExhaustedException(msg: String) : RuntimeException(msg)

    @Volatile
    private var deadlineNanos: Long = Long.MAX_VALUE

    private val factory = object : ContextFactory() {
        override fun makeContext(): JsContext {
            val cx = super.makeContext()
            cx.optimizationLevel = -1
            cx.languageVersion = JsContext.VERSION_ES6
            cx.instructionObserverThreshold = 2000
            // Rhino 默认把 Java 包暴露给脚本，Packages.java.lang.Runtime 能直接 exec。
            // 用户能随便写代码，这里必须全部拒掉，只留 JS 标准库。
            // 以前验证过，不拦的话真能跑系统命令。
            cx.setClassShutter(ClassShutter { _ -> false })
            return cx
        }

        override fun observeInstructionCount(cx: JsContext, instructionCount: Int) {
            if (System.nanoTime() > deadlineNanos) throw JsTimeoutException()
        }
    }

    // Context 非线程安全：所有求值串行化
    private val lock = Any()

    // 输出缓冲。判题时测试输出走单独的槽。
    // 边写边限量。只在结束时截断挡不住内存，死循环 print 在超时前就能把堆撑爆，
    // 所以超了直接抛异常停脚本。
    private class BoundedSink(private val hardLimit: Int = MAX_OUTPUT * 4) {
        private val sb = StringBuilder()
        var length: Int = 0
            private set

        fun append(s: String): BoundedSink {
            length += s.length
            if (length > hardLimit) throw JsTimeoutException()
            sb.append(s)
            return this
        }

        fun append(c: Char): BoundedSink = append(c.toString())
        // 取某段之后的内容，REPL 只显示这次新增的输出。
        fun from(mark: Int): String = if (mark >= sb.length) "" else sb.substring(mark)
        override fun toString(): String = sb.toString()
    }

    private class OutputSink {
        val main: BoundedSink = BoundedSink()
        val err: BoundedSink = BoundedSink()
        var test: BoundedSink? = null
    }

    private class InputQueue(private val lines: List<String>) {
        private var index = 0
        fun next(): String {
            if (index >= lines.size) throw InputExhaustedException(INPUT_EXHAUSTED_MSG)
            return lines[index++]
        }
    }

    // 对外接口=

    override fun runCode(
        code: String,
        stdinLines: List<String>,
        timeoutSec: Double
    ): RunResult = synchronized(lock) { runOnce(code, stdinLines, timeoutSec) }

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult = synchronized(lock) { checkInternal(code, tests, stdinLines) }

    override fun warmup(): Boolean = try {
        runCode("1 + 1", emptyList(), 6.0).ok
    } catch (_: Throwable) {
        false
    }

    // REPL 里括号配平才执行，不配平就等下一行；空行强制提交。

    private var replScope: Scriptable? = null
    private var replSink: OutputSink? = null
    private val replBuffer: StringBuilder = StringBuilder()

    override fun replStart() {
        synchronized(lock) {
            replScope = null
            replSink = null
            replBuffer.setLength(0)
        }
    }

    override fun replPush(line: String): Pair<Boolean, String> = synchronized(lock) {
        val sink = replSink ?: OutputSink().also { replSink = it }
        if (replScope == null) {
            val (scope, _) = initScope(sink)
            replScope = scope
        }
        val scope = replScope!!
        val mainOut = sink.main
        val mark = mainOut.length

        if (line.isNotBlank()) replBuffer.append(line).append('\n')
        val source = replBuffer.toString().trim()

        if (source.isEmpty()) return@synchronized false to mainOut.from(mark)
        if (!bracketsBalanced(source)) return@synchronized true to mainOut.from(mark)
        replBuffer.setLength(0)

        deadlineNanos = System.nanoTime() + (DEFAULT_TIMEOUT * 1e9).toLong()
        val cx = factory.enterContext()
        try {
            val result = cx.evaluateString(scope, source, "<repl>", 1, null)
            if (result !== Undefined.instance) {
                mainOut.append(render(cx, scope, result)).append('\n')
            }
        } catch (t: Throwable) {
            mainOut.append(formatError(t, DEFAULT_TIMEOUT))
        } finally {
            JsContext.exit()
            deadlineNanos = Long.MAX_VALUE
        }
        false to mainOut.from(mark)
    }

    // 内部实现

    // 建作用域、装宿主函数，顺手记下已有的变量名，后面做新增差集。
    private fun initScope(sink: OutputSink, stdin: List<String> = emptyList()): Pair<Scriptable, Set<String>> {
        val cx = factory.enterContext()
        try {
            val scope = cx.initStandardObjects()
            installHost(cx, scope, sink, InputQueue(stdin))
            val before = (scope.ids ?: emptyArray()).map { it.toString() }.toSet()
            return scope to before
        } finally {
            JsContext.exit()
        }
    }

    private fun runOnce(code: String, stdin: List<String>, timeoutSec: Double): RunResult {
        val startedAt = System.nanoTime()
        val sink = OutputSink()
        var error: Err? = null
        var variables: List<VarInfo> = emptyList()

        val cx = factory.enterContext()
        try {
            val (scope, before) = initScope(sink, stdin)

            deadlineNanos = System.nanoTime() + (timeoutSec * 1e9).toLong()
            try {
                cx.evaluateString(scope, code, "<pyneon>", 1, null)
            } catch (t: Throwable) {
                error = mapError(t, timeoutSec)
            } finally {
                deadlineNanos = Long.MAX_VALUE
            }
            if (error == null) variables = snapshot(cx, scope, before)
        } finally {
            JsContext.exit()
        }
        return RunResult(
            ok = error == null,
            stdout = truncate(sink.main.toString()),
            stderr = truncate(sink.err.toString()),
            errorType = error?.type,
            errorMessage = error?.message,
            traceback = error?.traceback,
            durationMs = (System.nanoTime() - startedAt) / 1_000_000,
            variables = variables,
            passed = null
        )
    }

    private fun checkInternal(code: String, tests: List<String>, stdin: List<String>): RunResult {
        val startedAt = System.nanoTime()
        val sink = OutputSink()

        val cx = factory.enterContext()
        try {
            val (scope, before) = initScope(sink, stdin)

            // 1) 用户代码
            var userError: Err? = null
            deadlineNanos = System.nanoTime() + (DEFAULT_TIMEOUT * 1e9).toLong()
            try {
                cx.evaluateString(scope, code, "<pyneon>", 1, null)
            } catch (t: Throwable) {
                userError = mapError(t, DEFAULT_TIMEOUT)
            } finally {
                deadlineNanos = Long.MAX_VALUE
            }

            if (userError != null) {
                return RunResult(
                    ok = false, passed = false,
                    stdout = truncate(sink.main.toString()),
                    stderr = truncate(sink.err.toString()),
                    errorType = userError.type, errorMessage = userError.message,
                    traceback = userError.traceback,
                    durationMs = (System.nanoTime() - startedAt) / 1_000_000,
                    variables = emptyList()
                )
            }

            // 2) 快照先于判题采集：Rhino 无 scope.copy，测试跑在同一作用域，
            //    先快照保证测试里的赋值不污染变量面板（等价 runner.py 语义）
            val variables = snapshot(cx, scope, before)

            // 3) 逐条判题：用例之间共享状态（与 runner.py 的有序脚本语义一致）
            val failures = mutableListOf<String>()
            val testOutputs = StringBuilder()
            tests.forEachIndexed { index, test ->
                sink.test = BoundedSink()
                deadlineNanos = System.nanoTime() + (TEST_TIMEOUT * 1e9).toLong()
                try {
                    cx.evaluateString(scope, test, "<neon-test-${index + 1}>", 1, null)
                } catch (t: JsTimeoutException) {
                    failures.add("测试执行超时")
                } catch (t: JavaScriptException) {
                    failures.add(t.value?.toString() ?: "断言未通过")
                } catch (t: Throwable) {
                    val e = unwrap(t)
                    failures.add("${typeNameOfThrowable(e)}: ${e.message}")
                } finally {
                    deadlineNanos = Long.MAX_VALUE
                    testOutputs.append(sink.test?.toString() ?: "")
                    sink.test = null
                }
            }

            val passed = failures.isEmpty()
            val message = if (passed) "" else
                "共 ${tests.size} 个用例，${failures.size} 个未通过；首个失败：${failures[0]}"
            return RunResult(
                ok = true, passed = passed,
                stdout = truncate(sink.main.toString()),
                stderr = truncate(sink.err.toString()),
                errorType = if (passed) null else "TestFailed",
                errorMessage = if (passed) null else message,
                traceback = null,
                durationMs = (System.nanoTime() - startedAt) / 1_000_000,
                variables = variables
            )
        } finally {
            JsContext.exit()
        }
    }

    // 注入 print、input、assert 和 console。

    private fun installHost(cx: JsContext, scope: Scriptable, sink: OutputSink, input: InputQueue) {
        ScriptableObject.putProperty(scope, "print", object : BaseFunction() {
            override fun call(
                cx: JsContext, scope: Scriptable, thisObj: Scriptable, args: Array<Any?>
            ): Any? {
                (sink.test ?: sink.main).append(joinArgs(args)).append('\n')
                return Undefined.instance
            }
        })

        ScriptableObject.putProperty(scope, "input", object : BaseFunction() {
            override fun call(
                cx: JsContext, scope: Scriptable, thisObj: Scriptable, args: Array<Any?>
            ): Any? {
                if (args.isNotEmpty()) {
                    val prompt = ContextToString(args[0])
                    (sink.test ?: sink.main).append(prompt)
                }
                return input.next()
            }
        })

        ScriptableObject.putProperty(scope, "assert", object : BaseFunction() {
            override fun call(
                cx: JsContext, scope: Scriptable, thisObj: Scriptable, args: Array<Any?>
            ): Any? {
                val cond = args.isNotEmpty() && JsContext.toBoolean(args[0])
                if (!cond) {
                    val msg = if (args.size > 1) ContextToString(args[1]) else "断言未通过"
                    throw JavaScriptException("AssertionError: $msg")
                }
                return Undefined.instance
            }
        })

        ScriptableObject.putProperty(scope, "__stderr", object : BaseFunction() {
            override fun call(
                cx: JsContext, scope: Scriptable, thisObj: Scriptable, args: Array<Any?>
            ): Any? {
                sink.err.append(joinArgs(args)).append('\n')
                return Undefined.instance
            }
        })

        cx.evaluateString(
            scope,
            """
            var console = {
              log:  function(){ print(Array.prototype.map.call(arguments, String).join(' ')); },
              info: function(){ print(Array.prototype.map.call(arguments, String).join(' ')); },
              warn: function(){ __stderr(Array.prototype.map.call(arguments, String).join(' ')); },
              error:function(){ __stderr(Array.prototype.map.call(arguments, String).join(' ')); }
            };
            """.trimIndent(),
            "<pyneon-console>", 1, null
        )
    }

    private fun joinArgs(args: Array<Any?>): String =
        args.joinToString(" ") { ContextToString(it) }

    // 错误转换

    private class Err(val type: String, val message: String, val traceback: String)

    private fun mapError(t: Throwable, timeoutSec: Double): Err = when {
        t is InputExhaustedException ->
            Err("InputError", INPUT_EXHAUSTED_MSG, "")
        t is JsTimeoutException ->
            Err("Timeout", "代码运行超过 %.1f 秒，已被强制中断（可能是死循环）".format(timeoutSec), "")
        t is WrappedException -> mapError(t.wrappedException ?: t, timeoutSec)
        t is EcmaError ->
            Err(t.name, t.message ?: "", t.scriptStackTrace ?: "")
        t is JavaScriptException ->
            Err("UserError", t.value?.toString() ?: "", "")
        else ->
            Err(typeNameOfThrowable(t), t.message ?: "", "")
    }

    private fun unwrap(t: Throwable): Throwable =
        (t as? WrappedException)?.wrappedException ?: t

    private fun typeNameOfThrowable(t: Throwable): String =
        t.javaClass.simpleName.ifEmpty { "Error" }

    private fun formatError(t: Throwable, timeoutSec: Double): String {
        val e = mapError(t, timeoutSec)
        // Rhino 的 message 自带「ReferenceError: ...」前缀，再拼一次类型就重复了。
        val msg = e.message.removePrefix("${e.type}: ").removePrefix("${e.type}:")
        val head = if (msg.isBlank()) e.type else "${e.type}: $msg"
        val trace = e.traceback.lines().take(4).joinToString("\n")
        return if (trace.isEmpty()) "$head\n" else "$head\n$trace\n"
    }

    // 变量快照，只列这次新增的，最新的放前面。

    private fun snapshot(cx: JsContext, scope: Scriptable, before: Set<String>): List<VarInfo> {
        val items = mutableListOf<VarInfo>()
        val ids = scope.ids ?: return emptyList()
        for (i in ids.indices.reversed()) {
            if (items.size >= MAX_VARS) break
            val name = ids[i]?.toString() ?: continue
            if (name in before) continue
            val value = scope.get(name, scope)
            if (value === Undefined.instance || value === Scriptable.NOT_FOUND) continue
            if (value is Function) continue
            items.add(VarInfo(name, typeNameOf(value), render(cx, scope, value)))
        }
        return items
    }

    private fun typeNameOf(value: Any?): String = when (value) {
        is Undefined -> "undefined"
        is String -> "string"
        is java.lang.Boolean -> "boolean"
        is Number -> "number"
        is Function -> "function"
        is org.mozilla.javascript.NativeJavaObject -> "java"
        else -> "object"
    }

    private fun render(cx: JsContext, scope: Scriptable, value: Any?): String {
        val text = try {
            when (value) {
                is String -> "\"" + value + "\""
                is Scriptable -> {
                    val json = NativeJSON.stringify(cx, scope, value, null, null)
                    if (json is String) json else ContextToString(value)
                }
                else -> ContextToString(value)
            }
        } catch (_: Throwable) {
            "<unrepresentable>"
        }
        return if (text.length > MAX_REPR) text.substring(0, MAX_REPR) + "…" else text
    }

    private fun ContextToString(value: Any?): String = JsContext.toString(value)

    // 小工具

    private fun truncate(text: String): String =
        if (text.length > MAX_OUTPUT) "[输出过长已截断]\n" + text.takeLast(MAX_OUTPUT) else text

    /** 统计 () [] {} 是否平衡；跳过字符串与行注释，避免字符串里的括号误判 */
    private fun bracketsBalanced(src: String): Boolean {
        var parens = 0; var braces = 0; var brackets = 0
        var i = 0
        var quote: Char? = null
        while (i < src.length) {
            val c = src[i]
            if (quote != null) {
                when {
                    c == '\\' -> i++
                    c == quote -> quote = null
                }
            } else {
                when (c) {
                    '\'', '"' -> quote = c
                    '/' -> if (i + 1 < src.length && src[i + 1] == '/') {
                        while (i < src.length && src[i] != '\n') i++
                    }
                    '(' -> parens++
                    ')' -> parens--
                    '{' -> braces++
                    '}' -> braces--
                    '[' -> brackets++
                    ']' -> brackets--
                }
            }
            i++
        }
        return parens <= 0 && braces <= 0 && brackets <= 0
    }
}

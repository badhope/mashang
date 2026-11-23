package com.pyneon.academy.ts

import android.content.Context
import com.pyneon.academy.js.JsBridge
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRuntime
import com.pyneon.academy.lang.RunResult

/**
 * TypeScript 运行时。先把类型擦掉，再交给 Rhino 跑，不引第二个 JS 引擎。
 *
 * 类型本来运行时就不存在，擦完和 JS 没什么两样。判题、变量快照、超时、
 * 输出截断和 REPL 都复用 JsBridge，同一个沙箱，不会出现 TS 能摸 Java 类。
 * 也不新增 native 依赖，APK 不会变大。
 *
 * 和 JS 的区别就三点：源码先转译，判题用例也转译，version 报 TS 教学子集。
 * 装饰器、namespace、条件类型这些不做，越界就报语法错误，不猜。
 */
object TsBridge : LanguageRuntime {

    override val id: String = LanguageIds.TYPESCRIPT
    override val displayName: String = LanguageIds.displayNameOf(LanguageIds.TYPESCRIPT)
    override val fileExtension: String = "ts"
    override val grammarScope: String = "source.ts"

    // 报教学子集，别让人以为内置了完整 tsc。
    override val version: String get() = "教学子集（类型擦除）"

    override fun isAvailable(): Boolean = true

    override fun ensureStarted(context: Context) = JsBridge.ensureStarted(context)

    override fun runCode(
        code: String,
        stdinLines: List<String>,
        timeoutSec: Double
    ): RunResult {
        val result = TypeScriptTranspiler.transpile(code)
        return JsBridge.runCode(result.code, stdinLines, timeoutSec)
    }

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult {
        val js = TypeScriptTranspiler.transpile(code)
        // 判题用例也可能带类型，一样擦一遍。
        val jsTests = tests.map { TypeScriptTranspiler.transpile(it).code }
        return JsBridge.checkExercise(js.code, jsTests, stdinLines)
    }

    override fun warmup(): Boolean = JsBridge.warmup()

    override fun replStart() = JsBridge.replStart()

    // REPL 每行先转译再交给 JS。多行缓冲 JsBridge 管，这里不重复。
    override fun replPush(line: String): Pair<Boolean, String> =
        JsBridge.replPush(TypeScriptTranspiler.transpile(line).code)
}

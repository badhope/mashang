package com.pyneon.academy.c

import android.content.Context
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRuntime
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.lang.VarInfo

/**
 * C 运行时，用 Kotlin 写的教学子集解释器。
 *
 * Android 离线装 clang 加头文件和链接器要两三百 MB，还链不上 bionic，
 * 所以这里直接解释执行。gcc 能算的跟着算，不支持的明确报错。
 *
 * Java、C++、Go、Rust 在 PLANNED 里显示暂不支持，C 是真的能跑：
 * 写 printf 有输出，能判题，能改错。
 */
object CBridge : LanguageRuntime {

    override val id: String = LanguageIds.C
    override val displayName: String = LanguageIds.displayNameOf(LanguageIds.C)
    override val fileExtension: String = "c"
    override val grammarScope: String = "source.c"

    // 标清楚是子集解释器，别让人以为是 gcc。
    override val version: String get() = "教学子集（解释执行）"

    override fun isAvailable(): Boolean = true

    override fun ensureStarted(context: Context) = Unit

    override fun runCode(
        code: String,
        stdinLines: List<String>,
        timeoutSec: Double
    ): RunResult {
        val started = System.nanoTime()
        val r = CInterpreter.run(code, stdinLines)
        return toRunResult(r, started)
    }

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult {
        val started = System.nanoTime()
        val r = CInterpreter.check(code, tests)
        val base = toRunResult(r, started)
        return base.copy(passed = r.ok)
    }

    override fun warmup(): Boolean = true

    // C 的交互会话。以前每行都当完整程序跑，变量不跨行，
    // 写个 printf 还要求有 main。现在用有状态的 Session：
    // 函数和全局变量都留着，花括号没闭合就等下一行。
    @Volatile
    private var session: CInterpreter.Session? = null

    private fun sessionLocked(): CInterpreter.Session =
        session ?: CInterpreter.Session().also { session = it }

    override fun replStart() {
        session = CInterpreter.Session()
    }

    override fun replPush(line: String): Pair<Boolean, String> =
        synchronized(this) { sessionLocked().push(line) }

    private fun toRunResult(r: CInterpreter.Result, startedAt: Long): RunResult = RunResult(
        ok = r.ok,
        stdout = r.stdout,
        stderr = "",
        errorType = r.errorType,
        errorMessage = r.errorMessage,
        traceback = null,
        durationMs = (System.nanoTime() - startedAt) / 1_000_000,
        variables = r.variables.map { VarInfo(it.first, it.second, it.third) },
        passed = null
    )
}

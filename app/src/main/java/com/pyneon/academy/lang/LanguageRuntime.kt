package com.pyneon.academy.lang

import android.content.Context

object LanguageIds {
    const val PYTHON = "python"
    const val JAVASCRIPT = "javascript"
    const val TYPESCRIPT = "typescript"
    const val LUA = "lua"
    const val SQL = "sql"
    const val C = "c"

    const val DEFAULT = PYTHON

    // 这几个还没运行时，只用来显示名字和做内容校验。
    val PLANNED = listOf("java", "cpp", "go", "rust")

    fun displayNameOf(id: String): String = when (id) {
        PYTHON -> "Python"
        JAVASCRIPT -> "JavaScript"
        TYPESCRIPT -> "TypeScript"
        LUA -> "Lua"
        SQL -> "SQL"
        C -> "C"
        "java" -> "Java"
        "cpp" -> "C++"
        "go" -> "Go"
        "rust" -> "Rust"
        else -> id.replaceFirstChar { it.uppercase() }
    }
}

data class VarInfo(val name: String, val type: String, val value: String)

// 和 Python 侧 runner.py 返回的字段一一对应，改字段记得两边一起改。
data class RunResult(
    val ok: Boolean,
    val stdout: String,
    val stderr: String,
    val errorType: String?,
    val errorMessage: String?,
    val traceback: String?,
    val durationMs: Long,
    val variables: List<VarInfo>,
    val passed: Boolean?
)

interface LanguageRuntime {
    val id: String
    val displayName: String
    val fileExtension: String
    val grammarScope: String
    val version: String

    fun isAvailable(): Boolean
    fun ensureStarted(context: Context)

    fun runCode(
        code: String,
        stdinLines: List<String> = emptyList(),
        timeoutSec: Double = 8.0
    ): RunResult

    fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String> = emptyList()
    ): RunResult

    fun warmup(): Boolean
    fun replStart()
    fun replPush(line: String): Pair<Boolean, String>
}

/**
 * 语言没内置时拿它顶着，UI 不用到处判空。
 */
class UnsupportedRuntime(override val id: String) : LanguageRuntime {

    override val displayName: String get() = LanguageIds.displayNameOf(id)
    override val fileExtension: String get() = "txt"
    override val grammarScope: String get() = "text.plain"
    override val version: String get() = "-"

    override fun isAvailable(): Boolean = false
    override fun ensureStarted(context: Context) = Unit

    private fun notBundled() = RunResult(
        ok = false,
        stdout = "",
        stderr = "",
        errorType = "LanguageNotBundled",
        errorMessage = "「$displayName」运行时未内置于当前版本，暂不可执行。",
        traceback = null,
        durationMs = 0L,
        variables = emptyList(),
        passed = false
    )

    override fun runCode(code: String, stdinLines: List<String>, timeoutSec: Double) = notBundled()

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ) = notBundled()

    override fun warmup(): Boolean = false
    override fun replStart() = Unit
    override fun replPush(line: String): Pair<Boolean, String> =
        false to "「$displayName」运行时未内置于当前版本。\n"
}

object LanguageRegistry {

    private const val PREFS = "mashang_language"
    private const val CURRENT = "current_lang"

    private val runtimes = linkedMapOf<String, LanguageRuntime>()

    fun register(runtime: LanguageRuntime) {
        runtimes[runtime.id] = runtime
    }

    fun all(): List<LanguageRuntime> = runtimes.values.toList()

    fun ids(): List<String> = runtimes.keys.toList()

    fun has(id: String): Boolean = runtimes.containsKey(id)

    /**
     * 空 id 才回退默认语言。非空但没注册的 id 返回 UnsupportedRuntime，
     * 不能悄悄拿 Python 顶替，不然用户会看到一堆莫名其妙的 Python 报错。
     */
    fun get(id: String): LanguageRuntime = resolveRuntime(id, runtimes)

    internal fun resolveRuntime(
        id: String,
        runtimes: Map<String, LanguageRuntime>
    ): LanguageRuntime = when {
        id.isBlank() ->
            runtimes[LanguageIds.DEFAULT] ?: UnsupportedRuntime(id.ifBlank { LanguageIds.DEFAULT })
        else -> runtimes[id] ?: UnsupportedRuntime(id)
    }

    fun currentId(context: Context): String {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CURRENT, LanguageIds.DEFAULT)
            ?: LanguageIds.DEFAULT
        return stored.takeIf { runtimes.containsKey(it) } ?: LanguageIds.DEFAULT
    }

    fun current(context: Context): LanguageRuntime = get(currentId(context))

    fun setCurrent(context: Context, id: String) {
        if (!runtimes.containsKey(id)) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CURRENT, id)
            .apply()
    }
}

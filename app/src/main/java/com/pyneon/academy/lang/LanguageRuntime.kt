package com.pyneon.academy.lang

import android.content.Context

/**
 * 语言标识常量。
 *
 * 取值必须与课程 JSON 里的 `lang` 字段、以及语言扩展包清单里的 `lang` 字段完全一致
 * （小写英文、无空格）。新增语言时在此处补常量，并在 [LanguageRegistry] 中注册对应运行时。
 */
object LanguageIds {
    const val PYTHON = "python"
    const val JAVASCRIPT = "javascript"
    const val TYPESCRIPT = "typescript"
    const val LUA = "lua"
    const val SQL = "sql"
    const val C = "c"

    /** 内置默认语言：任何回落路径都指向它 */
    const val DEFAULT = PYTHON

    /**
     * 已规划但尚未实现的语言：仅用于课程内容校验与显示名映射。
     *
     * 这些语言需要编译器或原生工具链（JVM / NDK / Go toolchain），
     * 无法以纯 JVM 或内嵌解释器方式随 APK 提供；UI 必须显示「暂不支持」，
     * 不得出现下载入口。
     *
     * 注意：本列表是**提示性**的，不参与「语言是否可学」的判定。真正的能力
     * 判定只有一条：【是否有已注册且 isAvailable() 的运行时】。因此新增语言
     * 时不必改动本列表，只需注册运行时 + 提供课程包即可。
     */
    val PLANNED: List<String> = listOf("java", "cpp", "go", "rust")

    /**
     * 显示名映射。注册表中没有对应运行时时也能给出可读名称。
     *
     * 新增语言只需在此补一行（大小写、连字符等展示差异都收敛在这里），
     * 其余 UI 一律通过本函数取名字，不得到处硬编码。
     */
    fun displayNameOf(id: String): String = when (id) {
        PYTHON -> "Python"
        JAVASCRIPT -> "JavaScript"
        LUA -> "Lua"
        SQL -> "SQL"
        "typescript" -> "TypeScript"
        C -> "C"
        "java" -> "Java"
        "cpp" -> "C++"
        "go" -> "Go"
        "rust" -> "Rust"
        else -> id.replaceFirstChar { it.uppercase() }
    }
}

/** 单个变量的求值快照 */
data class VarInfo(val name: String, val type: String, val value: String)

/** 一次执行/判题的统一结果结构（与 runner.py 的 JSON 契约一一对应） */
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

/**
 * 语言运行时统一契约 —— 多语言架构的核心抽象。
 *
 * 设计约定：
 * 1. 一个实现类 = 一门语言的一个运行时后端。
 *    （Python → Chaquopy 内嵌 CPython；JavaScript → Rhino 1.7.15；
 *      TypeScript → 类型擦除后复用 Rhino；C → 纯 Kotlin 教学子集解释器；
 *      Lua → LuaJ 3.0.1；SQL → SQLite 内存库）
 * 2. 跨语言通信统一走 JSON 字符串，字段结构与 Python 侧
 *    `runner.run_code_json` / `check_exercise_json` / `repl.*` 保持一致，
 *    这样新增语言时只需另起一个"说同一种 JSON"的后端，UI 与判题逻辑零改动。
 * 3. UI 层禁止直接引用具体运行时，一律经 `LanguageRegistry.current(context)` 获取。
 * 4. 实现必须是幂等且线程安全的：`ensureStarted` 可被任意界面重复调用。
 */
interface LanguageRuntime {

    /** 语言标识，见 [LanguageIds] */
    val id: String

    /** 展示名，如 "Python" */
    val displayName: String

    /** 源码文件扩展名（不含点），如 "py" */
    val fileExtension: String

    /** 编辑器语法高亮使用的 TextMate scope 名，如 "source.python" */
    val grammarScope: String

    /** 运行时版本字符串，如 "3.13.0"；不可用时返回占位值 */
    val version: String

    /** 该运行时是否已在当前设备上可用（语言包可能未内置） */
    fun isAvailable(): Boolean

    /** 惰性初始化运行时；已启动时直接返回 */
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

    /** 预热（编译/加载标准库），返回是否成功 */
    fun warmup(): Boolean

    /** 重置 REPL 会话 */
    fun replStart()

    /** 推入一行；返回 (是否等待续行, 输出文本) */
    fun replPush(line: String): Pair<Boolean, String>
}

/**
 * 占位运行时：语言未随当前版本内置，或课程 JSON 里出现了未注册的 lang。
 *
 * 存在的意义是让 UI 层永远拿到非空对象，不必到处写空判断；
 * 同时把"语言缺失"这件事以可读的错误回显给用户，而不是抛异常崩溃。
 */
class UnsupportedRuntime(override val id: String) : LanguageRuntime {

    override val displayName: String get() = LanguageIds.displayNameOf(id)
    override val fileExtension: String get() = "txt"
    override val grammarScope: String get() = "text.plain"
    override val version: String get() = "-"
    override fun isAvailable(): Boolean = false
    override fun ensureStarted(context: Context) = Unit

    private fun notBundled(): RunResult = RunResult(
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

    override fun runCode(code: String, stdinLines: List<String>, timeoutSec: Double): RunResult = notBundled()

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult = notBundled()

    override fun warmup(): Boolean = false
    override fun replStart() = Unit
    override fun replPush(line: String): Pair<Boolean, String> =
        false to "「$displayName」运行时未内置于当前版本。\n"
}

/**
 * 语言运行时注册表。
 *
 * - 注册时机：Application.onCreate 中集中 [register]（目前仅 Python）。
 * - 当前语言持久化在 SharedPreferences，卸载语言包后会自动回落到 [LanguageIds.DEFAULT]。
 */
object LanguageRegistry {

    private const val PREFS_NAME = "mashang_language"
    private const val KEY_CURRENT = "current_lang"

    private val runtimes: LinkedHashMap<String, LanguageRuntime> = linkedMapOf()

    /** 注册一个运行时；同 id 后注册者覆盖前者 */
    fun register(runtime: LanguageRuntime) {
        runtimes[runtime.id] = runtime
    }

    /** 已注册的运行时列表（按注册顺序） */
    fun all(): List<LanguageRuntime> = runtimes.values.toList()

    /** 已注册的语言 id 列表 */
    fun ids(): List<String> = runtimes.keys.toList()

    fun has(id: String): Boolean = runtimes.containsKey(id)

    /**
     * 取运行时，永不返回 null。
     *
     * 只有「id 为空」才回落到默认语言；**非空但未注册**的 id 必须返回
     * [UnsupportedRuntime]，而不是悄悄用 Python 顶上。
     * 旧实现无条件回落默认语言，导致「一门没做运行时的语言」被判成可用：
     * 语言中心显示「可离线运行」，用户写完代码后由 Python 报出一堆语法错误，
     * 排查方向完全错（2026-11 审查发现）。
     */
    fun get(id: String): LanguageRuntime = resolveRuntime(id, runtimes)

    /**
     * 纯函数版的回落规则，便于单测（[LanguageRegistry] 是全局 object，
     * 直接测会污染其他用例）。
     *
     * 规则：
     *  - id 为空 → 默认语言（没有默认语言时才 Unsupported）
     *  - id 非空且已注册 → 该运行时
     *  - id 非空但未注册 → UnsupportedRuntime（绝不拿别的语言顶替）
     */
    internal fun resolveRuntime(
        id: String,
        runtimes: Map<String, LanguageRuntime>
    ): LanguageRuntime = when {
        id.isBlank() ->
            runtimes[LanguageIds.DEFAULT] ?: UnsupportedRuntime(id.ifBlank { LanguageIds.DEFAULT })
        else -> runtimes[id] ?: UnsupportedRuntime(id)
    }

    /** 当前语言 id；若首选项里残留了已不可用的语言，自动回落默认语言 */
    fun currentId(context: Context): String {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENT, LanguageIds.DEFAULT) ?: LanguageIds.DEFAULT
        return stored.takeIf { runtimes.containsKey(it) } ?: LanguageIds.DEFAULT
    }

    /** 当前语言对应的运行时 */
    fun current(context: Context): LanguageRuntime = get(currentId(context))

    /** 切换当前语言；未注册的 id 静默忽略 */
    fun setCurrent(context: Context, id: String) {
        if (!runtimes.containsKey(id)) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT, id)
            .apply()
    }
}

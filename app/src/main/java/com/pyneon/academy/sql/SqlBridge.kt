package com.pyneon.academy.sql

import android.content.Context
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRuntime
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.lang.parseRunResultJson
import com.pyneon.academy.py.PyBridge
import org.json.JSONArray
import org.json.JSONObject

/**
 * SQL 运行时：复用内嵌 CPython 的 sqlite3 模块，在内存库上执行脚本。
 *
 * 为什么不做独立 SQL 引擎：
 * - Android 自带 SQLite，CPython 的 sqlite3 已经链接它，零额外体积；
 * - 与 Python 运行时共用 Chaquopy 桥，不引入新的原生依赖；
 * - 执行与判题语义统一走 `sql_runner.py` 的 JSON 契约，与 runner.py 同构。
 *
 * 安全边界：`sql_runner._authorizer` 拒绝 ATTACH / DETACH，数据库固定为
 * `:memory:`，脚本不能打开或写入设备上的任何文件。
 */
object SqlBridge : LanguageRuntime {

    override val id: String = LanguageIds.SQL
    override val displayName: String = "SQL"
    override val fileExtension: String = "sql"
    override val grammarScope: String = "source.sql"

    /** 判题超时（秒）：与 runner.py / JsBridge 的默认值对齐。 */
    private const val DEFAULT_TIMEOUT = 8.0
    override val version: String
        get() = try {
            val obj = JSONObject(module().callAttr("info_json").toString())
            "SQLite " + obj.optString("sqlite_version", "3.x")
        } catch (_: Throwable) {
            "SQLite 3.x"
        }

    override fun isAvailable(): Boolean = true

    override fun ensureStarted(context: Context) {
        PyBridge.ensureStarted(context)
    }

    private fun module() = com.chaquo.python.Python.getInstance().getModule("sql_runner")

    override fun runCode(
        code: String,
        stdinLines: List<String>,
        timeoutSec: Double
    ): RunResult {
        val payload = JSONObject().apply {
            put("code", code)
            put("timeout", timeoutSec)
        }
        return parse(module().callAttr("run_sql_json", payload.toString()).toString())
    }

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult {
        val payload = JSONObject().apply {
            put("code", code)
            put("tests", JSONArray(tests))
            // 判题超时与 runCode 一致：死循环 CTE 必须在执行阶段就被中断，
            // 否则 UI 会永远停在「运行中」（2026-11 审查发现）。
            put("timeout", DEFAULT_TIMEOUT)
        }
        return parse(module().callAttr("check_sql_json", payload.toString()).toString())
    }

    override fun warmup(): Boolean = try {
        runCode("SELECT 1", emptyList(), 6.0).ok
    } catch (_: Throwable) {
        false
    }

    private var replSession: MutableList<String> = mutableListOf()

    override fun replStart() {
        replSession = mutableListOf()
    }

    override fun replPush(line: String): Pair<Boolean, String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false to ""
        // 多行语句：每一行都要进缓冲，只有出现分号才提交。
        replSession.add(trimmed)
        if (!trimmed.endsWith(";")) return true to ""
        val script = replSession.joinToString("\n")
        replSession = mutableListOf()
        val result = runCode(script, emptyList(), 8.0)
        val text = buildString {
            if (result.stdout.isNotEmpty()) append(result.stdout).append('\n')
            result.errorType?.let { append(it).append(": ").append(result.errorMessage ?: "").append('\n') }
        }
        return false to text
    }

    private fun parse(raw: String): RunResult = parseRunResultJson(raw)
}

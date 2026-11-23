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
 * SQL 运行时直接用内嵌 CPython 的 sqlite3，在内存库上跑。
 *
 * Android 自带 SQLite，CPython 的 sqlite3 已经链上了，不用再带一个引擎，
 * 也继续用 Chaquopy 这座桥。执行和判题都走 sql_runner.py 的 JSON，和 runner.py 一样。
 *
 * sql_runner 的 authorizer 会拒 ATTACH/DETACH，库固定是 :memory:，
 * 脚本碰不到设备上的文件。
 */
object SqlBridge : LanguageRuntime {

    override val id: String = LanguageIds.SQL
    override val displayName: String = "SQL"
    override val fileExtension: String = "sql"
    override val grammarScope: String = "source.sql"

    // 判题超时，和 runner.py、JsBridge 默认值一致。
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
            // 判题也得带超时。死循环 CTE 要在执行时掐掉，不然界面一直转圈。
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

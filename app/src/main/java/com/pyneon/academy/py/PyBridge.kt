package com.pyneon.academy.py

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.pyneon.academy.lang.LanguageIds
import com.pyneon.academy.lang.LanguageRuntime
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.lang.parseRunResultJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Python 运行时（Chaquopy 内嵌 CPython）。
 *
 * 多语言架构下它是 [LanguageRuntime] 的首个实现：UI 与判题逻辑只依赖接口，
 * 新增 JavaScript / Java 等语言时新增实现类并注册进 LanguageRegistry 即可，
 * 本文件之外的调用方无需改动。
 */
object PyBridge : LanguageRuntime {

    override val id: String = LanguageIds.PYTHON
    override val displayName: String = "Python"
    override val fileExtension: String = "py"
    override val grammarScope: String = "source.python"

    @Volatile
    private var started = false

    override val version: String
        get() = pythonVersion()

    override fun isAvailable(): Boolean = started || Python.isStarted()

    override fun ensureStarted(context: Context) {
        if (started) return
        synchronized(this) {
            if (!started && !Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
                started = true
            }
        }
    }

    private fun module(name: String) = Python.getInstance().getModule(name)

    /**
     * 执行互斥锁。
     *
     * runner.py 的一次运行会改写进程级全局 `sys.stdout` / `sys.stderr` /
     * `builtins.input`，两次执行并发进入就会互相劫持缓冲区（实测：A 的 47 行
     * 输出被写进 B 的输出、A 自己只剩 3 行）。Python 侧已有 `runner.EXEC_LOCK`
     * 串行化，这里再加一层与 [com.pyneon.academy.js.JsBridge] /
     * [com.pyneon.academy.lua.LuaBridge] 相同的 synchronized 门闩：
     * 让阻塞发生在 JVM 侧，避免多线程同时压进 Chaquopy 的调用通道。
     * 同一线程重入是安全的（JVM monitor 可重入，warmup → runCode 依赖这一点）。
     */
    private val execLock = Any()

    override fun runCode(
        code: String,
        stdinLines: List<String>,
        timeoutSec: Double
    ): RunResult {
        val payload = JSONObject().apply {
            put("code", code)
            put("stdin", JSONArray(stdinLines))
            put("timeout", timeoutSec)
        }
        return synchronized(execLock) {
            parseRun(module("runner").callAttr("run_code_json", payload.toString()).toString())
        }
    }

    override fun checkExercise(
        code: String,
        tests: List<String>,
        stdinLines: List<String>
    ): RunResult {
        val payload = JSONObject().apply {
            put("code", code)
            put("tests", JSONArray(tests))
            put("stdin", JSONArray(stdinLines))
        }
        return synchronized(execLock) {
            parseRun(module("runner").callAttr("check_exercise_json", payload.toString()).toString())
        }
    }

    fun pythonVersion(): String {
        return try {
            val obj = JSONObject(module("runner").callAttr("info_json").toString())
            obj.optString("python_version", "3.x")
        } catch (_: Throwable) {
            "3.x"
        }
    }

    override fun warmup(): Boolean {
        return try {
            runCode("pass", emptyList(), 6.0).ok
        } catch (_: Throwable) {
            false
        }
    }

    override fun replStart() {
        module("repl").callAttr("start")
    }

    override fun replPush(line: String): Pair<Boolean, String> {
        val obj = JSONObject(module("repl").callAttr("push", line).toString())
        return obj.getBoolean("more") to obj.getString("output")
    }

    private fun parseRun(raw: String): RunResult = parseRunResultJson(raw)
}

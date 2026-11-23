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
 * Python 运行时，Chaquopy 内嵌 CPython。
 * 它是 LanguageRuntime 的第一个实现，上层只认接口，
 * 加别的语言时注册一个新实现就行，调用方不用动。
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

    // 执行互斥锁。runner.py 跑一次会改进程里的 sys.stdout、sys.stderr 和 input，
    // 两个一起跑就会互相抢缓冲区，输出全串。Python 那边有 EXEC_LOCK，
    // 这层再挡一次，把阻塞放在 JVM 侧，别让线程都挤进 Chaquopy。
    // 同线程重入没问题，warmup 里调 runCode 就靠这个。
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

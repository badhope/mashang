package com.pyneon.academy.lang

import org.json.JSONArray
import org.json.JSONObject

// Python 和 SQL 引擎都返回这个结构，统一在这里解析。
fun parseRunResultJson(raw: String): RunResult {
    val obj = JSONObject(raw)
    val errorObj = obj.optJSONObject("error")
    val varsJson = obj.optJSONArray("variables") ?: JSONArray()
    val variables = List(varsJson.length()) { i ->
        val v = varsJson.getJSONObject(i)
        VarInfo(v.getString("name"), v.getString("type"), v.getString("value"))
    }
    return RunResult(
        ok = obj.getBoolean("ok"),
        stdout = obj.optString("stdout"),
        stderr = obj.optString("stderr"),
        errorType = errorObj?.optString("type")?.ifEmpty { null },
        errorMessage = errorObj?.optString("message")?.ifEmpty { null },
        traceback = errorObj?.optString("traceback")?.ifEmpty { null },
        durationMs = obj.optLong("duration_ms"),
        variables = variables,
        passed = if (obj.has("passed") && !obj.isNull("passed")) obj.getBoolean("passed") else null
    )
}

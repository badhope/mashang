package com.pyneon.academy.lang

import org.json.JSONArray
import org.json.JSONObject

/**
 * 解析 Python / SQL 引擎返回的统一 JSON 契约。
 *
 * 2026-11 去重：PyBridge 与 SqlBridge 曾各写一份字段映射，SqlBridge 还漏掉了
 * `variables`（目前 SQL 恒为空，但契约应一致）。这里作为唯一解析入口。
 */
fun parseRunResultJson(raw: String): RunResult {
    val obj = JSONObject(raw)
    val errorObj = obj.optJSONObject("error")
    val varsJson = obj.optJSONArray("variables") ?: JSONArray()
    val variables = buildList {
        for (i in 0 until varsJson.length()) {
            val v = varsJson.getJSONObject(i)
            add(VarInfo(v.getString("name"), v.getString("type"), v.getString("value")))
        }
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

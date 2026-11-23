package com.pyneon.academy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 手动检查更新。主发国内应用市场，不在 Google Play。
 * 先查 GitCode，挂了再查 GitHub。只读公开版本号，不带设备信息，
 * 不自动下载，也不申请安装权限。查不到就算了，不影响离线学习。
 */
object UpdateChecker {

    data class ReleaseInfo(
        val version: String,
        val url: String,
        val publishedAt: String
    )

    // 按顺序试，哪个先成功就用哪个。
    private val SOURCES = listOf(
        "https://api.gitcode.com/api/v5/repos/badhope/mashang/releases/latest",
        "https://api.github.com/repos/badhope/mashang/releases/latest"
    )

    suspend fun fetchLatest(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (api in SOURCES) {
            try {
                val body = HttpText.getText(
                    url = api,
                    timeoutMs = 6000,
                    maxBytes = 2 * 1024 * 1024,
                    accept = "application/json"
                )
                val json = JSONObject(body)
                val tag = json.optString("tag_name").trim().removePrefix("v")
                // GitCode 和 GitHub 的字段名不一样，两个都认。
                val url = json.optString("html_url").ifBlank { json.optString("web_url") }
                if (tag.isBlank() || url.isBlank()) {
                    lastError = IllegalStateException("返回内容缺少版本信息")
                    continue
                }
                return@withContext Result.success(
                    ReleaseInfo(tag, url, json.optString("published_at"))
                )
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IllegalStateException("所有更新源均不可达"))
    }

    // 按点号拆版本号逐段比，格式不对就当没有新版本。
    fun isNewer(latest: String, current: String): Boolean {
        fun parse(v: String): List<Int>? =
            v.trim().removePrefix("v").split('.').map { it.toIntOrNull() ?: return null }
        val a = parse(latest) ?: return false
        val b = parse(current) ?: return false
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

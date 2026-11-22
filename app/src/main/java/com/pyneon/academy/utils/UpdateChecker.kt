package com.pyneon.academy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 手动检查更新。
 *
 * 渠道策略（2026-11 决策：主发国内安卓应用市场，不投 Google Play）：
 *   1. **GitCode 优先** —— 国内直连稳定，作为主更新源
 *   2. **GitHub 兜底** —— 海外网络或 GitCode 故障时使用
 * 只读取公开发布版本号，不带任何设备标识；不自动下载、不申请安装权限。
 * 失败时静默降级为「无法检查」，绝不阻塞离线学习。
 */
object UpdateChecker {

    data class ReleaseInfo(
        val version: String,
        val url: String,
        val publishedAt: String
    )

    /** 按顺序尝试的发布源，第一个成功即返回。 */
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
                // GitCode 与 GitHub 的详情页字段名不同，两者都兼容
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

    /** 语义化比较：返回 latest 是否比 current 新。非法输入按「不更新」处理。 */
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

package com.pyneon.academy.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 内容中心和检查更新共用的小 HTTP 客户端，限制超时和响应大小。
 */
object HttpText {
    const val DEFAULT_USER_AGENT = "CodeLearning-Android"
    private const val DEFAULT_TIMEOUT_MS = 6000
    private const val DEFAULT_MAX_BYTES = 5 * 1024 * 1024

    fun getText(
        url: String,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        maxBytes: Int = DEFAULT_MAX_BYTES,
        accept: String = "application/json",
        userAgent: String = DEFAULT_USER_AGENT
    ): String = String(getBytes(url, timeoutMs, maxBytes, accept, userAgent), StandardCharsets.UTF_8)

    fun getBytes(
        url: String,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        maxBytes: Int = DEFAULT_MAX_BYTES,
        accept: String = "application/octet-stream",
        userAgent: String = DEFAULT_USER_AGENT
    ): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IOException("HTTP ${conn.responseCode} ($url)")
            }
            return conn.inputStream.use { readCapped(it, maxBytes, url) }
        } finally {
            conn.disconnect()
        }
    }

    private fun readCapped(stream: InputStream, maxBytes: Int, url: String): ByteArray {
        val buf = ByteArray(64 * 1024)
        var total = 0
        val out = ByteArrayOutputStream(256 * 1024)
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            total += n
            if (total > maxBytes) {
                throw IOException("HTTP 响应超过 ${maxBytes / (1024 * 1024)}MB，已断开 ($url)")
            }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

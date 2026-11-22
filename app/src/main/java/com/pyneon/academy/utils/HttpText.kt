package com.pyneon.academy.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 统一 HTTP 文本/字节读取。
 *
 * 2026-11 去重：ContentCenter 与 UpdateChecker 曾各写一份 HttpURLConnection，
 * 超时、UA、响应码、5MB 上限与流式读取的细节并不一致。这里收敛为一个入口，
 * 所有下载都必须显式给出超时与体积上限，禁止无界 readText()。
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

    /** 按字节流式读取并设上限；超限立即断开并抛错，避免 OOM。 */
    private fun readCapped(stream: InputStream, maxBytes: Int, url: String): ByteArray {
        val buf = ByteArray(64 * 1024)
        var total = 0
        val out = ByteArrayOutputStream(256 * 1024)
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            total += n
            if (total > maxBytes) {
                throw IOException(
                    "HTTP 响应超过 ${maxBytes / (1024 * 1024)}MB 上限，已断开以防 OOM ($url)"
                )
            }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

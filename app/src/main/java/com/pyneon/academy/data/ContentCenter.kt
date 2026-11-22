package com.pyneon.academy.data

import android.content.Context
import android.util.Log
import com.pyneon.academy.utils.HttpText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class ContentPack(
    val id: String,
    val name: String,
    val version: Int,
    val description: String,
    val sha256: String? = null,
    /** 内容类型：course = 课程包（增补/语言），默认 course */
    val kind: String = "course",
    /** 该包课程所属语言 id；空表示不绑定语言（Python 增补包除外都应有值） */
    val lang: String = "",
    /** 讲数（catalog 提供，仅用于展示；缺失为 0 时 UI 不显示） */
    val lessons: Int = 0,
    /** 包体积（字节，catalog 提供；缺失为 0 时 UI 不显示） */
    val bytes: Long = 0,
    /** 难度层级：basics / core / advanced；空表示未分层 */
    val tier: String = ""
)

data class ContentCatalog(
    val updatedAt: String,
    val packs: List<ContentPack>
)

class ContentCenter {

    companion object {
        private const val TIMEOUT_MS = 10000
        // 目录只用来决定“去哪儿下载”，单个源不值得等 10 秒。
        // 国内网络下 GitHub 可能先卡满 10 秒再失败，三个源最坏 20 秒才回落 GitCode。
        // 2026-11：改为 GitCode 优先，目录源 4 秒超时，最坏 8 秒内拿到目录。
        private const val CATALOG_TIMEOUT_MS = 4000
        private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024 // C5: HTTP 响应体积上限 5MB
        private const val TAG = "ContentCenter"
        // C3: 包 id 白名单，仅允许安全文件名字符，杜绝路径穿越
        private val SAFE_ID_REGEX = Regex("""^[a-zA-Z0-9_-]+$""")

        // 国内优先：GitCode OpenAPI → GitHub raw（主仓）→ GitHub Pages（CDN）。
        // 三个源都指向同一份 catalog.json（Pages 的副本由 tools/sync_pages_assets.py 生成并校验）。
        private val CATALOG_URLS = listOf(
            "https://api.gitcode.com/api/v5/repos/badhope/mashang/raw/catalog.json",
            "https://raw.githubusercontent.com/badhope/mashang/main/catalog.json",
            "https://badhope.github.io/mashang/catalog.json"
        )
    }

    fun packsDir(context: Context): File =
        File(context.filesDir, "courses").apply { mkdirs() }

    fun installedVersions(context: Context): Map<String, Int> {
        val regex = Regex("""^(.+)\.v(\d+)\.json$""")
        return packsDir(context).listFiles()
            ?.mapNotNull { f ->
                val m = regex.find(f.name) ?: return@mapNotNull null
                m.groupValues[1] to (m.groupValues[2].toIntOrNull() ?: 0)
            }?.toMap() ?: emptyMap()
    }

    fun installedFiles(context: Context): List<File> =
        packsDir(context).listFiles()?.filter { it.extension == "json" && !it.name.startsWith("tmp_") } ?: emptyList()

    fun fetchCatalog(): Pair<ContentCatalog, String> {
        var lastError: Exception? = null
        for (url in CATALOG_URLS) {
            try {
                val text = httpGet(url, CATALOG_TIMEOUT_MS)
                val obj = JSONObject(text)
                // C1: catalog 强制 sha256 校验（必须提供，缺失即拒绝）
                val catalogSha = obj.optString("sha256").takeIf { it.isNotBlank() }
                    ?: throw SecurityException("catalog 缺少 sha256 字段，拒绝加载（$url）")
                // 自引用约束：签名目标是“剥离顶层 sha256 字段后的原文”，
                // 这样目录自身的哈希可计算，且篡改任一字段都会导致校验失败。
                if (sha256Hex(stripSelfSha(text)) != catalogSha) {
                    throw SecurityException("catalog 完整性校验失败（sha256 不匹配）: $url")
                }
                val packs = mutableListOf<ContentPack>()
                val arr = obj.optJSONArray("packs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val id = p.optString("id", "")
                    // C3: 非法 id 直接跳过，避免进入下载链路
                    if (!id.matches(SAFE_ID_REGEX)) {
                        Log.w(TAG, "跳过非法包 id（含非法字符）: '$id'")
                        continue
                    }
                    packs.add(
                        ContentPack(
                            id = id,
                            name = p.getString("name"),
                            version = p.getInt("version"),
                            description = p.optString("description"),
                            sha256 = p.optString("sha256").takeIf { it.isNotBlank() },
                            kind = p.optString("kind", "course"),
                            lang = p.optString("lang", ""),
                            lessons = p.optInt("lessons", 0),
                            bytes = p.optLong("bytes", 0L),
                            tier = p.optString("tier", "")
                        )
                    )
                }
                return ContentCatalog(obj.optString("updated_at"), packs) to url.removeSuffix("/catalog.json")
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException("课程目录不可达: ${lastError?.message}", lastError)
    }

    fun download(context: Context, pack: ContentPack, baseUrl: String): File {
        requireSafeId(pack.id) // C3: 强制白名单校验，非法 id 不落盘
        val packs = packsDir(context)
        val target = File(packs, "${pack.id}.v${pack.version}.json")
        val tmp = File(packs, "tmp_${pack.id}.v${pack.version}.json")
        // C3: 规范化路径断言，防止写越出 courses/ 目录（纵深防御）
        val packsCanon = packs.canonicalPath
        for (f in listOf(target, tmp)) {
            val canon = f.canonicalPath
            require(canon == packsCanon || canon.startsWith("$packsCanon${File.separator}")) {
                "拒绝写入越界路径: $canon"
            }
        }
        val text = httpGet("$baseUrl/content_packs/${pack.id}.json")
        // C1: pack 强制 sha256 校验（必须提供，缺失即拒绝）
        val expected = pack.sha256 ?: throw SecurityException("pack ${pack.id} 缺少 sha256 字段，拒绝下载")
        if (sha256Hex(text) != expected) {
            throw SecurityException("pack ${pack.id} 完整性校验失败（sha256 不匹配）")
        }
        JSONArray(text)
        tmp.writeText(text, StandardCharsets.UTF_8)
        packs.listFiles()
            ?.filter { it.name.startsWith("${pack.id}.v") && it.name != target.name }
            ?.forEach { it.delete() }
        tmp.renameTo(target)
        return target
    }

    private fun requireSafeId(id: String) {
        require(id.isNotBlank() && id.matches(SAFE_ID_REGEX)) {
            "非法包 id（含非法字符，已拒绝）: '$id'"
        }
    }

    private fun httpGet(url: String, timeoutMs: Int = TIMEOUT_MS): String =
        HttpText.getText(url, timeoutMs, MAX_RESPONSE_BYTES, accept = "application/json")

    private fun httpGetBytes(url: String, maxBytes: Int): ByteArray =
        HttpText.getBytes(url, TIMEOUT_MS, maxBytes, accept = "application/octet-stream")

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /**
     * 剥离顶层 `"sha256": "<64位hex>"` 字段，使目录自签名可计算。
     * 注意：只替换第一次出现（count=1），即顶层字段；
     * packs[].sha256 等子字段保留原值并进入签名覆盖范围——
     * 若用正则替换全部 sha256 字段，目录对"篡改各包哈希"将失去校验能力。
     */
    private fun stripSelfSha(text: String): String =
        text.replaceFirst(Regex("\"sha256\"\\s*:\\s*\"[0-9a-f]{64}\""), "\"sha256\": \"\"")
}

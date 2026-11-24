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
    // course 就是课程包，增补和语言包都算。
    val kind: String = "course",
    // 课程语言。空表示不绑定，Python 增补包可以不填。
    val lang: String = "",
    // 讲数，目录里没给就显示 0，界面会跳过。
    val lessons: Int = 0,
    // 包大小，单位字节，没给就不显示。
    val bytes: Long = 0,
    // 难度层级 basics/core/advanced，空就是没分层。
    val tier: String = ""
)

data class ContentCatalog(
    val updatedAt: String,
    val packs: List<ContentPack>
)

class ContentCenter {

    companion object {
        private const val TIMEOUT_MS = 10000
        // 目录只是决定去哪儿下载，一个源不值得等太久。
        // 国内 GitHub 可能先卡满再失败，所以 GitCode 放前面，单源 4 秒，最坏 8 秒。
        private const val CATALOG_TIMEOUT_MS = 4000
        private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024 // 下载响应最多 5MB。
        private const val TAG = "ContentCenter"
        // 离线目录里的包没有 base；万一某个包文件缺失，仍能用这个源补下。
        const val FALLBACK_BASE_URL = "https://badhope.github.io/mashang"
        // 包 id 只允许字母、数字、下划线和横杠，挡路径穿越。
        private val SAFE_ID_REGEX = Regex("""^[a-zA-Z0-9_-]+$""")
        // assets 里的预装包带版本后缀，例如 js-basics.v2.json。
        private val BUNDLED_NAME_REGEX = Regex("""^([a-zA-Z0-9_-]+)\.v(\d+)\.json$""")
        private val seedLock = Any()

        // 国内优先 GitCode，再 GitHub raw，最后 Pages。
        // 三个地址内容一样，Pages 那份由 sync_pages_assets.py 同步。
        private val CATALOG_URLS = listOf(
            "https://api.gitcode.com/api/v5/repos/badhope/mashang/raw/catalog.json",
            "https://raw.githubusercontent.com/badhope/mashang/main/catalog.json",
            "https://badhope.github.io/mashang/catalog.json"
        )
    }

    fun packsDir(context: Context): File =
        File(context.filesDir, "courses").apply { mkdirs() }

    fun installedVersions(context: Context): Map<String, Int> {
        seedBundledPacks(context)
        val regex = Regex("""^(.+)\.v(\d+)\.json$""")
        return packsDir(context).listFiles()
            ?.mapNotNull { f ->
                val m = regex.find(f.name) ?: return@mapNotNull null
                m.groupValues[1] to (m.groupValues[2].toIntOrNull() ?: 0)
            }?.toMap() ?: emptyMap()
    }

    fun installedFiles(context: Context): List<File> {
        seedBundledPacks(context)
        return packsDir(context).listFiles()
            ?.filter { it.extension == "json" && !it.name.startsWith("tmp_") }
            ?: emptyList()
    }

    /**
     * 把 APK assets 里的基础课程包复制到 filesDir/courses。
     *
     * 只在没有同版本或更高版本时复制；用户已经联网更新到 v3 时，
     * 不会在下次启动又被 v2 覆盖。复制后删掉同包的旧版本，避免列表里
     * 同一讲出现两份。
     */
    fun seedBundledPacks(context: Context): Int = synchronized(seedLock) {
        val dir = packsDir(context)
        val names = context.assets.list("content_packs") ?: emptyArray()
        var copied = 0
        for (name in names) {
            val match = BUNDLED_NAME_REGEX.matchEntire(name) ?: continue
            val id = match.groupValues[1]
            val version = match.groupValues[2].toIntOrNull() ?: continue
            val installed = dir.listFiles()
                ?.mapNotNull { f ->
                    val m = Regex("""^(.+)\.v(\d+)\.json$""").find(f.name) ?: return@mapNotNull null
                    if (m.groupValues[1] != id) null
                    else f to (m.groupValues[2].toIntOrNull() ?: 0)
                }
                ?: emptyList()
            if (installed.any { it.second >= version }) continue

            val target = File(dir, name)
            val tmp = File(dir, "tmp_seed_$name")
            try {
                context.assets.open("content_packs/$name").use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                // 预装内容也得是合法 JSON 数组，坏文件不能进课程列表。
                JSONArray(tmp.readText(StandardCharsets.UTF_8))
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                installed.filter { it.second < version }.forEach { it.first.delete() }
                copied++
            } catch (e: Exception) {
                tmp.delete()
                Log.w(TAG, "预装课程包失败: $name", e)
            }
        }
        copied
    }

    /** 离线目录，和 APK 一起装；语言中心没网也能列出全部预装包。 */
    fun bundledCatalog(context: Context): ContentCatalog? = runCatching {
        val text = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        parseCatalog(text, "assets/catalog.json")
    }.getOrNull()

    fun fetchCatalog(): Pair<ContentCatalog, String> {
        var lastError: Exception? = null
        for (url in CATALOG_URLS) {
            try {
                val text = httpGet(url, CATALOG_TIMEOUT_MS)
                return parseCatalog(text, url) to url.removeSuffix("/catalog.json")
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException("课程目录不可达: ${lastError?.message}", lastError)
    }

    private fun parseCatalog(text: String, source: String): ContentCatalog {
        val obj = JSONObject(text)
        // 目录必须带 sha256，没带就拒绝。
        val catalogSha = obj.optString("sha256").takeIf { it.isNotBlank() }
            ?: throw SecurityException("catalog 缺少 sha256 字段，拒绝加载（$source）")
        // 签名的是去掉顶层 sha256 后的原文。这样目录能自己算自己，
        // 改哪个字段都会对不上。
        if (sha256Hex(stripSelfSha(text)) != catalogSha) {
            throw SecurityException("catalog 完整性校验失败（sha256 不匹配）: $source")
        }
        val packs = mutableListOf<ContentPack>()
        val arr = obj.optJSONArray("packs") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val id = p.optString("id", "")
            // id 不合法就跳过，不往下走。
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
        return ContentCatalog(obj.optString("updated_at"), packs)
    }

    fun download(context: Context, pack: ContentPack, baseUrl: String): File {
        requireSafeId(pack.id) // 非法 id 不许落盘。
        val packs = packsDir(context)
        val target = File(packs, "${pack.id}.v${pack.version}.json")
        val tmp = File(packs, "tmp_${pack.id}.v${pack.version}.json")
        // 再查一遍规范化路径，别写出 courses 目录。
        val packsCanon = packs.canonicalPath
        for (f in listOf(target, tmp)) {
            val canon = f.canonicalPath
            require(canon == packsCanon || canon.startsWith("$packsCanon${File.separator}")) {
                "拒绝写入越界路径: $canon"
            }
        }
        val text = httpGet("$baseUrl/content_packs/${pack.id}.json")
        // 包也必须带 sha256，没有就不下。
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

    // 只替换第一次出现的顶层 sha256。packs 里的 sha256 要留着参与签名，
    // 全替换就挡不住别人改包哈希了。
    private fun stripSelfSha(text: String): String =
        text.replaceFirst(Regex("\"sha256\"\\s*:\\s*\"[0-9a-f]{64}\""), "\"sha256\": \"\"")
}

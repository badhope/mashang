package com.pyneon.academy.data

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import org.json.JSONArray
import org.json.JSONObject

object BackupUtil {
    private const val BACKUP_VERSION = 2

    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val timestamp: Long = System.currentTimeMillis(),
        val xpTotal: Int,
        val completedLessons: Set<String>,
        val solvedKeys: Set<String>,
        val streakDays: Int,
        val longestStreak: Int,
        val totalActiveDays: Int,
        val lastActiveEpochDay: Long,
        val dailyDate: Long,
        val dailyCount: Int,
        val firstRunDone: Boolean,
        val nightOwl: Boolean,
        val terminalHead: Boolean,
        val badges: Set<String>,
        val reviewCards: List<ReviewCard>,
        val mistakes: List<MistakeRecord>
    )

    // Export backup to Downloads/CodeLearning
    suspend fun exportBackup(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val progress = ProgressStore.snapshot(context)
            val reviewCards = loadReviewCardsFromFile(context)
            val mistakes = loadMistakesFromFile(context)

            val backup = BackupData(
                xpTotal = progress.xpTotal,
                completedLessons = progress.completedLessons,
                solvedKeys = progress.solvedKeys,
                streakDays = progress.streakDays,
                longestStreak = progress.longestStreak,
                totalActiveDays = progress.totalActiveDays,
                lastActiveEpochDay = progress.lastActiveEpochDay,
                dailyDate = progress.dailyDate,
                dailyCount = progress.dailyCount,
                firstRunDone = progress.firstRunDone,
                nightOwl = progress.nightOwl,
                terminalHead = progress.terminalHead,
                badges = progress.badges,
                reviewCards = reviewCards,
                mistakes = mistakes
            )

            val jsonString = backupToJson(backup).toString(2)
            val uri = saveToStorage(context, jsonString, "codelearning_backup_${System.currentTimeMillis()}.json")
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Import backup from file picker
    suspend fun importBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("无法打开文件"))

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val backup = backupFromJson(JSONObject(jsonString))

            if (backup.version > BACKUP_VERSION) {
                return@withContext Result.failure(IllegalArgumentException("备份版本过新，请更新应用"))
            }

            // Restore DataStore progress
            ProgressStore.restoreFromBackup(
                context,
                xpTotal = backup.xpTotal,
                completedLessons = backup.completedLessons,
                solvedKeys = backup.solvedKeys,
                streakDays = backup.streakDays,
                lastActiveEpochDay = backup.lastActiveEpochDay,
                dailyDate = backup.dailyDate,
                dailyCount = backup.dailyCount,
                firstRunDone = backup.firstRunDone,
                nightOwl = backup.nightOwl,
                terminalHead = backup.terminalHead,
                badges = backup.badges
            )
            // restoreFromBackup 不带 longest/total（旧备份字段），单独补写
            ProgressStore.updateStreakStats(context, backup.streakDays, backup.longestStreak, backup.totalActiveDays)

            // Save review cards and mistakes to their JSON files
            saveReviewCardsToFile(context, backup.reviewCards)
            saveMistakesToFile(context, backup.mistakes)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- JSON serialization helpers using org.json ----
    private fun setToJson(set: Set<String>): JSONArray = JSONArray().apply {
        for (item in set) put(item)
    }

    private fun setFromJson(arr: JSONArray): Set<String> = buildSet {
        for (i in 0 until arr.length()) add(arr.getString(i))
    }

    private fun progressToJson(b: BackupData) = JSONObject().apply {
        put("xpTotal", b.xpTotal)
        put("completedLessons", setToJson(b.completedLessons))
        put("solvedKeys", setToJson(b.solvedKeys))
        put("streakDays", b.streakDays)
        put("longestStreak", b.longestStreak)
        put("totalActiveDays", b.totalActiveDays)
        put("lastActiveEpochDay", b.lastActiveEpochDay)
        put("dailyDate", b.dailyDate)
        put("dailyCount", b.dailyCount)
        put("firstRunDone", b.firstRunDone)
        put("nightOwl", b.nightOwl)
        put("terminalHead", b.terminalHead)
        put("badges", setToJson(b.badges))
    }

    private fun progressFromJson(o: JSONObject) = object {
        val xpTotal = o.optInt("xpTotal")
        val completedLessons = setFromJson(o.getJSONArray("completedLessons"))
        val solvedKeys = setFromJson(o.getJSONArray("solvedKeys"))
        val streakDays = o.optInt("streakDays")
        val longestStreak = o.optInt("longestStreak")
        val totalActiveDays = o.optInt("totalActiveDays")
        val lastActiveEpochDay = o.optLong("lastActiveEpochDay", -1L)
        val dailyDate = o.optLong("dailyDate", -1L)
        val dailyCount = o.optInt("dailyCount")
        val firstRunDone = o.optBoolean("firstRunDone")
        val nightOwl = o.optBoolean("nightOwl")
        val terminalHead = o.optBoolean("terminalHead")
        val badges = setFromJson(o.getJSONArray("badges"))
    }

    // 2026-11 去重：卡片/错题不再手写字段映射，统一走 kotlinx.serialization；
    // 与 ViewModels、JsonListFile 共用同一份 @Serializable 定义。
    private fun reviewCardsToJson(cards: List<ReviewCard>): JSONArray =
        JSONArray(UserDataJson.encodeToString(ListSerializer(ReviewCard.serializer()), cards))

    private fun reviewCardsFromJson(arr: JSONArray): List<ReviewCard> =
        UserDataJson.decodeFromString(ListSerializer(ReviewCard.serializer()), arr.toString())

    private fun mistakesToJson(mistakes: List<MistakeRecord>): JSONArray =
        JSONArray(UserDataJson.encodeToString(ListSerializer(MistakeRecord.serializer()), mistakes))

    private fun mistakesFromJson(arr: JSONArray): List<MistakeRecord> =
        UserDataJson.decodeFromString(ListSerializer(MistakeRecord.serializer()), arr.toString())

    private fun backupToJson(b: BackupData) = JSONObject().apply {
        put("version", b.version)
        put("timestamp", b.timestamp)
        put("progress", progressToJson(b))
        put("reviewCards", reviewCardsToJson(b.reviewCards))
        put("mistakes", mistakesToJson(b.mistakes))
    }

    private fun backupFromJson(o: JSONObject): BackupData {
        val p = progressFromJson(o.getJSONObject("progress"))
        return BackupData(
            version = o.optInt("version", 1),
            timestamp = o.optLong("timestamp"),
            xpTotal = p.xpTotal,
            completedLessons = p.completedLessons,
            solvedKeys = p.solvedKeys,
            streakDays = p.streakDays,
            longestStreak = p.longestStreak,
            totalActiveDays = p.totalActiveDays,
            lastActiveEpochDay = p.lastActiveEpochDay,
            dailyDate = p.dailyDate,
            dailyCount = p.dailyCount,
            firstRunDone = p.firstRunDone,
            nightOwl = p.nightOwl,
            terminalHead = p.terminalHead,
            badges = p.badges,
            reviewCards = reviewCardsFromJson(o.getJSONArray("reviewCards")),
            mistakes = mistakesFromJson(o.getJSONArray("mistakes"))
        )
    }

    // File I/O helpers：与 ViewModels 共用 UserDataFiles 的 store，别再各写一套。
    private fun loadReviewCardsFromFile(context: Context): List<ReviewCard> =
        UserDataFiles.reviewCardsStore(context).read()

    private fun loadMistakesFromFile(context: Context): List<MistakeRecord> =
        UserDataFiles.mistakesStore(context).read()

    private fun saveReviewCardsToFile(context: Context, cards: List<ReviewCard>) {
        UserDataFiles.reviewCardsStore(context).write(cards)
    }

    private fun saveMistakesToFile(context: Context, mistakes: List<MistakeRecord>) {
        UserDataFiles.mistakesStore(context).write(mistakes)
    }

    private fun saveToStorage(context: Context, jsonString: String, fileName: String): Uri =
        saveToMediaStoreQ(context, jsonString, fileName)

    private fun saveToMediaStoreQ(context: Context, jsonString: String, fileName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CodeLearning")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("创建文件失败")

        try {
            val outputStream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("无法打开输出流")
            try {
                outputStream.write(jsonString.toByteArray())
            } finally {
                outputStream.close()
            }
        } catch (e: Exception) {
            // 写入失败时删除半成品，避免 MediaStore 里留下 pending 的空文件
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        // MediaStore 写入完成后系统自动索引，无需手动发扫描广播
        return uri
    }

    // Launch file picker for import
    fun launchImportPicker(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
        }
        activity.startActivityForResult(intent, requestCode)
    }
}

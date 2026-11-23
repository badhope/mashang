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

    suspend fun exportBackup(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val progress = ProgressStore.snapshot(context)
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
                reviewCards = UserDataFiles.reviewCardsStore(context).read(),
                mistakes = UserDataFiles.mistakesStore(context).read()
            )
            val name = "codelearning_backup_${System.currentTimeMillis()}.json"
            Result.success(saveBackup(context, backupToJson(backup).toString(2), name))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("无法打开文件"))
            val json = input.bufferedReader().use { it.readText() }
            val backup = backupFromJson(JSONObject(json))

            if (backup.version > BACKUP_VERSION) {
                return@withContext Result.failure(IllegalArgumentException("备份版本过新，请更新应用"))
            }

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
            ProgressStore.updateStreakStats(
                context,
                backup.streakDays,
                backup.longestStreak,
                backup.totalActiveDays
            )

            UserDataFiles.reviewCardsStore(context).write(backup.reviewCards)
            UserDataFiles.mistakesStore(context).write(backup.mistakes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun setToJson(set: Set<String>) = JSONArray().apply {
        for (item in set) put(item)
    }

    private fun setFromJson(arr: JSONArray) = buildSet {
        for (i in 0 until arr.length()) add(arr.getString(i))
    }

    private fun backupToJson(b: BackupData) = JSONObject().apply {
        put("version", b.version)
        put("timestamp", b.timestamp)
        put("progress", JSONObject().apply {
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
        })
        put("reviewCards", JSONArray(UserDataJson.encodeToString(ListSerializer(ReviewCard.serializer()), b.reviewCards)))
        put("mistakes", JSONArray(UserDataJson.encodeToString(ListSerializer(MistakeRecord.serializer()), b.mistakes)))
    }

    private fun backupFromJson(o: JSONObject): BackupData {
        val p = o.getJSONObject("progress")
        val cards = o.optJSONArray("reviewCards") ?: JSONArray()
        val mistakes = o.optJSONArray("mistakes") ?: JSONArray()
        return BackupData(
            version = o.optInt("version", 1),
            timestamp = o.optLong("timestamp"),
            xpTotal = p.optInt("xpTotal"),
            completedLessons = setFromJson(p.optJSONArray("completedLessons") ?: JSONArray()),
            solvedKeys = setFromJson(p.optJSONArray("solvedKeys") ?: JSONArray()),
            streakDays = p.optInt("streakDays"),
            longestStreak = p.optInt("longestStreak"),
            totalActiveDays = p.optInt("totalActiveDays"),
            lastActiveEpochDay = p.optLong("lastActiveEpochDay", -1L),
            dailyDate = p.optLong("dailyDate", -1L),
            dailyCount = p.optInt("dailyCount"),
            firstRunDone = p.optBoolean("firstRunDone"),
            nightOwl = p.optBoolean("nightOwl"),
            terminalHead = p.optBoolean("terminalHead"),
            badges = setFromJson(p.optJSONArray("badges") ?: JSONArray()),
            reviewCards = UserDataJson.decodeFromString(
                ListSerializer(ReviewCard.serializer()),
                cards.toString()
            ),
            mistakes = UserDataJson.decodeFromString(
                ListSerializer(MistakeRecord.serializer()),
                mistakes.toString()
            )
        )
    }

    private fun saveBackup(context: Context, json: String, fileName: String): Uri {
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
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: throw IllegalStateException("无法打开输出流")
        } catch (e: Exception) {
            // 写失败就把 MediaStore 里的半成品删掉，别留下空文件。
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    fun launchImportPicker(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
        }
        activity.startActivityForResult(intent, requestCode)
    }
}

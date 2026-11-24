package com.pyneon.academy.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "pyneon_progress")

enum class ActivityType(val key: String) {
    LESSON_OPEN("lesson_open"),
    LESSON_DONE("lesson_done"),
    EXERCISE("exercise_solved"),
    CHALLENGE("challenge_solved")
}

data class ActivityRecord(
    val epochMs: Long,
    val type: ActivityType,
    val refId: String
)

data class Progress(
    val xpTotal: Int = 0,
    val completedLessons: Set<String> = emptySet(),
    val solvedKeys: Set<String> = emptySet(),
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val totalActiveDays: Int = 0,
    val lastActiveEpochDay: Long = -1L,
    val dailyDate: Long = -1L,
    val dailyCount: Int = 0,
    val firstRunDone: Boolean = false,
    val nightOwl: Boolean = false,
    val terminalHead: Boolean = false,
    val badges: Set<String> = emptySet(),
    val recentActivities: List<ActivityRecord> = emptyList(),
    /** 最近打开的课程 id（断点续读）。空串表示从未打开过任何课。 */
    val lastLessonId: String = ""
)

object ProgressStore {
    private val KEY_XP = intPreferencesKey("xp_total")
    private val KEY_DONE_LESSONS = stringSetPreferencesKey("done_lessons")
    private val KEY_SOLVED = stringSetPreferencesKey("solved_keys")
    private val KEY_STREAK = intPreferencesKey("streak_days")
    private val KEY_LONGEST_STREAK = intPreferencesKey("longest_streak")
    private val KEY_TOTAL_ACTIVE_DAYS = intPreferencesKey("total_active_days")
    private val KEY_LAST_ACTIVE = longPreferencesKey("last_active_epoch_day")
    private val KEY_DAILY_DATE = longPreferencesKey("daily_date")
    private val KEY_DAILY_COUNT = intPreferencesKey("daily_count")
    private val KEY_FIRST_RUN = booleanPreferencesKey("first_run_done")
    private val KEY_NIGHT_OWL = booleanPreferencesKey("night_owl")
    private val KEY_LAST_LESSON = stringPreferencesKey("last_lesson_id")
    private val KEY_TERMINAL_HEAD = booleanPreferencesKey("terminal_head")
    private val KEY_BADGES = stringSetPreferencesKey("badges")
    private val KEY_ACTIVITY_LOG = stringSetPreferencesKey("activity_log")

    private const val DAILY_BONUS_XP = 30
    private const val MAX_ACTIVITY = 50

    fun flow(context: Context): Flow<Progress> =
        context.applicationContext.progressDataStore.data.map { p -> p.toProgress() }

    suspend fun snapshot(context: Context): Progress =
        context.applicationContext.progressDataStore.data.first().toProgress()

    /**
     * 读取「最近打开的课程 id」（断点续读）。
     *
     * 为什么独立提供：首页只需要这一个字段，snapshot 会把全部偏好读一遍。
     * 该键由 [markLessonOpened] 在进入课程详情页时写入。
     */
    suspend fun lastOpenedLesson(context: Context): String? =
        context.applicationContext.progressDataStore.data.first()[KEY_LAST_LESSON]
            ?.takeIf { it.isNotBlank() }

    private fun Preferences.toProgress(): Progress = Progress(
        xpTotal = this[KEY_XP] ?: 0,
        completedLessons = this[KEY_DONE_LESSONS] ?: emptySet(),
        solvedKeys = this[KEY_SOLVED] ?: emptySet(),
        streakDays = this[KEY_STREAK] ?: 0,
        longestStreak = this[KEY_LONGEST_STREAK] ?: 0,
        totalActiveDays = this[KEY_TOTAL_ACTIVE_DAYS] ?: 0,
        lastActiveEpochDay = this[KEY_LAST_ACTIVE] ?: -1L,
        dailyDate = this[KEY_DAILY_DATE] ?: -1L,
        dailyCount = this[KEY_DAILY_COUNT] ?: 0,
        firstRunDone = this[KEY_FIRST_RUN] ?: false,
        nightOwl = this[KEY_NIGHT_OWL] ?: false,
        terminalHead = this[KEY_TERMINAL_HEAD] ?: false,
        badges = this[KEY_BADGES] ?: emptySet(),
        recentActivities = parseActivities(this[KEY_ACTIVITY_LOG] ?: emptySet()),
        lastLessonId = this[KEY_LAST_LESSON] ?: ""
    )

    private fun parseActivities(raw: Set<String>): List<ActivityRecord> =
        raw.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            val type = ActivityType.entries.firstOrNull { it.key == parts[1] } ?: return@mapNotNull null
            ActivityRecord(
                epochMs = parts[0].toLongOrNull() ?: return@mapNotNull null,
                type = type,
                refId = parts[2]
            )
        }.sortedByDescending { it.epochMs }

    private fun appendActivity(
        log: Set<String>,
        type: ActivityType,
        refId: String,
        epochMs: Long
    ): Set<String> {
        val line = "$epochMs|${type.key}|$refId"
        val next = (log + line)
            .mapNotNull { raw -> raw.split("|").firstOrNull()?.toLongOrNull()?.let { it to raw } }
            .sortedByDescending { it.first }
            .take(MAX_ACTIVITY)
            .map { it.second }
            .toSet()
        return next.ifEmpty { log }
    }

    suspend fun touchStreak(context: Context, todayEpochDay: Long) {
        context.applicationContext.progressDataStore.edit { p ->
            val last = p[KEY_LAST_ACTIVE] ?: -1L
            val current = p[KEY_STREAK] ?: 0
            val next = when {
                last == todayEpochDay -> current.coerceAtLeast(1)
                last == todayEpochDay - 1 -> current + 1
                else -> 1
            }
            p[KEY_STREAK] = next
            p[KEY_LAST_ACTIVE] = todayEpochDay
        }
    }

    suspend fun markExerciseSolved(context: Context, key: String, xp: Int, todayEpochDay: Long) {
        context.applicationContext.progressDataStore.edit { p ->
            val solved = p[KEY_SOLVED] ?: emptySet()
            if (key !in solved) {
                p[KEY_SOLVED] = solved + key
                p[KEY_XP] = (p[KEY_XP] ?: 0) + xp
            }
            val date = p[KEY_DAILY_DATE] ?: -1L
            if (date != todayEpochDay) {
                p[KEY_XP] = (p[KEY_XP] ?: 0) + DAILY_BONUS_XP
                p[KEY_DAILY_DATE] = todayEpochDay
                p[KEY_DAILY_COUNT] = 1
            } else {
                p[KEY_DAILY_COUNT] = (p[KEY_DAILY_COUNT] ?: 0) + 1
            }
            p[KEY_ACTIVITY_LOG] = appendActivity(
                p[KEY_ACTIVITY_LOG] ?: emptySet(),
                if (key.startsWith("chal_")) ActivityType.CHALLENGE else ActivityType.EXERCISE,
                key,
                System.currentTimeMillis()
            )
        }
    }

    suspend fun markLessonDone(context: Context, lessonId: String, lessonXp: Int, lastLessonId: String? = null) {
        context.applicationContext.progressDataStore.edit { p ->
            val done = p[KEY_DONE_LESSONS] ?: emptySet()
            if (lessonId !in done) {
                p[KEY_DONE_LESSONS] = done + lessonId
                p[KEY_XP] = (p[KEY_XP] ?: 0) + lessonXp
            }
            if (lastLessonId != null) p[KEY_LAST_LESSON] = lastLessonId
            p[KEY_ACTIVITY_LOG] = appendActivity(
                p[KEY_ACTIVITY_LOG] ?: emptySet(),
                ActivityType.LESSON_DONE,
                lessonId,
                System.currentTimeMillis()
            )
        }
    }

    suspend fun markLessonOpened(context: Context, lessonId: String) {
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_LAST_LESSON] = lessonId
            p[KEY_ACTIVITY_LOG] = appendActivity(
                p[KEY_ACTIVITY_LOG] ?: emptySet(),
                ActivityType.LESSON_OPEN,
                lessonId,
                System.currentTimeMillis()
            )
        }
    }

    suspend fun markFirstRun(context: Context) {
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_FIRST_RUN] = true
        }
    }

    suspend fun markNightRun(context: Context, hourOfDay: Int) {
        if (hourOfDay !in 0..5) return
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_NIGHT_OWL] = true
        }
    }

    suspend fun markTerminalHead(context: Context) {
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_TERMINAL_HEAD] = true
        }
    }

    suspend fun setBadges(context: Context, badges: Set<String>) {
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_BADGES] = badges
        }
    }

    suspend fun updateStreakStats(context: Context, streakDays: Int, longestStreak: Int, totalActiveDays: Int) {
        context.applicationContext.progressDataStore.edit { p ->
            p[KEY_STREAK] = streakDays
            p[KEY_LONGEST_STREAK] = longestStreak
            p[KEY_TOTAL_ACTIVE_DAYS] = totalActiveDays
        }
    }

    suspend fun restoreFromBackup(context: Context, xpTotal: Int, completedLessons: Set<String>, solvedKeys: Set<String>, streakDays: Int, lastActiveEpochDay: Long, dailyDate: Long, dailyCount: Int, firstRunDone: Boolean, nightOwl: Boolean, terminalHead: Boolean, badges: Set<String>) {
        context.applicationContext.progressDataStore.edit { prefs ->
            prefs[KEY_XP] = xpTotal
            prefs[KEY_DONE_LESSONS] = completedLessons
            prefs[KEY_SOLVED] = solvedKeys
            prefs[KEY_STREAK] = streakDays
            prefs[KEY_LAST_ACTIVE] = lastActiveEpochDay
            prefs[KEY_DAILY_DATE] = dailyDate
            prefs[KEY_DAILY_COUNT] = dailyCount
            prefs[KEY_FIRST_RUN] = firstRunDone
            prefs[KEY_NIGHT_OWL] = nightOwl
            prefs[KEY_TERMINAL_HEAD] = terminalHead
            prefs[KEY_BADGES] = badges
        }
    }

    suspend fun clearActivityLog(context: Context) {
        context.applicationContext.progressDataStore.edit { p ->
            p.remove(KEY_ACTIVITY_LOG)
        }
    }

    suspend fun reset(context: Context) {
        val app = context.applicationContext
        app.progressDataStore.edit { it.clear() }
        DraftStore.clearAll(app)
        val failures = withContext(Dispatchers.IO) { UserDataFiles.deleteAll(app.filesDir) }
        if (failures.isNotEmpty()) {
            Log.w("ProgressStore", "reset 未能删除：${failures.joinToString()}")
        }
    }
}

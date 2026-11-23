package com.pyneon.academy.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// 错题记录
@Serializable
data class MistakeRecord(
    val id: Int = 0,
    val lessonId: String,
    val blockType: String,
    val blockIndex: Int,
    val userCode: String,
    val expectedOutput: String,
    val actualOutput: String,
    val errorMessage: String,
    val conceptTags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

// 复习卡
@Serializable
data class ReviewCard(
    val id: String,
    val lessonId: String,
    val blockType: String,
    val blockIndex: Int,
    val question: String,
    val answer: String,
    val difficulty: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewDate: Long = 0L,
    val lapses: Int = 0
)

private const val TAG = "ViewModels"

class StreakViewModel(app: Application) : AndroidViewModel(app) {
    private val _streak = MutableStateFlow(StreakData())
    val streak: StateFlow<StreakData> = _streak.asStateFlow()

    init {
        loadStreak()
    }

    private fun loadStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            val progress = ProgressStore.snapshot(getApplication())
            _streak.value = StreakData(
                currentStreak = progress.streakDays,
                longestStreak = progress.longestStreak,
                totalActiveDays = progress.totalActiveDays,
                lastActiveDate = progress.lastActiveEpochDay,
                badges = progress.badges
            )
        }
    }

    fun onAppOpen() {
        viewModelScope.launch(Dispatchers.IO) {
            // 用本地日期算天序号。拿毫秒除 86400000 在跨时区和夏令时会差一天，
            // 连击就白断了。
            val today = Clock.todayEpochDay()
            val progress = ProgressStore.snapshot(getApplication())
            val last = progress.lastActiveEpochDay
            val context = getApplication<Application>()

            if (last == -1L) {
                // 第一次打开。
                ProgressStore.touchStreak(context, today)
                // 首日把最长和总天数一起写上。
                updateProgressForNewStreak(context)
                // 首日也要发徽章。以前这条分支没调 updateBadges，第一次打开拿不到首日。
                updateBadges(context, 1)
                loadStreak()
                return@launch
            }

            val diff = today - last
            when {
                diff == 0L -> {} // 今天已经记过。
                diff == 1L -> {
                    ProgressStore.touchStreak(context, today)
                    // 连击加一，顺便更新最长和总天数。
                    val newCurrent = progress.streakDays + 1
                    updateLongestAndTotal(context, newCurrent, progress.totalActiveDays + 1)
                    updateBadges(context, newCurrent)
                    loadStreak()
                }
                else -> {
                    // 断档了，从 1 重新开始。
                    ProgressStore.touchStreak(context, today)
                    updateLongestAndTotal(context, 1, progress.totalActiveDays + 1)
                    loadStreak()
                }
            }
        }
    }

    private suspend fun updateProgressForNewStreak(context: Application) {
        ProgressStore.updateStreakStats(context, 1, 1, 1)
    }

    private suspend fun updateLongestAndTotal(context: Application, newCurrent: Int, newTotal: Int) {
        val progress = ProgressStore.snapshot(context)
        val newLongest = maxOf(progress.longestStreak, newCurrent)
        ProgressStore.updateStreakStats(context, newCurrent, newLongest, newTotal)
    }

    // 补徽章。以前每次拿空集合整体覆盖，断档重来会把 30 天徽章抹掉。
    // 徽章是历史成就，只增不减，所以按最长连击补，再和已有的取并集。
    private suspend fun updateBadges(context: Application, current: Int) {
        val progress = ProgressStore.snapshot(context)
        // 断档后 current 归 1，最长连击还在 longestStreak 里。
        val peak = maxOf(current, progress.longestStreak)
        val badges = progress.badges.toMutableSet()
        if (peak >= 1) badges.add("first_day")
        if (peak >= 3) badges.add("three_days")
        if (peak >= 7) badges.add("week_warrior")
        if (peak >= 14) badges.add("fortnight")
        if (peak >= 30) badges.add("month_master")
        if (peak >= 100) badges.add("centurion")
        ProgressStore.setBadges(context, badges)
    }

    suspend fun getDueReviewCount(): Int {
        val cards = readReviewCards(getApplication())
        val now = System.currentTimeMillis()
        return cards.count { it.nextReviewDate <= now }
    }
}

@Serializable
data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalActiveDays: Int = 0,
    val lastActiveDate: Long = -1L,
    val badges: Set<String> = emptySet()
) {
    val badgesUnlocked: String = badges.joinToString(",")
}

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val _dueCards = MutableStateFlow<List<ReviewCard>>(emptyList())
    val dueCards: StateFlow<List<ReviewCard>> = _dueCards.asStateFlow()

    fun loadDueCards(limit: Int = 10) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = readReviewCards(getApplication())
            val now = System.currentTimeMillis()
            _dueCards.value = all.filter { it.nextReviewDate <= now }.take(limit)
        }
    }

    fun answerCard(card: ReviewCard, quality: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ReviewScheduler.schedule(card.toSchedulable(), quality)
            val updated = card.copy(
                difficulty = result.newEase,
                interval = result.newInterval,
                repetitions = result.newRepetitions,
                nextReviewDate = result.nextReviewDate,
                lastReviewDate = System.currentTimeMillis(),
                lapses = result.lapses
            )
            val all = readReviewCards(getApplication()).map { if (it.id == card.id) updated else it }
            writeReviewCards(getApplication(), all)
            loadDueCards()
        }
    }

    fun generateCardsForLesson(lesson: Lesson) {
        viewModelScope.launch(Dispatchers.IO) {
            // 只补缺的卡。以前把整课旧卡删掉重写，用户复习过的间隔和次数全没了。
            val existing = readReviewCards(getApplication())
            val existingIds = existing.map { it.id }.toSet()
            val missing = ReviewScheduler.createInitialCards(lesson).filter { it.id !in existingIds }
            if (missing.isNotEmpty()) {
                writeReviewCards(getApplication(), existing + missing)
            }
        }
    }
}

class MistakeViewModel(app: Application) : AndroidViewModel(app) {
    private val _allMistakes = MutableStateFlow<List<MistakeRecord>>(emptyList())
    val allMistakes: StateFlow<List<MistakeRecord>> = _allMistakes.asStateFlow()

    init {
        loadMistakes()
    }

    fun getByLesson(lessonId: String): Flow<List<MistakeRecord>> =
        _allMistakes.map { all -> all.filter { it.lessonId == lessonId } }

    fun recordMistake(
        lessonId: String,
        blockType: String,
        blockIndex: Int,
        userCode: String,
        expected: String,
        actual: String,
        error: String,
        conceptTags: List<String> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val mistakes = readMistakes(getApplication())
            val newId = (mistakes.maxOfOrNull { it.id } ?: 0) + 1
            val mistake = MistakeRecord(
                id = newId,
                lessonId = lessonId,
                blockType = blockType,
                blockIndex = blockIndex,
                userCode = userCode,
                expectedOutput = expected,
                actualOutput = actual,
                errorMessage = error,
                conceptTags = conceptTags
            )
            writeMistakes(getApplication(), mistakes + mistake)
            _allMistakes.value = mistakes + mistake
        }
    }

    suspend fun getWeakConcepts(): List<Pair<String, Int>> {
        val mistakes = readMistakes(getApplication())
        return mistakes.flatMap { it.conceptTags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }
    }

    fun deleteMistake(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val mistakes = readMistakes(getApplication()).filter { it.id != id }
            writeMistakes(getApplication(), mistakes)
            _allMistakes.value = mistakes
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            writeMistakes(getApplication(), emptyList())
            _allMistakes.value = emptyList()
        }
    }

    private fun loadMistakes() {
        viewModelScope.launch(Dispatchers.IO) {
            _allMistakes.value = readMistakes(getApplication())
        }
    }
}

private fun readMistakes(context: Application): List<MistakeRecord> =
    UserDataFiles.mistakesStore(context).read()

private fun writeMistakes(context: Application, mistakes: List<MistakeRecord>) {
    try {
        UserDataFiles.mistakesStore(context).write(mistakes)
    } catch (e: Exception) {
        Log.e(TAG, "保存错题失败", e)
    }
}

private fun readReviewCards(context: Application): List<ReviewCard> =
    UserDataFiles.reviewCardsStore(context).read()

private fun writeReviewCards(context: Application, cards: List<ReviewCard>) {
    try {
        UserDataFiles.reviewCardsStore(context).write(cards)
    } catch (e: Exception) {
        Log.e(TAG, "保存复习卡失败", e)
    }
}

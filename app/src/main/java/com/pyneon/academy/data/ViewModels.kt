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

// --- Mistake data model ---
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

// --- Review card data model ---
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
            // 复用 Clock.todayEpochDay()（本地日期 → 天序号，与 java.time.toEpochDay 等价）；
            // 不能用 millis / 86_400_000 折算，跨时区与夏令时会差一天导致连击清零。
            val today = Clock.todayEpochDay()
            val progress = ProgressStore.snapshot(getApplication())
            val last = progress.lastActiveEpochDay
            val context = getApplication<Application>()

            if (last == -1L) {
                // First time ever
                ProgressStore.touchStreak(context, today)
                // Set initial longest and total
                updateProgressForNewStreak(context, today)
                // 首日也要发 first_day 徽章（旧实现走这条路时不调用 updateBadges，
                // 用户第一次打开永远拿不到首日徽章 —— 2026-11 审查发现）
                updateBadges(context, 1)
                loadStreak()
                return@launch
            }

            val diff = today - last
            when {
                diff == 0L -> {} // already counted today
                diff == 1L -> {
                    ProgressStore.touchStreak(context, today)
                    // Increment total active days and possibly longest
                    val newCurrent = progress.streakDays + 1
                    updateLongestAndTotal(context, newCurrent, progress.totalActiveDays + 1)
                    updateBadges(context, newCurrent)
                    loadStreak()
                }
                else -> {
                    // Streak broken, reset to 1
                    ProgressStore.touchStreak(context, today)
                    updateLongestAndTotal(context, 1, progress.totalActiveDays + 1)
                    loadStreak()
                }
            }
        }
    }

    private suspend fun updateProgressForNewStreak(context: Application, today: Long) {
        val progress = ProgressStore.snapshot(context)
        ProgressStore.updateStreakStats(context, 1, 1, 1)
        // Also need to update lastActiveEpochDay separately since touchStreak already did it
    }

    private suspend fun updateLongestAndTotal(context: Application, newCurrent: Int, newTotal: Int) {
        val progress = ProgressStore.snapshot(context)
        val newLongest = maxOf(progress.longestStreak, newCurrent)
        ProgressStore.updateStreakStats(context, newCurrent, newLongest, newTotal)
    }

    /**
     * 补发连击徽章。
     *
     * 旧实现每次新建一个空集合并整体覆盖，导致「曾连击 30 天、断档后重新开始」
     * 会把 week_warrior / month_master / centurion 永久抹掉（2026-11 审查发现）。
     * 正确语义是：徽章是历史成就，只增不减。这里按历史最长连击补发，
     * 并与已存徽章取并集。
     */
    private suspend fun updateBadges(context: Application, current: Int) {
        val progress = ProgressStore.snapshot(context)
        // 断档后 current 会重置为 1，但历史最长连击仍保留在 longestStreak 里
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
        val cards = loadReviewCards()
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
            val all = loadReviewCards()
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
            saveReviewCard(updated)
            loadDueCards()
        }
    }

    fun generateCardsForLesson(lesson: Lesson) {
        viewModelScope.launch(Dispatchers.IO) {
            // B3: 只在缺失时补充该课卡片；旧实现 filter 掉该课全部旧卡再重写，
            // 会覆盖用户已推进的复习进度（interval/repetitions）。
            val existing = loadReviewCards()
            val existingIds = existing.map { it.id }.toSet()
            val missing = ReviewScheduler.createInitialCards(lesson).filter { it.id !in existingIds }
            if (missing.isNotEmpty()) {
                saveAllReviewCards(existing + missing)
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
            val mistakes = loadMistakesInternal()
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
            saveMistakesInternal(mistakes + mistake)
            _allMistakes.value = mistakes + mistake
        }
    }

    suspend fun getWeakConcepts(): List<Pair<String, Int>> {
        val mistakes = loadMistakesInternal()
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
            val mistakes = loadMistakesInternal().filter { it.id != id }
            saveMistakesInternal(mistakes)
            _allMistakes.value = mistakes
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            saveMistakesInternal(emptyList())
            _allMistakes.value = emptyList()
        }
    }

    private fun loadMistakes() {
        viewModelScope.launch(Dispatchers.IO) {
            _allMistakes.value = loadMistakesInternal()
        }
    }
}

// --- JSON file I/O helpers ---
// 2026-11 去重：读/写/原子替换统一收敛到 UserDataFiles + JsonListFile，
// 错题本、复习卡、备份三条链路共用同一套字段映射，避免各自维护一份。
private fun loadMistakesInternal(context: Application? = null): List<MistakeRecord> =
    context?.let { UserDataFiles.mistakesStore(it).read() } ?: emptyList()

private fun saveMistakesInternal(mistakes: List<MistakeRecord>, context: Application? = null) {
    if (context == null) return
    try {
        UserDataFiles.mistakesStore(context).write(mistakes)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save mistakes", e)
    }
}

fun MistakeViewModel.loadMistakesInternal(): List<MistakeRecord> =
    loadMistakesInternal(getApplication())

fun MistakeViewModel.saveMistakesInternal(mistakes: List<MistakeRecord>) =
    saveMistakesInternal(mistakes, getApplication())

private fun loadReviewCards(context: Application? = null): List<ReviewCard> =
    context?.let { UserDataFiles.reviewCardsStore(it).read() } ?: emptyList()

private fun saveReviewCards(cards: List<ReviewCard>, context: Application? = null) {
    if (context == null) return
    try {
        UserDataFiles.reviewCardsStore(context).write(cards)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save review cards", e)
    }
}

fun ReviewViewModel.loadReviewCards(): List<ReviewCard> =
    loadReviewCards(getApplication())

fun ReviewViewModel.saveReviewCard(card: ReviewCard) {
    val all = loadReviewCards()
    val updated = all.map { if (it.id == card.id) card else it }
    saveReviewCards(updated, getApplication())
}

fun ReviewViewModel.saveAllReviewCards(cards: List<ReviewCard>) =
    saveReviewCards(cards, getApplication())

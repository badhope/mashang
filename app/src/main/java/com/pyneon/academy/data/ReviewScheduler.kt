package com.pyneon.academy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReviewScheduler {
    private const val MIN_EASE = 1.3f
    private const val INITIAL_EASE = 2.5f
    private const val MAX_INTERVAL = 365 // days

    data class ReviewResult(
        val newEase: Float,
        val newInterval: Int,
        val newRepetitions: Int,
        val nextReviewDate: Long,
        val lapses: Int
    )

    // 调度只看这几个字段，跟存储层解耦。
    interface SchedulableCard {
        val difficulty: Float
        val interval: Int
        val repetitions: Int
        val lapses: Int
    }

    suspend fun schedule(
        card: SchedulableCard,
        quality: Int, // 0=again, 1=hard, 2=good, 3=easy
        now: Long = System.currentTimeMillis()
    ): ReviewResult = withContext(Dispatchers.Default) {
        var ease = card.difficulty
        var interval = card.interval
        var reps = card.repetitions
        var lapses = card.lapses

        when (quality) {
            0 -> { // Again - complete blackout
                ease = maxOf(ease - 0.2f, MIN_EASE)
                interval = 0
                reps = 0
                lapses++
            }
            1 -> { // Hard
                ease = maxOf(ease - 0.15f, MIN_EASE)
                interval = if (reps == 0) 1 else if (reps == 1) 6 else maxOf((interval * 1.2).toInt(), 1)
                reps++
            }
            2 -> { // Good
                interval = when (reps) {
                    0 -> 1
                    1 -> 6
                    else -> minOf((interval * ease).toInt(), MAX_INTERVAL)
                }
                reps++
            }
            3 -> { // Easy
                ease = ease + 0.15f
                interval = when (reps) {
                    0 -> 4
                    1 -> 10
                    else -> minOf((interval * ease * 1.3).toInt(), MAX_INTERVAL)
                }
                reps++
            }
        }

        val nextReview = now + (interval * 24L * 60 * 60 * 1000)
        ReviewResult(ease, interval, reps, nextReview, lapses)
    }

    fun createInitialCards(lesson: Lesson): List<ReviewCard> {
        val cards = mutableListOf<ReviewCard>()
        val now = System.currentTimeMillis()

        lesson.blocks.forEachIndexed { idx, block ->
            when (block) {
                is Block.Quiz -> {
                    // 一个 quiz 只生成一张卡，题目加正确选项。
                    // 以前每个选项生成一张，三道选项就出三张差不多的卡。
                    cards.add(ReviewCard(
                        id = "${lesson.id}:quiz:$idx",
                        lessonId = lesson.id,
                        blockType = "quiz",
                        blockIndex = idx,
                        question = block.question,
                        answer = block.options[block.answerIndex],
                        nextReviewDate = now
                    ))
                }
                is Block.Fill -> {
                    cards.add(ReviewCard(
                        id = "${lesson.id}:fill:$idx",
                        lessonId = lesson.id,
                        blockType = "fill",
                        blockIndex = idx,
                        question = block.goal,
                        answer = block.answer,
                        nextReviewDate = now
                    ))
                }
                is Block.Order -> {
                    cards.add(ReviewCard(
                        id = "${lesson.id}:order:$idx",
                        lessonId = lesson.id,
                        blockType = "order",
                        blockIndex = idx,
                        question = block.title,
                        answer = block.lines.joinToString("\n"),
                        nextReviewDate = now
                    ))
                }
                else -> {} // 其余 Block 类型不生成复习卡
            }
        }

        // 练习题也做成复习卡
        lesson.exercise?.let { exercise ->
            cards.add(ReviewCard(
                id = "${lesson.id}:exercise:0",
                lessonId = lesson.id,
                blockType = "exercise",
                blockIndex = -1,
                question = exercise.title,
                answer = exercise.hint,
                nextReviewDate = now
            ))
        }

        return cards
    }
}

// ReviewCard 实现调度接口
fun ReviewCard.toSchedulable(): ReviewScheduler.SchedulableCard = object : ReviewScheduler.SchedulableCard {
    override val difficulty: Float get() = this@toSchedulable.difficulty
    override val interval: Int get() = this@toSchedulable.interval
    override val repetitions: Int get() = this@toSchedulable.repetitions
    override val lapses: Int get() = this@toSchedulable.lapses
}

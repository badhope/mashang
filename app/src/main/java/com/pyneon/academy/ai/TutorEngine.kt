package com.pyneon.academy.ai

import com.pyneon.academy.data.Block
import com.pyneon.academy.data.Lesson

/**
 * 课程页和终端共用的离线助教。
 *
 * 它不联网，也不假装自己是大模型：所有回答都从当前题目、运行结果和课程正文里
 * 现找现拼。目标是把人往下一步推，而不是把答案递到手里。
 */
enum class TutorIntent {
    THINKING,
    ERROR,
    CONCEPT,
    NEXT_STEP,
    GENERAL
}

data class TutorSource(
    val lessonId: String,
    val title: String,
    val text: String
)

data class TutorRequest(
    val question: String,
    val lessonId: String = "",
    val lessonTitle: String = "",
    val lessonBrief: String? = null,
    val starterCode: String? = null,
    val currentCode: String = "",
    val tests: List<String> = emptyList(),
    val hint: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val traceback: String? = null,
    val passed: Boolean? = null,
    val nextLessonTitle: String? = null,
    val sources: List<TutorSource> = emptyList()
)

data class TutorReply(
    val intent: TutorIntent,
    val title: String,
    val lead: String,
    val steps: List<String>,
    val checkpoint: String,
    val example: String? = null,
    val sourceTitle: String? = null,
    val sourceText: String? = null
)

object TutorEngine {

    private val ERROR_HINTS = listOf("报错", "错误", "为什么错", "跑不通", "失败", "异常", "崩溃", "红了")
    private val THINKING_HINTS = listOf("怎么开始", "怎么做", "没思路", "不会", "卡住", "从哪", "思路", "拆解")
    private val CONCEPT_HINTS = listOf("什么是", "是什么", "解释", "区别", "原理", "定义")
    private val NEXT_HINTS = listOf("下一步", "接下来", "学什么", "然后学", "下一讲")
    private val FUNCTION_PATTERN = Regex("""(?:def|fun|function)\s+([A-Za-z_][A-Za-z0-9_]*)""")

    // 这些词在课程里到处都是，留着会把“没找到”硬凑成“找到”。
    private val STOP_TERMS = setOf(
        "什么", "么是", "怎么", "为何", "为什么", "如何", "这个", "那个",
        "一下", "可以", "是否", "介绍", "解释", "区别", "原理", "定义"
    )

    fun reply(request: TutorRequest): TutorReply {
        val question = request.question.trim()
        val lower = question.lowercase()
        return when {
            request.hasError() && ERROR_HINTS.any { lower.contains(it) } -> diagnose(request)
            THINKING_HINTS.any { lower.contains(it) } -> thinking(request)
            CONCEPT_HINTS.any { lower.contains(it) } -> concept(request)
            NEXT_HINTS.any { lower.contains(it) } -> nextStep(request)
            question.isBlank() && request.lessonBrief != null -> thinking(request)
            else -> general(request)
        }
    }

    private fun TutorRequest.hasError(): Boolean =
        errorType != null || errorMessage != null || passed == false

    private fun diagnose(request: TutorRequest): TutorReply {
        val tip = CoachEngine.analyze(
            errorType = request.errorType,
            errorMessage = request.errorMessage,
            traceback = request.traceback,
            code = request.currentCode
        )
        val steps = tip.steps.toMutableList()
        steps += "只改一处，再用同一条用例重跑；一次动三个地方，下次还是不知道哪个改对了。"
        return TutorReply(
            intent = TutorIntent.ERROR,
            title = "报错诊断：${tip.title}",
            lead = tip.summary,
            steps = steps,
            checkpoint = "能指出报错行、改了什么、重跑结果是否变化，这三个都说清楚再往下走。",
            example = tip.example
        )
    }

    private fun thinking(request: TutorRequest): TutorReply {
        val code = request.currentCode.ifBlank { request.starterCode.orEmpty() }
        val functionName = FUNCTION_PATTERN.find(code)?.groupValues?.getOrNull(1)
        val firstTest = request.tests.firstOrNull()?.trim()?.take(100)
        val steps = buildList {
            add("先读题：把输入、要返回的东西、边界条件各写一行，别急着动键盘。")
            if (functionName != null) {
                add("骨架里的入口是 $functionName(...)。先确认参数和返回值，函数签名不要改。")
            }
            add("用题目给的最小输入手算一遍，把中间值写在注释里。手算卡住，说明还没读懂题。")
            if (firstTest != null) {
                add("第一条断言是 `$firstTest`。先让它通过，再管后面的边界。")
            }
            add("先写能跑通的最小版本，不要一上来处理所有异常输入。")
            add("运行后看变量快照，对照手算结果。对不上就先停，不要继续往下堆代码。")
        }
        return TutorReply(
            intent = TutorIntent.THINKING,
            title = "先拆题，再写代码",
            lead = "这题按“读入—处理—返回”三段拆。你现在卡在哪一段，就从哪一段开始。",
            steps = steps,
            checkpoint = "能在注释里写出三步计划，并且手算出一组最小输入，再开始写。",
            example = """
                # 先用题目最小样例走一遍
                sample = ...
                # 把处理结果打印出来，确认中间值
                print(sample)
            """.trimIndent()
        )
    }

    private fun concept(request: TutorRequest): TutorReply {
        val hits = search(request.question, request.sources, request.lessonId)
        if (hits.isEmpty()) {
            return TutorReply(
                intent = TutorIntent.CONCEPT,
                title = "课程里没有直接解释",
                lead = "这个问题在当前课程包里没找到对应段落。先别猜，回到已经学过的例子找一个最接近的。",
                steps = listOf(
                    "把问题拆成“它解决什么问题、输入是什么、输出是什么”三句。",
                    "去当前这讲的代码块里找一次实际调用，先看它怎么用。",
                    "如果还是对不上，把具体代码片段发我，我按代码里的变量名讲。"
                ),
                checkpoint = "能说出一个你见过的使用场景，而不是只会背定义。"
            )
        }
        val hit = hits.first()
        return TutorReply(
            intent = TutorIntent.CONCEPT,
            title = "课程里的解释：${hit.title}",
            lead = "先读原文，再用自己的话回答两个问题：它解决什么，什么时候不该用。",
            steps = listOf(
                "把下面原文里的关键词圈出来，别整段背。",
                "用一句自己的话解释它，尽量不用原文句子。",
                "回到当前练习，找一个能用到它的位置；找不到就说明这题不需要它。",
                "如果原文和题目对不上，把冲突的地方发我，不要硬套。"
            ),
            checkpoint = "能举一个例子，也能举一个反例，才算真的理解。",
            sourceTitle = hit.title,
            sourceText = hit.text.take(520).trim()
        )
    }

    private fun nextStep(request: TutorRequest): TutorReply {
        val next = request.nextLessonTitle?.trim().orEmpty()
        val steps = buildList {
            add("当前这讲先做到能独立复现：合上讲解，自己重写一遍关键代码。")
            add("去错题本把这讲的失败记录重做一次，确认不是碰巧通过。")
            if (next.isNotBlank()) {
                add("下一讲是《$next》。先看标题猜它会解决什么问题，再打开，不要直接抄代码。")
            } else {
                add("主线已经走完，去复习台按到期卡片过一轮，再挑一道挑战题。")
            }
            add("今天只追一个目标：能讲清楚自己为什么这么写，而不是多刷几题。")
        }
        return TutorReply(
            intent = TutorIntent.NEXT_STEP,
            title = if (next.isNotBlank()) "下一站：$next" else "主线之后：复习与挑战",
            lead = "进度不是目的，能独立写出来才是。先把当前这讲收干净。",
            steps = steps,
            checkpoint = "不看答案，能在 10 分钟内重写当前练习并通过判题。"
        )
    }

    private fun general(request: TutorRequest): TutorReply {
        val hasCode = request.currentCode.isNotBlank()
        val steps = buildList {
            if (!hasCode) {
                add("先把题目样例写成变量，写一行 print，确认输入是对的。")
                add("再写处理逻辑，每次只加一个步骤。")
            } else {
                add("先判断卡在哪一类：读题、语法、逻辑、输出格式。四类的修法不一样。")
                add("把最近一次完整输出贴出来，不要只贴最后一行。")
                add("把代码缩到最小：保留能复现问题的最少几行，其他先注释掉。")
            }
            add("每次只改一个地方，改完立刻重跑；不要攒一堆改动再一起运行。")
        }
        return TutorReply(
            intent = TutorIntent.GENERAL,
            title = "先把问题缩小",
            lead = "我没法替你写答案，但可以帮你把问题圈到最小范围。",
            steps = steps,
            checkpoint = "能用一句话说出“我现在卡在哪个具体步骤”，再继续。"
        )
    }

    private fun search(
        question: String,
        sources: List<TutorSource>,
        currentLessonId: String
    ): List<TutorSource> {
        val terms = searchTerms(question)
        if (terms.isEmpty()) return emptyList()
        return sources.asSequence()
            .map { source ->
                var score = 0
                if (source.title.contains(question, ignoreCase = true)) score += 6
                if (source.text.contains(question, ignoreCase = true)) score += 8
                for (term in terms) {
                    score += source.title.countOccurrences(term) * 4
                    score += source.text.countOccurrences(term)
                }
                if (source.lessonId == currentLessonId) score += 2
                source to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()
    }

    private fun searchTerms(question: String): List<String> {
        val cleaned = question.lowercase()
            .replace(Regex("[\\s，。？！、；：,.?!;:（）()\\[\\]【】]+"), " ")
        val latin = Regex("[a-z0-9_]{2,}").findAll(cleaned).map { it.value }.toList()
        val chinese = Regex("[\\u4e00-\\u9fa5]{2,}").findAll(cleaned).map { it.value }.toList()
        val grams = chinese.flatMap { word ->
            if (word.length <= 4) listOf(word) else word.windowed(2).take(8)
        }
        return (latin + grams)
            .filterNot { it in STOP_TERMS }
            .distinct()
            .take(16)
    }

    private fun String.countOccurrences(term: String): Int {
        if (term.isBlank() || length < term.length) return 0
        var count = 0
        var index = indexOf(term, ignoreCase = true)
        while (index >= 0) {
            count += 1
            index = indexOf(term, index + term.length, ignoreCase = true)
        }
        return count
    }
}

/**
 * 把课程正文压成可检索的纯文本。代码只留短片段，避免整段答案把检索结果带偏。
 */
object TutorKnowledge {

    fun fromLessons(lessons: List<Lesson>): List<TutorSource> = lessons.map { lesson ->
        TutorSource(
            lessonId = lesson.id,
            title = "第${lesson.order}讲 ${lesson.title}",
            text = blocksToText(lesson.blocks)
        )
    }

    private fun blocksToText(blocks: List<Block>): String = buildString {
        blocks.forEach { block ->
            when (block) {
                is Block.Heading -> appendLine("标题：${block.text}")
                is Block.Paragraph -> appendLine(block.text)
                is Block.Tip -> appendLine("提示：${block.text}")
                is Block.Warn -> appendLine("注意：${block.text}")
                is Block.CodeBlock -> appendLine("代码：\n${block.code.take(320)}")
                is Block.Output -> appendLine("输出：${block.text}")
                is Block.Table -> {
                    appendLine(block.headers.joinToString(" | "))
                    block.rows.forEach { appendLine(it.joinToString(" | ")) }
                }
                is Block.Diagram -> appendLine(block.text)
                is Block.Task -> appendLine("任务：${block.text}")
                is Block.Steps -> appendLine("步骤：${block.items.joinToString("；")}")
                is Block.Practice -> appendLine("练习 ${block.title}：${block.hint}")
                is Block.Fill -> appendLine("填空 ${block.goal}：${block.explain}")
                is Block.Order -> appendLine("排序 ${block.title}：${block.lines.joinToString(" / ")}")
                is Block.Quiz -> appendLine("测验：${block.question}；${block.explain}")
                is Block.Compare -> {
                    appendLine("对比 ${block.title}：")
                    block.columns.forEach { appendLine("${it.lang} ${it.note}") }
                }
                is Block.Visualize -> {
                    appendLine("图解 ${block.title}：${block.caption}")
                    block.frames.forEach { appendLine(it.label) }
                }
            }
        }
    }.trim()
}

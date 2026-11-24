package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyneon.academy.ai.CoachEngine
import com.pyneon.academy.ai.CoachTip
import com.pyneon.academy.ai.TutorKnowledge
import com.pyneon.academy.data.Block
import com.pyneon.academy.data.Clock
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.debug.DebugLog
import com.pyneon.academy.data.MistakeViewModel
import com.pyneon.academy.data.Progress
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.data.ReviewViewModel
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.ui.components.ConsoleResult
import com.pyneon.academy.ui.components.CodeEditor
import com.pyneon.academy.ui.components.PythonCodeField
import com.pyneon.academy.ui.components.SyntaxHintBar
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.SectionHeader
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.Bg1
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LessonDetailScreen(lessonId: String, onBack: () -> Unit, onNextLesson: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lesson = remember(lessonId) { LessonRepository.lesson(context, lessonId) }
    val progress by ProgressStore.flow(context).collectAsState(initial = Progress())
    val mistakeVm: MistakeViewModel = viewModel()
    val reviewVm: ReviewViewModel = viewModel()

    val runResults = remember(lessonId) { mutableStateMapOf<Int, RunResult>() }
    var runningKey by remember(lessonId) { mutableStateOf<Int?>(null) }

    val exercise = lesson?.exercise
    // 没写完的答案要能扛住旋转和切后台，key 带 lessonId，换课就换草稿。
    var editorValue by rememberSaveable(lessonId) {
        mutableStateOf(exercise?.starterCode ?: "")
    }
    // 判题结果和教练提示不保存，旋转后重判；草稿和提示展开状态会保存。
    var checkResult by remember(lessonId) { mutableStateOf<RunResult?>(null) }
    var coachTip by remember(lessonId) { mutableStateOf<CoachTip?>(null) }
    var tutorOpen by rememberSaveable(lessonId) { mutableStateOf(false) }
    var checking by remember(lessonId) { mutableStateOf(false) }
    var hintOpen by rememberSaveable(lessonId) { mutableStateOf(false) }
    var rewardXp by remember { mutableStateOf<Int?>(null) }

    val solvedKey = "les_$lessonId"
    val alreadySolved = solvedKey in progress.solvedKeys

    if (lesson == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Text("课程数据缺失", color = NeonMagenta)
        }
        return
    }

    // 代码块和跟做示例都走这里，别各写一份。
    fun runBlock(index: Int, code: String, lang: String, stdin: List<String>) {
        scope.launch {
            runningKey = index
            val r = try {
                withContext(Dispatchers.Default) {
                    LanguageRegistry.get(lang).runCode(code, stdin)
                }
            } catch (t: Throwable) {
                DebugLog.e("Run", "运行时异常 lesson=${lesson.id}", t)
                RunResult(
                    ok = false, stdout = "", stderr = "",
                    errorType = "RuntimeCrash",
                    errorMessage = "运行时异常：${t.javaClass.simpleName}",
                    traceback = null, durationMs = 0L,
                    variables = emptyList(), passed = null
                )
            }
            runResults[index] = r
            runningKey = null
            if (!r.ok) {
                DebugLog.w(
                    "Run",
                    "代码块执行失败 lesson=${lesson.id} lang=$lang type=${r.errorType} msg=${r.errorMessage?.take(120)}"
                )
            }
            recordSideEffects(context, r)
        }
    }

    // 进度按当前课程包（chapterName）算，不按语言。DSA 的 lang 也是 python，
    // 按语言会把 Python 主线算乱。
    val allLessons = remember(lessonId, lesson.chapterName) {
        val chapter = lesson.chapterName
        if (chapter.isBlank()) {
            LessonRepository.mainline(context)
        } else {
            val sameChapter = LessonRepository.lessons(context)
                .filter { it.chapterName == chapter }
            // 章节数据缺失就退回主线，免得进度条变成 0/0。
            sameChapter.ifEmpty { LessonRepository.mainline(context) }
        }
    }
    val lessonIdx = allLessons.indexOfFirst { it.id == lessonId }
    // 完成度按已通关讲数算，和「我的」页保持一致。
    val doneCount = allLessons.count { it.id in progress.completedLessons }
    val tutorSources = remember(lessonId) { TutorKnowledge.fromLessons(LessonRepository.lessons(context)) }

    // 第一次通关时生成复习卡。
    androidx.compose.runtime.LaunchedEffect(alreadySolved) {
        if (alreadySolved) {
            reviewVm.generateCardsForLesson(lesson)
        }
    }

    // 打开课程就记一笔学习历史。
    androidx.compose.runtime.LaunchedEffect(lesson.id) {
        com.pyneon.academy.data.ProgressStore.markLessonOpened(context, lesson.id)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 40.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonButton(label = "返回", accent = TextMid, onClick = onBack)
            Spacer(Modifier.size(10.dp))
            GlitchText(
                "第%02d讲  %s".format(lesson.order, lesson.title),
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
        }

        if (lessonIdx >= 0 && allLessons.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "课程进度 $doneCount / ${allLessons.size} 讲",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(doneCount * 100) / allLessons.size}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { doneCount.toFloat() / allLessons.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    color = NeonCyan,
                    trackColor = NeonCyan.copy(alpha = 0.12f)
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeonButton(
                label = "AI 助教：先讲思路，不代写",
                accent = NeonMagenta,
                leadingIcon = Icons.Outlined.AutoAwesome,
                onClick = { tutorOpen = true },
                modifier = Modifier.fillMaxWidth()
            )
            lesson.blocks.forEachIndexed { index, block ->
                when (block) {
                    is Block.Heading -> SectionHeader(block.text)
                    is Block.Paragraph -> Text(
                        block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextHi
                    )
                    is Block.Tip -> TipBox(block.text, NeonCyan, "TIP")
                    is Block.Warn -> TipBox(block.text, NeonMagenta, "WARN")
                    is Block.Output -> OutputPreview(block.text)
                    is Block.Table -> NeonTable(block.headers, block.rows)
                    is Block.Diagram -> DiagramBox(block.text)
                    is Block.Task -> TipBox(block.text, NeonYellow, "TASK 跟着做")
                    is Block.Steps -> StepsCard(block.items)
                    is Block.Practice -> PracticeCard(
                        practice = block,
                        lang = lesson.lang,
                        result = runResults[index],
                        running = runningKey == index,
                        // TypeScript 的示例逐条验证过才开放运行，别的语言先只读。
                        onRun = if (lesson.lang == "typescript") {
                            { runBlock(index, block.code, lesson.lang, emptyList()) }
                        } else null
                    )
                    is Block.Fill -> FillCard(block, lang = lesson.lang)
                    is Block.Order -> OrderPuzzleCard(block)
                    is Block.Quiz -> QuizCard(block)
                    is Block.Compare -> CompareCard(
                        block = block,
                        results = runResults,
                        runningKey = runningKey,
                        onRun = { key, col ->
                            scope.launch {
                                runningKey = key
                                val r = withContext(Dispatchers.Default) {
                                    LanguageRegistry.get(col.lang).runCode(col.code, emptyList())
                                }
                                runResults[key] = r
                                runningKey = null
                                recordSideEffects(context, r)
                            }
                        }
                    )
                    is Block.Visualize -> VisualizeCard(block)
                    is Block.CodeBlock -> CodeExampleCard(
                        code = block.code,
                        runnable = block.runnable,
                        lang = block.lang.ifBlank { lesson.lang },
                        result = runResults[index],
                        running = runningKey == index,
                        onRun = {
                            // 代码块可自带 lang 覆盖（对比教学）；否则沿用本讲语言
                            runBlock(index, block.code, block.lang.ifBlank { lesson.lang }, block.stdin)
                        }
                    )
                }
            }

            if (exercise != null) {
                SectionHeader("实战演练：${exercise.title}", accent = NeonMagenta)
                NeonCard(accent = if (alreadySolved) NeonGreen else NeonMagenta, filled = false) {
                    Text(exercise.brief, style = MaterialTheme.typography.bodyLarge, color = TextMid)

                    Spacer(Modifier.height(14.dp))
                    Text("CODE EDITOR", style = MaterialTheme.typography.labelSmall, color = NeonMagenta.copy(alpha = 0.7f))
                    Spacer(Modifier.height(4.dp))
                    CodeEditor(
                        value = editorValue,
                        onValueChange = { editorValue = it },
                        hint = exercise.brief,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        showLineNumbers = true,
                        language = lesson.lang
                    )
                    SyntaxHintBar(code = editorValue, language = lesson.lang)

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton(
                            label = "运行判题",
                            accent = if (alreadySolved) NeonGreen else NeonYellow,
                            enabled = !checking,
                            onClick = {
                                scope.launch {
                                    checking = true
                                    checkResult = null
                                    val r = try {
                                        withContext(Dispatchers.Default) {
                                            LanguageRegistry.get(lesson.lang)
                                                .checkExercise(editorValue, exercise.tests, exercise.stdin)
                                        }
                                    } catch (t: Throwable) {
                                        DebugLog.e("Judge", "判题异常 lesson=${lesson.id}", t)
                                        RunResult(
                                            ok = false, stdout = "", stderr = "",
                                            errorType = "RuntimeCrash",
                                            errorMessage = "判题异常：${t.javaClass.simpleName}",
                                            traceback = null, durationMs = 0L,
                                            variables = emptyList(), passed = false
                                        )
                                    }
                                    checkResult = r
                                    checking = false
                                    if (r.passed != true) {
                                        DebugLog.i(
                                            "Judge",
                                            "判题未通过 lesson=${lesson.id} type=${r.errorType}"
                                        )
                                    }
                                    recordSideEffects(context, r)
                                    if (r.passed == true) {
                                        if (solvedKey !in progress.solvedKeys) {
                                            ProgressStore.markExerciseSolved(context, solvedKey, exercise.xp, Clock.todayEpochDay())
                                            ProgressStore.markLessonDone(context, lesson.id, lesson.xp)
                                            rewardXp = exercise.xp + lesson.xp
                                            // 第一次通关才生成复习卡。
                                            reviewVm.generateCardsForLesson(lesson)
                                        } else {
                                            rewardXp = 0
                                        }
                                    } else {
                                        // 没过就进错题本。
                                        val errorMsg = if (r.stdout.isNotBlank()) r.stdout else r.stderr.ifBlank { "未知错误" }
                                        mistakeVm.recordMistake(
                                            lessonId = lesson.id,
                                            blockType = "exercise",
                                            blockIndex = -1,
                                            userCode = editorValue,
                                            expected = exercise.tests.joinToString("\n"),
                                            actual = r.stdout,
                                            error = errorMsg,
                                            conceptTags = extractConceptTags(lesson.id)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            label = "提示",
                            accent = NeonCyan,
                            leadingIcon = Icons.Outlined.Lightbulb,
                            onClick = { hintOpen = !hintOpen }
                        )
                    }

                    if (hintOpen && exercise.hint.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        TipBox(exercise.hint, NeonYellow, "HINT")
                    }
                    if (!checking && checkResult != null) {
                        Spacer(Modifier.height(10.dp))
                        ConsoleResult(result = checkResult, running = false)
                    }
                    val needCoach = !checking && checkResult != null &&
                        (checkResult?.passed == false || checkResult?.errorType != null)
                    if (needCoach) {
                        Spacer(Modifier.height(10.dp))
                        NeonButton(
                            label = "问教练（只提示不代写）",
                            accent = NeonMagenta,
                            leadingIcon = Icons.Outlined.Lightbulb,
                            onClick = {
                                coachTip = CoachEngine.analyze(
                                    checkResult?.errorType,
                                    checkResult?.errorMessage,
                                    checkResult?.traceback,
                                    editorValue
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    coachTip?.let { tip ->
                        AlertDialog(
                            onDismissRequest = { coachTip = null },
                            title = { Text("教练：${tip.title}", style = MaterialTheme.typography.titleMedium) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(tip.summary, style = MaterialTheme.typography.bodyMedium)
                                    tip.lineRef?.let {
                                        Text(
                                            "定位：疑似第 $it 行附近",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextDim
                                        )
                                    }
                                    tip.steps.forEachIndexed { i, s ->
                                        Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    tip.example?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { coachTip = null }) { Text("明白了") }
                            }
                        )
                    }
                    if (alreadySolved && checkResult?.passed != false) {
                        Text(
                            "已通关",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonGreen,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (tutorOpen) {
        TutorDialog(
            lessonId = lesson.id,
            lessonTitle = lesson.title,
            lessonBrief = lesson.exercise?.brief,
            starterCode = lesson.exercise?.starterCode,
            tests = lesson.exercise?.tests.orEmpty(),
            hint = lesson.exercise?.hint,
            currentCode = editorValue,
            result = checkResult,
            nextLessonTitle = allLessons.getOrNull(lessonIdx + 1)?.title,
            sources = tutorSources,
            onDismiss = { tutorOpen = false }
        )
    }

    rewardXp?.let { gained ->
        // 学完弹「下一课」，别把人卡在结果页。
        val nextLesson = allLessons.getOrNull(lessonIdx + 1)
        AlertDialog(
            onDismissRequest = { rewardXp = null },
            title = { GlitchText(if (gained > 0) "任务完成" else "已通关", style = MaterialTheme.typography.headlineSmall, color = NeonGreen) },
            text = {
                Column {
                    if (gained > 0) {
                        Text("+$gained XP 已记录", style = MaterialTheme.typography.bodyLarge, color = NeonYellow)
                        Text("课程进度已同步，下一课解锁。", style = MaterialTheme.typography.bodyMedium, color = TextMid)
                    } else {
                        Text("本练习此前已完成，继续保持连击吧。", style = MaterialTheme.typography.bodyMedium, color = TextMid)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { rewardXp = null }) { Text("留在本课", color = TextMid) }
                    if (gained > 0 && nextLesson != null && onNextLesson != null) {
                        TextButton(onClick = { rewardXp = null; onNextLesson(nextLesson.id) }) {
                            Text("下一课：${nextLesson.title}", color = NeonGreen)
                        }
                    }
                }
            },
            containerColor = SurfaceDark
        )
    }
}

private suspend fun recordSideEffects(context: android.content.Context, result: RunResult?) {
    if (result == null || !result.ok) return
    if (!ProgressStore.snapshot(context).firstRunDone) {
        ProgressStore.markFirstRun(context)
    }
    ProgressStore.markNightRun(context, Clock.currentHour())
}

private fun extractConceptTags(lessonId: String): List<String> {
    return when {
        lessonId.startsWith("l01") -> listOf("basics", "print")
        lessonId.startsWith("l02") -> listOf("types", "variables")
        lessonId.startsWith("l03") -> listOf("strings", "text")
        lessonId.startsWith("l04") -> listOf("operators", "math")
        lessonId.startsWith("l05") -> listOf("input", "io")
        lessonId.startsWith("l06") -> listOf("conditionals", "logic")
        lessonId.startsWith("l07") -> listOf("loops", "iteration")
        lessonId.startsWith("l08") -> listOf("lists", "collections")
        lessonId.startsWith("l09") -> listOf("dicts", "mappings")
        lessonId.startsWith("l10") -> listOf("project", "review")
        lessonId.startsWith("l11") -> listOf("strings", "text")
        lessonId.startsWith("l12") -> listOf("tuples", "sets")
        lessonId.startsWith("l13") -> listOf("functions", "def")
        lessonId.startsWith("l14") -> listOf("comprehensions", "lists")
        lessonId.startsWith("l15") -> listOf("exceptions", "errors")
        lessonId.startsWith("l16") -> listOf("persistence", "files")
        lessonId.startsWith("l17") -> listOf("modules", "import")
        lessonId.startsWith("l18") -> listOf("oop", "classes")
        lessonId.startsWith("l19") -> listOf("inheritance", "magic-methods")
        lessonId.startsWith("l20") -> listOf("project", "bank")
        lessonId.startsWith("l21") -> listOf("generators", "iterators")
        lessonId.startsWith("l22") -> listOf("decorators", "wrappers")
        lessonId.startsWith("l23") -> listOf("lambdas", "functional")
        lessonId.startsWith("l24") -> listOf("stdlib", "modules")
        lessonId.startsWith("l25") -> listOf("datetime", "random")
        lessonId.startsWith("l26") -> listOf("project", "logging")
        lessonId.startsWith("l27") -> listOf("context-managers", "with")
        lessonId.startsWith("l28") -> listOf("file-io", "persistence")
        lessonId.startsWith("l29") -> listOf("exceptions", "custom")
        lessonId.startsWith("l30") -> listOf("modules", "packages")
        else -> listOf("general")
    }
}

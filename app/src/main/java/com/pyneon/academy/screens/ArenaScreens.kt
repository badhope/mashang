package com.pyneon.academy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.DarkTokens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pyneon.academy.ai.TutorKnowledge
import com.pyneon.academy.data.Clock
import com.pyneon.academy.data.DraftStore
import com.pyneon.academy.data.LessonRepository
import com.pyneon.academy.data.ProgressStore
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.ui.components.ConsoleResult
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.components.PythonCodeField
import com.pyneon.academy.ui.components.SyntaxHintBar
import com.pyneon.academy.ui.effects.GlitchText
import com.pyneon.academy.ui.effects.NeonCard
import com.pyneon.academy.ui.effects.cyberGrid
import com.pyneon.academy.ui.effects.scanlines
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonGreen
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArenaScreen(openChallenge: (String) -> Unit) {
    val context = LocalContext.current
    val challenges = remember { LessonRepository.challenges(context) }
    val difficultyColor: (String) -> Color = {
        when (it) {
            "低" -> DarkTokens.success
            "高" -> DarkTokens.secondary
            else -> DarkTokens.gold
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .cyberGrid(NeonYellow.copy(alpha = 0.04f), 48.dp)
            .scanlines()
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GlitchText("挑战", style = MaterialTheme.typography.headlineSmall, color = NeonYellow)
            Text("编程挑战，用断言判题，通过得 XP", style = MaterialTheme.typography.bodySmall, color = TextDim, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            challenges.forEach { c ->
                val accent = difficultyColor(c.difficulty)
                NeonCard(accent = accent, onClick = { openChallenge(c.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium, color = TextHi)
                            Text(
                                "难度 ${c.difficulty}，${c.xp} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text("进入", style = MaterialTheme.typography.labelLarge, color = accent)
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun ChallengeDetailScreen(challengeId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val challenge = remember(challengeId) { LessonRepository.challenge(context, challengeId) }
    val progress by ProgressStore.flow(context).collectAsState(initial = com.pyneon.academy.data.Progress())

    // 和课程页一样先读落盘草稿，光标放到末尾，避免恢复后从第一行开始敲。
    val starterCode = challenge?.starterCode.orEmpty()
    val savedDraft = remember(challengeId) { DraftStore.load(context, "challenge_$challengeId") }
    var editorValue by rememberSaveable(challengeId) {
        val text = savedDraft ?: starterCode
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    var result by remember(challengeId) { mutableStateOf<RunResult?>(null) }
    var busy by remember(challengeId) { mutableStateOf(false) }
    var hintOpen by remember(challengeId) { mutableStateOf(false) }
    var tutorOpen by rememberSaveable(challengeId) { mutableStateOf(false) }
    // 挑战 XP 也可能带每日首练奖励，别拿题目面板值直接当入账值。
    var gainedXp by remember(challengeId) { mutableStateOf<Int?>(null) }

    if (challenge == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Text("挑战数据缺失", color = NeonMagenta)
        }
        return
    }

    LaunchedEffect(challengeId, editorValue.text) {
        delay(500)
        if (savedDraft != null || editorValue.text != starterCode) {
            DraftStore.save(context, "challenge_$challengeId", editorValue.text)
        } else {
            DraftStore.save(context, "challenge_$challengeId", "")
        }
    }
    val latestEditor by rememberUpdatedState(editorValue.text)
    DisposableEffect(challengeId) {
        onDispose {
            if (savedDraft != null || latestEditor != starterCode) {
                DraftStore.saveNow(context, "challenge_$challengeId", latestEditor)
            } else {
                DraftStore.saveNow(context, "challenge_$challengeId", "")
            }
        }
    }

    val solvedKey = "chal_$challengeId"
    val alreadySolved = solvedKey in progress.solvedKeys

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .cyberGrid(NeonYellow.copy(alpha = 0.03f), 48.dp)
            .scanlines()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 40.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonButton(label = "返回", accent = TextMid, onClick = onBack)
            Spacer(Modifier.size(10.dp))
            GlitchText(challenge.title, style = MaterialTheme.typography.titleMedium, color = NeonYellow)
        }
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeonCard(accent = if (alreadySolved) NeonGreen else NeonYellow) {
                Text(challenge.brief, style = MaterialTheme.typography.bodyLarge, color = TextMid)
                Spacer(Modifier.height(12.dp))
                PythonCodeField(
                    value = editorValue,
                    onValueChange = { editorValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceHigh.copy(alpha = 0.5f))
                        .padding(4.dp),
                    minHeight = 160,
                    language = challenge.lang
                )
                SyntaxHintBar(code = editorValue.text, language = challenge.lang)
                if (savedDraft != null && savedDraft != starterCode && editorValue.text == savedDraft) {
                    Text(
                        "已恢复上次未提交的草稿",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                NeonButton(
                    label = "AI 助教：先讲思路，不代写",
                    accent = NeonMagenta,
                    leadingIcon = Icons.Outlined.AutoAwesome,
                    onClick = { tutorOpen = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        label = "运行判题",
                        accent = NeonGreen,
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                result = null
                                // 运行时异常得接住。Python 没初始化、JNI 崩了、JSON 解析失败
                                // 都会抛，裸调用会闪退，用户刚写的代码全丢。
                                val r = try {
                                    withContext(Dispatchers.Default) {
                                        LanguageRegistry.get(challenge.lang)
                                            .checkExercise(editorValue.text, challenge.tests, challenge.stdin)
                                    }
                                } catch (t: Throwable) {
                                    com.pyneon.academy.lang.RunResult(
                                        ok = false,
                                        stdout = "",
                                        stderr = "",
                                        errorType = "RuntimeCrash",
                                        errorMessage = "运行时执行失败：${t.javaClass.simpleName} ${t.message?.take(120) ?: ""}",
                                        traceback = null,
                                        durationMs = 0L,
                                        variables = emptyList(),
                                        passed = false
                                    )
                                }
                                result = r
                                busy = false
                                if (r.ok && r.passed == true && solvedKey !in progress.solvedKeys) {
                                    val beforeXp = ProgressStore.snapshot(context).xpTotal
                                    ProgressStore.markExerciseSolved(context, solvedKey, challenge.xp, Clock.todayEpochDay())
                                    val afterXp = ProgressStore.snapshot(context).xpTotal
                                    gainedXp = (afterXp - beforeXp).coerceAtLeast(0)
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
                if (hintOpen && challenge.hint.isNotEmpty()) {
                    Text(
                        "提示",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(challenge.hint, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                }
                if (!busy && result != null) {
                    ConsoleResult(result = result, running = false)
                }
                if (alreadySolved) {
                    val gained = gainedXp
                    Text(
                        if (gained != null && gained > 0) "已通过，+$gained XP 已入账" else "已通过",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonGreen,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }

    if (tutorOpen) {
        val tutorSources = remember(challengeId, tutorOpen) {
            TutorKnowledge.fromLessons(
                LessonRepository.lessons(context).filter { it.lang == challenge.lang }
            )
        }
        TutorDialog(
            lessonId = challenge.id,
            lessonTitle = challenge.title,
            lessonBrief = challenge.brief,
            starterCode = challenge.starterCode,
            tests = challenge.tests,
            hint = challenge.hint,
            currentCode = editorValue.text,
            result = result,
            nextLessonTitle = null,
            sources = tutorSources,
            isChallenge = true,
            onDismiss = { tutorOpen = false }
        )
    }
}

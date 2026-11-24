package com.pyneon.academy.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyneon.academy.ai.TutorEngine
import com.pyneon.academy.ai.TutorIntent
import com.pyneon.academy.ai.TutorRequest
import com.pyneon.academy.ai.TutorReply
import com.pyneon.academy.ai.TutorSource
import com.pyneon.academy.lang.RunResult
import com.pyneon.academy.ui.components.NeonButton
import com.pyneon.academy.ui.theme.Bg1
import com.pyneon.academy.ui.theme.Bg0
import com.pyneon.academy.ui.theme.NeonCyan
import com.pyneon.academy.ui.theme.NeonMagenta
import com.pyneon.academy.ui.theme.NeonYellow
import com.pyneon.academy.ui.theme.SurfaceDark
import com.pyneon.academy.ui.theme.SurfaceHigh
import com.pyneon.academy.ui.theme.TextDim
import com.pyneon.academy.ui.theme.TextHi
import com.pyneon.academy.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TutorDialog(
    lessonId: String,
    lessonTitle: String,
    lessonBrief: String?,
    starterCode: String?,
    tests: List<String>,
    hint: String?,
    currentCode: String,
    result: RunResult?,
    nextLessonTitle: String?,
    sources: List<TutorSource>,
    isChallenge: Boolean = false,
    onDismiss: () -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }
    var reply by remember { mutableStateOf<TutorReply?>(null) }
    var loading by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf(0) }
    var initialAsked by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val answerScroll = rememberScrollState()

    fun ask(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || loading) return
        question = text
        // “帮我读报错”在还没跑、或这次已经跑通时不该给一套泛泛的排错话。
        if (clean == "帮我读报错" &&
            (result == null || (result.errorType == null && result.passed != false))
        ) {
            loading = false
            reply = TutorReply(
                intent = TutorIntent.ERROR,
                title = if (result == null) "先跑一次，才有报错可读" else "这次没有报错",
                lead = if (result == null) {
                    "现在还没有运行结果。点一下“运行判题”，把输出和报错原样带回来。"
                } else {
                    if (result.passed == true) {
                        "判题已经通过，当前没有运行时异常。想继续加固，可以补边界输入再跑。"
                    } else {
                        "这次运行没有报错，但也没看到判题通过。把 stdout/stderr 和预期结果贴出来。"
                    }
                },
                steps = if (result == null) {
                    listOf("点“运行判题”，不要只看代码编辑器。", "如果运行按钮没反应，先看是否有语法提示。", "把完整输出贴给我，不要只贴最后一行。")
                } else {
                    if (result.passed == true) {
                        listOf("换一组空输入、重复值或最大规模再跑。", "如果结果仍然对，去错题本看看这题以前错在哪。")
                    } else {
                        listOf("把 stdout、stderr 和题目预期各贴一行。", "先确认是不是只运行了代码块，没有点判题。")
                    }
                },
                checkpoint = if (result == null) "先拿到一次真实运行结果。" else "能说出这题至少一个边界条件。"
            )
            return
        }
        val id = ++requestId
        loading = true
        reply = null
        keyboard?.hide()
        // 245 讲压成文本再检索不是零成本，放到 Default 线程，别卡住输入框。
        scope.launch {
            val next = withContext(Dispatchers.Default) {
                runCatching {
                    TutorEngine.reply(
                        TutorRequest(
                            question = clean,
                            lessonId = lessonId,
                            lessonTitle = lessonTitle,
                            lessonBrief = lessonBrief,
                            starterCode = starterCode,
                            currentCode = currentCode,
                            tests = tests,
                            hint = hint,
                            errorType = result?.errorType,
                            errorMessage = result?.errorMessage,
                            traceback = result?.traceback,
                            passed = result?.passed,
                            nextLessonTitle = nextLessonTitle,
                            isChallenge = isChallenge,
                            sources = sources
                        )
                    )
                }.getOrElse { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    TutorReply(
                        intent = TutorIntent.GENERAL,
                        title = "这次没算出来",
                        lead = "检索过程出了点问题，不影响你的代码和进度。",
                        steps = listOf("先把当前代码原样跑一次，确认报错还在不在。", "把报错的第一行和你最后改的地方贴出来。"),
                        checkpoint = "能复现一次，再把问题缩小到最小代码。"
                    )
                }
            }
            // 用户可能连点两次，旧请求回来时别覆盖新回答。
            if (id == requestId) {
                reply = next
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // 旋转后 question 会被 rememberSaveable 带回来，别再用默认问题把它覆盖掉。
        if (!initialAsked) {
            initialAsked = true
            ask(if (tests.isNotEmpty()) "这题怎么开始" else "这一讲的重点是什么")
        }
    }
    LaunchedEffect(reply) {
        if (reply != null) answerScroll.scrollTo(0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .border(1.dp, NeonMagenta.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
                    .imePadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = NeonMagenta,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI 助教", style = MaterialTheme.typography.titleMedium, color = NeonMagenta)
                        Text(
                            "本地运行 · 不联网 · 只给思路",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = TextMid)
                    }
                }

                Text(
                    "我不会替你写完整答案。先告诉我卡在哪，或者直接选一个方向。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("这题怎么开始", "帮我读报错", "解释这个知识点", "下一步学什么").forEach { item ->
                        NeonButton(
                            label = item,
                            onClick = { ask(item) },
                            accent = NeonCyan,
                            enabled = !loading
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = {
                        Text("例如：为什么我的循环没有退出？", style = MaterialTheme.typography.bodySmall)
                    },
                    minLines = 1,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { ask(question) })
                )
                Spacer(Modifier.height(6.dp))
                NeonButton(
                    label = if (loading) "正在翻课程…" else "问助教",
                    onClick = { ask(question) },
                    accent = NeonMagenta,
                    enabled = question.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(answerScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val current = reply
                    if (loading) {
                        Text(
                            "正在查课程原文和运行结果…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMid
                        )
                    } else if (current == null) {
                        Text(
                            "先选一个方向，或者把具体问题写下来。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMid
                        )
                    } else {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonYellow
                        )
                        Text(
                            current.lead,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHi
                        )
                        current.steps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMid
                                )
                            }
                        }
                        Surface(
                            color = Bg1,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "检查点：${current.checkpoint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonYellow,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        current.sourceText?.let { text ->
                            Surface(
                                color = SurfaceHigh,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        current.sourceTitle ?: "课程原文",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMid
                                    )
                                }
                            }
                        }
                        current.example?.let { code ->
                            Surface(
                                color = Bg0,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

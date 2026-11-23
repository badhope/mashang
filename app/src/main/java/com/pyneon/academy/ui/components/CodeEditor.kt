package com.pyneon.academy.ui.components

import android.graphics.Typeface
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.widget.CodeEditor as SoraCodeEditor
import com.pyneon.academy.ui.theme.LocalNeonTokens
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.awaitCancellation
import org.eclipse.tm4e.core.registry.IGrammarSource

/**
 * 练习编辑器用 sora-editor 做语法高亮、撤销、括号匹配和行号。
 * 之前用 BasicTextField 手搓，Tab、缩进、滚动同步都没有，实在没必要。
 */

/** 自建轻量 PyNeon 主题：把 TextMate scope 映射到品牌色板。 */
private val PYNEON_THEME_JSON: String =
    """
{
  "name": "PyNeon Dark",
  "type": "dark",
  "colors": {
    "editor.background": "#10141C",
    "editor.foreground": "#D8E2E8",
    "editor.lineHighlightBackground": "#1A2230",
    "editorLineNumber.foreground": "#3E4C5C",
    "editorLineNumber.activeForeground": "#00E5FF",
    "editorCursor.foreground": "#00E5FF",
    "editor.selectionBackground": "#1E4C5A",
    "editorIndentGuide.background": "#1A2230",
    "editorIndentGuide.activeBackground": "#00E5FF",
    "editorWhitespace.foreground": "#263244",
    "editor.findMatchBackground": "#4A3A00",
    "editorSuggestWidget.background": "#10141C",
    "editorSuggestWidget.foreground": "#D8E2E8",
    "editorSuggestWidget.selectedBackground": "#1A2230",
    "editorSuggestWidget.highlightForeground": "#F7FF00"
  },
  "tokenColors": [
    { "scope": "comment", "settings": { "foreground": "#5A6B78", "fontStyle": "italic" } },
    { "scope": "keyword", "settings": { "foreground": "#00E5FF", "fontStyle": "bold" } },
    { "scope": "keyword.operator", "settings": { "foreground": "#00E5FF" } },
    { "scope": "storage.type", "settings": { "foreground": "#00E5FF", "fontStyle": "bold" } },
    { "scope": "string", "settings": { "foreground": "#00FF9C" } },
    { "scope": "constant.numeric", "settings": { "foreground": "#F7FF00" } },
    { "scope": "constant.language", "settings": { "foreground": "#B45CFF", "fontStyle": "bold" } },
    { "scope": "entity.name.function", "settings": { "foreground": "#FF2D78" } },
    { "scope": "support.function.builtin", "settings": { "foreground": "#B45CFF" } },
    { "scope": "support.type.python", "settings": { "foreground": "#B45CFF" } },
    { "scope": "variable.language", "settings": { "foreground": "#B45CFF" } }
  ]
}
""".trimIndent()


// 白纸模式下的代码配色，scope 和暗色一致，只换色值。

private val PYNEON_THEME_LIGHT_JSON: String =
    """
{
  "name": "PyNeon Light",
  "type": "light",
  "colors": {
    "editor.background": "#FFFFFF",
    "editor.foreground": "#1A2332",
    "editor.lineHighlightBackground": "#F1F5FA",
    "editorLineNumber.foreground": "#9AA9BA",
    "editorLineNumber.activeForeground": "#007E96",
    "editorCursor.foreground": "#007E96",
    "editor.selectionBackground": "#CDE9EF",
    "editorIndentGuide.background": "#E4EAF2",
    "editorIndentGuide.activeBackground": "#007E96",
    "editorWhitespace.foreground": "#D6DEE8",
    "editor.findMatchBackground": "#FFE9A8",
    "editorSuggestWidget.background": "#FFFFFF",
    "editorSuggestWidget.foreground": "#1A2332",
    "editorSuggestWidget.selectedBackground": "#F1F5FA",
    "editorSuggestWidget.highlightForeground": "#B08900"
  },
  "tokenColors": [
    { "scope": "comment", "settings": { "foreground": "#6B7C8F", "fontStyle": "italic" } },
    { "scope": "keyword", "settings": { "foreground": "#007E96", "fontStyle": "bold" } },
    { "scope": "keyword.operator", "settings": { "foreground": "#007E96" } },
    { "scope": "storage.type", "settings": { "foreground": "#007E96", "fontStyle": "bold" } },
    { "scope": "string", "settings": { "foreground": "#1F7A3D" } },
    { "scope": "constant.numeric", "settings": { "foreground": "#8A6D00" } },
    { "scope": "constant.language", "settings": { "foreground": "#7B2FBE", "fontStyle": "bold" } },
    { "scope": "entity.name.function", "settings": { "foreground": "#C2185B" } },
    { "scope": "support.function.builtin", "settings": { "foreground": "#7B2FBE" } },
    { "scope": "support.type.python", "settings": { "foreground": "#7B2FBE" } },
    { "scope": "variable.language", "settings": { "foreground": "#7B2FBE" } }
  ]
}
""".trimIndent()

private const val TAG = "PyNeonCodeEditor"

// 建编辑器。语法按 language 从 assets 里读，主题按明暗选。

private fun createEditor(
    context: android.content.Context,
    language: String,
    isLight: Boolean
): SoraCodeEditor {
    val editor = SoraCodeEditor(context)
    try {
        val grammarText = context.assets
            .open("syntaxes/$language.tmLanguage.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        // plist 是 XML，ContentType 要写对。scopeName 也得一起传，
        // 不然 TextMateLanguage.create 会抛异常，最后掉回白底编辑器。
        val scopeName = when (language) {
            "python" -> "source.python"
            "javascript" -> "source.js"
            "typescript" -> "source.ts"
            "c" -> "source.c"
            "lua" -> "source.lua"
            "sql" -> "source.sql"
            else -> "source.$language"
        }
        val grammarSource = IGrammarSource.fromString(IGrammarSource.ContentType.XML, grammarText)
        val grammar = DefaultGrammarDefinition.withGrammarSource(
            grammarSource, language, scopeName
        )
        val textMateLanguage = TextMateLanguage.create(grammar, true)
        editor.setEditorLanguage(textMateLanguage)

        val themeSource = org.eclipse.tm4e.core.registry.IThemeSource.fromString(
            org.eclipse.tm4e.core.registry.IThemeSource.ContentType.JSON,
            if (isLight) PYNEON_THEME_LIGHT_JSON else PYNEON_THEME_JSON
        )
        val scheme = TextMateColorScheme.create(themeSource)
        editor.setColorScheme(scheme)
    } catch (t: Throwable) {
        // 加载失败就退回纯文本，别让编辑器打不开。
        Log.w(TAG, "TextMate 语法加载失败，回退默认高亮: ${t.message}")
        editor.setColorScheme(EditorColorScheme())
    }

    editor.apply {
        setUndoEnabled(true)
        setWordwrap(false)
        setTabWidth(4)
        // 字号跟系统缩放走。
        setTextSizePx(14f * context.resources.displayMetrics.scaledDensity)
        setTypefaceText(Typeface.MONOSPACE)
        setTypefaceLineNumber(Typeface.MONOSPACE)
        setEditable(true)
        setPinLineNumber(true)
    }
    return editor
}

/**
 * 练习编辑器。hint 和 onEnter 是给旧调用方留的：
 * sora 没有 placeholder，也没有回车钩子。
 */
@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onEnter: (() -> Unit)? = null,
    hint: String = "",
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    language: String = "python"
) {
    val context = LocalContext.current
    // 切明暗主题要重建编辑器，不然代码区还是旧配色。
    val isLight = LocalNeonTokens.current.isLight
    val editor = remember(context, language, isLight) { createEditor(context, language, isLight) }
    var listener by remember { mutableStateOf<((String) -> Unit)?>(null) }

    LaunchedEffect(editor) {
        val receipt = editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
            if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                listener?.invoke(editor.text.toString())
            }
        }
        // 页面销毁时退订，别漏。
        try {
            awaitCancellation()
        } finally {
            receipt.unsubscribe()
        }
    }

    AndroidView(
        factory = { editor },
        modifier = modifier,
        update = { view ->
            // 外部值变了就同步；内容一样就不动，免得打断光标和撤销。
            val current = view.text.toString()
            if (current != value) {
                view.setText(value)
            }
            view.setEditable(!readOnly)
            listener = onValueChange
        }
    )
}

// 只读示例块还用这套正则高亮，先留着。

// 只读示例用的简易高亮，支持 python、javascript、lua、sql。
// 三引号字符串、f-string 这些它认不全，只读展示够用。

fun buildAnnotatedString(
    code: String,
    language: String,
    tokens: com.pyneon.academy.ui.theme.NeonTokens = com.pyneon.academy.ui.theme.DarkTokens
): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    if (language != "python" && language != "javascript" && language != "lua" && language != "sql") {
        return builder.toAnnotatedString()
    }

    val keywordStyle = SpanStyle(color = tokens.primary, fontWeight = FontWeight.Bold)
    val stringStyle = SpanStyle(color = tokens.success)
    val commentStyle = SpanStyle(color = tokens.textDim, fontStyle = FontStyle.Italic)
    val numberStyle = SpanStyle(color = tokens.secondary)
    val functionStyle = SpanStyle(color = tokens.primary, fontWeight = FontWeight.Normal)
    val builtinStyle = SpanStyle(color = tokens.purple)

    val (keywords, builtins, commentPrefix) = when (language) {
        "javascript" -> jsHighlightSets()
        "lua" -> luaHighlightSets()
        "sql" -> sqlHighlightSets()
        else -> pythonHighlightSets()
    }

    val keywordPattern = "\\b(${keywords.joinToString("|")})\\b".toRegex()
    val stringPattern = """(["'])(?:(?=(\\?))\2.)*?\1""".toRegex()
    val commentPattern = "$commentPrefix.*".toRegex()
    val numberPattern = "\\b\\d+(\\.\\d+)?\\b".toRegex()
    val functionPattern = "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()".toRegex()
    val builtinPattern = "\\b(${builtins.joinToString("|")})\\b".toRegex()

    // 先注释，再字符串，最后其他。顺序反了会互相覆盖。
    commentPattern.findAll(code).forEach { match ->
        builder.addStyle(commentStyle, match.range.first, match.range.last + 1)
    }

    stringPattern.findAll(code).forEach { match ->
        builder.addStyle(stringStyle, match.range.first, match.range.last + 1)
    }

    keywordPattern.findAll(code).forEach { match ->
        builder.addStyle(keywordStyle, match.range.first, match.range.last + 1)
    }

    numberPattern.findAll(code).forEach { match ->
        builder.addStyle(numberStyle, match.range.first, match.range.last + 1)
    }

    builtinPattern.findAll(code).forEach { match ->
        builder.addStyle(builtinStyle, match.range.first, match.range.last + 1)
    }

    functionPattern.findAll(code).forEach { match ->
        val name = match.groupValues[1]
        if (name !in keywords && name !in builtins) {
            builder.addStyle(functionStyle, match.range.first, match.range.last + 1)
        }
    }

    return builder.toAnnotatedString()
}

private fun pythonHighlightSets(): Triple<Set<String>, Set<String>, String> = Triple(
    setOf(
        "def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally",
        "with", "as", "import", "from", "return", "yield", "lambda", "pass", "break",
        "continue", "raise", "assert", "del", "global", "nonlocal", "and", "or", "not",
        "in", "is", "None", "True", "False", "async", "await", "match", "case"
    ),
    setOf(
        "print", "len", "range", "int", "str", "float", "bool", "list", "dict", "set",
        "tuple", "open", "input", "round", "abs", "max", "min", "sum", "sorted", "reversed",
        "enumerate", "zip", "map", "filter", "any", "all", "isinstance", "issubclass",
        "hasattr", "getattr", "setattr", "type", "super", "object", "Exception", "ValueError",
        "TypeError", "KeyError", "IndexError", "FileNotFoundError", "ImportError", "AttributeError"
    ),
    "#"
)

private fun luaHighlightSets(): Triple<Set<String>, Set<String>, String> = Triple(
    setOf(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto",
        "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"
    ),
    setOf(
        "print", "type", "tostring", "tonumber", "pairs", "ipairs", "next", "select",
        "assert", "error", "pcall", "xpcall", "setmetatable", "getmetatable", "rawget",
        "rawset", "rawequal", "rawlen", "string", "table", "math", "coroutine"
    ),
    "--"
)

private fun sqlHighlightSets(): Triple<Set<String>, Set<String>, String> = Triple(
    setOf(
        "select", "from", "where", "insert", "into", "values", "update", "set", "delete",
        "create", "table", "drop", "alter", "join", "inner", "left", "right", "outer", "on",
        "group", "by", "order", "having", "limit", "offset", "distinct", "as", "and", "or",
        "not", "null", "is", "in", "between", "like", "exists", "union", "all", "case",
        "when", "then", "else", "end", "primary", "key", "foreign", "references", "default",
        "unique", "check", "index", "view", "with", "asc", "desc", "count", "sum", "avg",
        "min", "max", "integer", "text", "real", "blob", "numeric", "begin", "commit", "rollback"
    ),
    setOf("sqlite_master", "sqlite_sequence", "rowid", "typeof", "coalesce", "ifnull", "nullif", "substr", "length", "round", "random"),
    "--"
)

private fun jsHighlightSets(): Triple<Set<String>, Set<String>, String> = Triple(
    setOf(
        "var", "let", "const", "function", "class", "return", "if", "else", "for",
        "while", "do", "switch", "case", "default", "break", "continue", "new",
        "typeof", "instanceof", "delete", "in", "of", "try", "catch", "finally",
        "throw", "yield", "async", "await", "this", "super", "extends", "static",
        "null", "undefined", "true", "false", "NaN", "Infinity"
    ),
    setOf(
        "console", "Math", "JSON", "Object", "Array", "String", "Number", "Boolean",
        "parseInt", "parseFloat", "isNaN", "isFinite", "Promise", "Symbol", "Date",
        "toFixed", "slice", "split", "join", "toUpperCase", "toLowerCase", "push", "pop"
    ),
    "//"
)

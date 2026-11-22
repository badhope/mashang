package com.pyneon.academy.data

import android.content.Context
import com.pyneon.academy.lang.LanguageIds
import org.json.JSONArray
import org.json.JSONObject

sealed class Block {
    data class Heading(val text: String) : Block()
    data class Paragraph(val text: String) : Block()
    data class Tip(val text: String) : Block()
    data class Warn(val text: String) : Block()
    /**
     * 代码块。
     * @param lang 代码语言；为空表示继承所属课程的 lang（绝大多数情况）。
     *             显式指定可用于"同一讲里对比两门语言"的教学片段。
     */
    data class CodeBlock(
        val code: String,
        val runnable: Boolean,
        val stdin: List<String> = emptyList(),
        val lang: String = ""
    ) : Block()
    data class Output(val text: String) : Block()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : Block()
    data class Diagram(val text: String) : Block()
    data class Task(val text: String) : Block()
    data class Steps(val items: List<String>) : Block()
    data class Practice(val title: String, val code: String, val output: String, val hint: String) : Block()
    data class Fill(val goal: String, val code: String, val answer: String, val explain: String) : Block()
    data class Order(val title: String, val lines: List<String>) : Block()
    data class Quiz(
        val question: String,
        val options: List<String>,
        val answerIndex: Int,
        val explain: String
    ) : Block()

    /**
     * 多语言对照块：同一件事在 2–3 门语言里怎么写。
     *
     * 教学价值：学习者最容易建立「概念是共通的、语法才是差异」的认知。
     * 每栏可独立运行（runnable），由各自语言的运行时执行。
     */
    data class Compare(
        val title: String,
        val columns: List<CompareColumn>
    ) : Block()

    data class CompareColumn(
        val lang: String,
        val code: String,
        val note: String = "",
        val runnable: Boolean = true
    )

    /**
     * 可视化块：把抽象过程画成可逐步播放的动画。
     *
     * 用途：变量变化、循环迭代、数组移动、递归调用栈——这些用文字描述
     * 很吃力，但一帧一帧看就明白了。每帧是一组「标签 → 值」快照，
     * 播放器按顺序高亮变化项。
     */
    data class Visualize(
        val title: String,
        val caption: String = "",
        /** 每帧：一行说明 + 若干「名字 → 值」项 */
        val frames: List<VisualFrame>
    ) : Block()

    data class VisualFrame(
        val label: String,
        val items: List<VisualItem> = emptyList(),
        /** 高亮哪些项发生了变化（按 name 匹配） */
        val highlight: List<String> = emptyList()
    )

    data class VisualItem(val name: String, val value: String)
}

data class Exercise(
    val title: String,
    val brief: String,
    val starterCode: String,
    val hint: String,
    val tests: List<String>,
    val stdin: List<String>,
    val xp: Int
)

data class Lesson(
    val id: String,
    val order: Int,
    val chapter: Int,
    val chapterName: String = "",
    val title: String,
    val subtitle: String,
    val xp: Int,
    val blocks: List<Block>,
    val exercise: Exercise?,
    /** 课程语言，见 LanguageIds；缺省 python 以保持旧课程 JSON 完全兼容 */
    val lang: String = LanguageIds.PYTHON
)

data class Challenge(
    val id: String,
    val title: String,
    val difficulty: String,
    val xp: Int,
    val brief: String,
    val starterCode: String,
    val hint: String,
    val tests: List<String>,
    val stdin: List<String>,
    val lang: String = LanguageIds.PYTHON
)

object LessonRepository {

    @Volatile
    private var lessonCache: List<Lesson>? = null

    @Volatile
    private var challengeCache: List<Challenge>? = null

    fun lessons(context: Context): List<Lesson> {
        lessonCache?.let { return it }
        val parsed = listOf("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json").flatMap { fileName ->
            val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(parseLesson(array.getJSONObject(i), defaultRunnable = true))
            }
        }
        val downloaded = ContentCenter().installedFiles(context).flatMap { f ->
            try {
                val array = JSONArray(f.readText())
                buildList {
                    for (i in 0 until array.length()) add(parseLesson(array.getJSONObject(i), defaultRunnable = false))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
        val all = (parsed + downloaded).sortedBy { it.order }.distinctBy { it.id }
        lessonCache = all
        return all
    }

    fun invalidateCache() {
        lessonCache = null
    }

    fun lesson(context: Context, id: String): Lesson? = lessons(context).firstOrNull { it.id == id }

    /**
     * 主线课程 = APK 内置的 39 讲 Python（id 形如 l01..l39，l39 为综合项目）。
     * 不能用 lang=="python" 判断：语言中心下载的 DSA / 彩蛋包同样是 python，
     * 会把「课程进度 x/39」变成 x/50，毕业口径随之失真（实测）。
     */
    fun mainline(context: Context): List<Lesson> =
        lessons(context).filter { Regex("""^l\d{2}$""").matches(it.id) }

    fun challenges(context: Context): List<Challenge> {
        challengeCache?.let { return it }
        val raw = context.assets.open("challenges.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        val parsed = buildList {
            for (i in 0 until array.length()) add(parseChallenge(array.getJSONObject(i)))
        }
        challengeCache = parsed
        return parsed
    }

    fun challenge(context: Context, id: String): Challenge? =
        challenges(context).firstOrNull { it.id == id }

    private fun parseLesson(obj: JSONObject, defaultRunnable: Boolean = true): Lesson {
        val blocks = mutableListOf<Block>()
        val blockArray = obj.getJSONArray("blocks")
        for (i in 0 until blockArray.length()) {
            val b = blockArray.getJSONObject(i)
            when (b.getString("type")) {
                "heading" -> blocks.add(Block.Heading(b.getString("text")))
                "text" -> blocks.add(Block.Paragraph(b.getString("text")))
                "tip" -> blocks.add(Block.Tip(b.getString("text")))
                "warn" -> blocks.add(Block.Warn(b.getString("text")))
                "code" -> blocks.add(Block.CodeBlock(
                    code = b.getString("code"),
                    runnable = b.optBoolean("runnable", defaultRunnable),
                    stdin = b.optJSONArray("stdin")?.let { sa ->
                        buildList { for (j in 0 until sa.length()) add(sa.getString(j)) }
                    } ?: emptyList(),
                    lang = b.optString("lang", "")
                ))
                "output" -> blocks.add(Block.Output(b.getString("text")))
                "table" -> {
                    val headers = b.getJSONArray("headers").let { ha ->
                        buildList { for (j in 0 until ha.length()) add(ha.getString(j)) }
                    }
                    val rows = mutableListOf<List<String>>()
                    val rowsArray = b.getJSONArray("rows")
                    for (r in 0 until rowsArray.length()) {
                        val row = rowsArray.getJSONArray(r)
                        rows.add(buildList { for (c in 0 until row.length()) add(row.getString(c)) })
                    }
                    blocks.add(Block.Table(headers, rows))
                }
                "diagram" -> blocks.add(Block.Diagram(b.getString("text")))
                "task" -> blocks.add(Block.Task(b.getString("text")))
                "steps" -> {
                    val items = b.getJSONArray("items").let { ia ->
                        buildList { for (j in 0 until ia.length()) add(ia.getString(j)) }
                    }
                    blocks.add(Block.Steps(items))
                }
                "practice" -> blocks.add(Block.Practice(
                    title = b.getString("title"),
                    code = b.getString("code"),
                    output = b.getString("output"),
                    hint = b.getString("hint")
                ))
                "fill" -> blocks.add(Block.Fill(
                    goal = b.getString("goal"),
                    code = b.getString("code"),
                    answer = b.getString("answer"),
                    explain = b.getString("explain")
                ))
                "order" -> {
                    val ls = b.getJSONArray("lines").let { la ->
                        buildList { for (j in 0 until la.length()) add(la.getString(j)) }
                    }
                    blocks.add(Block.Order(b.getString("title"), ls))
                }
                "compare" -> {
                    val cols = mutableListOf<Block.CompareColumn>()
                    val ca = b.optJSONArray("columns") ?: JSONArray()
                    for (j in 0 until ca.length()) {
                        val c = ca.getJSONObject(j)
                        cols.add(
                            Block.CompareColumn(
                                lang = c.optString("lang", LanguageIds.PYTHON),
                                code = c.getString("code"),
                                note = c.optString("note", ""),
                                runnable = c.optBoolean("runnable", true)
                            )
                        )
                    }
                    blocks.add(Block.Compare(b.optString("title", "多语言对照"), cols))
                }
                "visualize" -> {
                    val frames = mutableListOf<Block.VisualFrame>()
                    val fa = b.optJSONArray("frames") ?: JSONArray()
                    for (j in 0 until fa.length()) {
                        val f = fa.getJSONObject(j)
                        val items = mutableListOf<Block.VisualItem>()
                        val ia = f.optJSONArray("items") ?: JSONArray()
                        for (k in 0 until ia.length()) {
                            val it = ia.getJSONObject(k)
                            items.add(Block.VisualItem(it.getString("name"), it.getString("value")))
                        }
                        val hl = f.optJSONArray("highlight")?.let { ha ->
                            buildList { for (k in 0 until ha.length()) add(ha.getString(k)) }
                        } ?: emptyList()
                        frames.add(Block.VisualFrame(f.getString("label"), items, hl))
                    }
                    blocks.add(
                        Block.Visualize(
                            title = b.optString("title", "过程可视化"),
                            caption = b.optString("caption", ""),
                            frames = frames
                        )
                    )
                }
                "quiz" -> {
                    val options = b.getJSONArray("options").let { oa ->
                        buildList { for (j in 0 until oa.length()) add(oa.getString(j)) }
                    }
                    blocks.add(
                        Block.Quiz(
                            question = b.getString("question"),
                            options = options,
                            answerIndex = b.getInt("answer"),
                            explain = b.optString("explain")
                        )
                    )
                }
            }
        }
        val exerciseObj = obj.optJSONObject("exercise")
        return Lesson(
            id = obj.getString("id"),
            order = obj.getInt("order"),
            chapter = obj.optInt("chapter", 1),
            chapterName = obj.optString("chapterName", ""),
            title = obj.getString("title"),
            subtitle = obj.optString("subtitle"),
            xp = obj.optInt("xp", 50),
            blocks = blocks,
            exercise = exerciseObj?.let { ex ->
                Exercise(
                    title = ex.getString("title"),
                    brief = ex.getString("brief"),
                    starterCode = ex.getString("starterCode"),
                    hint = ex.optString("hint"),
                    tests = ex.getJSONArray("tests").let { ta ->
                        buildList { for (j in 0 until ta.length()) add(ta.getString(j)) }
                    },
                    stdin = ex.optJSONArray("stdin")?.let { sa ->
                        buildList { for (j in 0 until sa.length()) add(sa.getString(j)) }
                    } ?: emptyList(),
                    xp = ex.optInt("xp", 80)
                )
            },
            lang = obj.optString("lang", LanguageIds.PYTHON).ifBlank { LanguageIds.PYTHON }
        )
    }

    private fun parseChallenge(obj: JSONObject): Challenge = Challenge(
        id = obj.getString("id"),
        title = obj.getString("title"),
        difficulty = obj.optString("difficulty", "中"),
        xp = obj.optInt("xp", 100),
        brief = obj.getString("brief"),
        starterCode = obj.getString("starterCode"),
        hint = obj.optString("hint"),
        tests = obj.getJSONArray("tests").let { ta ->
            buildList { for (j in 0 until ta.length()) add(ta.getString(j)) }
        },
        stdin = emptyList(),
        lang = obj.optString("lang", LanguageIds.PYTHON).ifBlank { LanguageIds.PYTHON }
    )

    /**
     * 课程里出现过的语言 id 列表（按出现顺序去重）。
     * 用于多语言切换器：只展示有内容的语言，避免切进空课程列表。
     */
    fun languages(context: Context): List<String> =
        lessons(context).map { it.lang }.distinct()

    /** 按语言过滤课程；传 null 返回全部 */
    fun lessonsByLang(context: Context, lang: String?): List<Lesson> {
        val all = lessons(context)
        return if (lang == null) all else all.filter { it.lang == lang }
    }
}

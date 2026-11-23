package com.pyneon.academy.debug

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 应用内诊断日志。离线 App 没有服务端日志，用户只说一句「报错了」没法查，
 * 所以把运行时启动、执行失败、内容下载、主题切换这些事件记下来，
 * 用户能在设置里查看、复制、导出。
 *
 * 日志不联网，不记用户代码，只留事件、耗时和错误摘要。
 * 内存最多 500 条，文件最多 256K 字符，免得跑久了占空间。
 */
object DebugLog {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class Entry(
        val timeMs: Long,
        val level: Level,
        val tag: String,
        val message: String
    ) {
        fun format(): String {
            val t = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timeMs))
            return "$t ${level.name.first()} $tag: $message"
        }
    }

    private const val MAX_ENTRIES = 500
    private const val MAX_FILE_CHARS = 256 * 1024
    private const val FILE_NAME = "debug.log"

    private val buffer = CopyOnWriteArrayList<Entry>()

    // 用户关掉后就不再记，省点开销。
    @Volatile
    var enabled: Boolean = true

    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(Level.INFO, tag, message)
    fun w(tag: String, message: String) = log(Level.WARN, tag, message)
    fun e(tag: String, message: String) = log(Level.ERROR, tag, message)

    // 异常只留类型和消息，堆栈太长不记。
    fun e(tag: String, message: String, t: Throwable?) {
        val detail = t?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "未知"
        log(Level.ERROR, tag, "$message | $detail")
    }

    fun log(level: Level, tag: String, message: String) {
        if (!enabled) return
        val entry = Entry(System.currentTimeMillis(), level, tag, message.take(500))
        buffer.add(entry)
        while (buffer.size > MAX_ENTRIES) buffer.removeAt(0)
        if (level == Level.ERROR) android.util.Log.e(tag, message)
    }


    fun entries(): List<Entry> = buffer.toList()

    fun clear() = buffer.clear()


    fun export(): String = buildString {
        appendLine("CodeLearning 诊断日志")
        appendLine("共 ${buffer.size} 条")
        appendLine()
        buffer.forEach { appendLine(it.format()) }
    }


    fun dumpToFile(context: Context): String {
        val file = java.io.File(context.filesDir, FILE_NAME)
        val text = export()
        file.writeText(if (text.length > MAX_FILE_CHARS) text.takeLast(MAX_FILE_CHARS) else text)
        return file.absolutePath
    }


    fun counts(): Map<Level, Int> = buffer.groupingBy { it.level }.eachCount()


    fun purge(context: Context) {
        buffer.clear()
        runCatching { java.io.File(context.filesDir, FILE_NAME).delete() }
    }
}

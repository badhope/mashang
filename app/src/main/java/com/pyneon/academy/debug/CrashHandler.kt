package com.pyneon.academy.debug

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

/**
 * 全局未捕获异常处理。应用一崩系统就杀进程，用户只看到「已停止运行」，
 * 什么信息都拿不到。这里在进程死之前把类型、消息和堆栈前 40 行记下来，
 * 一份进诊断日志，一份进 SharedPreferences，下次启动可以让用户反馈。
 *
 * 只存本地，不上传。异常还是交回系统处理，不吞。
 */
object CrashHandler {

    private const val PREFS = "crash_recovery"
    private const val KEY_LAST_CRASH = "last_crash"

    // 上次崩溃的摘要，启动时给弹窗用。
    data class CrashRecord(
        val timestamp: Long,
        val type: String,
        val message: String,
        val stackHead: String
    )

    private var installed = false

    fun install(app: Application) {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                record(app, throwable)
            } catch (_: Throwable) {
                // 记录失败也不能影响原来的崩溃流程。
            }
            previous?.uncaughtException(thread, throwable)
                ?: run {
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                }
        }
    }

    private fun record(app: Application, t: Throwable) {
        val stack = t.stackTraceToString().lines().take(40).joinToString("\n")
        val type = t.javaClass.simpleName
        val msg = (t.message ?: "").take(300)

        DebugLog.e("Crash", "未捕获异常：$type | $msg")

        val payload = listOf(
            System.currentTimeMillis().toString(),
            type,
            msg.replace("\n", " "),
            stack
        ).joinToString("\u0001")

        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CRASH, payload)
            .apply()
    }


    fun lastCrash(context: Context): CrashRecord? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, null) ?: return null
        val parts = raw.split("\u0001")
        if (parts.size < 4) return null
        return CrashRecord(
            timestamp = parts[0].toLongOrNull() ?: 0L,
            type = parts[1],
            message = parts[2],
            stackHead = parts[3]
        )
    }

    // 有没有还没告诉用户的崩溃。弹一次就清；点「查看诊断」时保留记录，
    // 但由调用方在本次会话里记住已经弹过。
    fun hasPendingCrash(context: Context): Boolean = lastCrash(context) != null

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_CRASH).apply()
    }

    // 拼崩溃反馈邮件的内容，用户自己决定发不发。
    fun crashReportText(context: Context): String {
        val c = lastCrash(context) ?: return "（无崩溃记录）"
        return buildString {
            appendLine("应用版本：${com.pyneon.academy.utils.AppConstants.VERSION_NAME}")
            appendLine("崩溃类型：${c.type}")
            appendLine("错误消息：${c.message}")
            appendLine()
            appendLine("堆栈摘要：")
            appendLine(c.stackHead)
        }
    }


    fun shareByEmail(context: Context, email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "[崩溃报告] CodeLearning ${com.pyneon.academy.utils.AppConstants.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, crashReportText(context))
        }
        runCatching { context.startActivity(intent) }
    }
}

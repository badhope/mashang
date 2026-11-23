package com.pyneon.academy.utils


import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {

        // 用系统浏览器打开外链。
    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 复制到剪贴板。诊断日志导出用，不申请存储权限，也不留文件。
    fun copyText(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("CodeLearning", text))
    }

    fun sendEmail(context: Context, email: String, subject: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(Intent.createChooser(intent, "选择邮件应用"))
    }



    fun shareAchievement(context: Context, xp: Int, streak: Int, rank: String) {
        val text = buildString {
            appendLine("我在码上学的编程")
            appendLine()
            appendLine("XP：$xp")
            appendLine("连击：$streak 天")
            appendLine("段位：$rank")
            appendLine()
            appendLine("完全离线，随时能练。")
            appendLine(AppConstants.REPO_URL)
        }
        
        shareText(context, text, "分享我的学习成就")
    }
    

    private fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        
        val chooser = Intent.createChooser(intent, "分享到")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

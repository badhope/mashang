package com.pyneon.academy.data

import android.content.Context
import java.io.File

// 学习数据涉及哪些文件，只在这里写。
// 重置进度、错题本、复习卡和备份都从这里取，别三处各写一份。
object UserDataFiles {

    const val MISTAKES = "pyneon_mistakes.json"
    const val REVIEW_CARDS = "pyneon_review_cards.json"

    val names: List<String> = listOf(MISTAKES, REVIEW_CARDS)


    fun mistakesStore(context: Context): JsonListFile<MistakeRecord> =
        JsonListFile(File(context.filesDir, MISTAKES), MistakeRecord.serializer(), "Mistakes")


    fun reviewCardsStore(context: Context): JsonListFile<ReviewCard> =
        JsonListFile(File(context.filesDir, REVIEW_CARDS), ReviewCard.serializer(), "ReviewCards")

    // 删掉 filesDir 里的用户数据，返回没删成功的文件名。本来就不存在不算失败。
    fun deleteAll(filesDir: File): List<String> =
        names.filter { name ->
            val f = File(filesDir, name)
            f.exists() && !f.delete()
        }
}

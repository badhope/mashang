package com.pyneon.academy.data

import android.content.Context
import java.io.File

/**
 * 「清除全部学习数据」涉及的本地文件清单。
 *
 * 单一事实源：ProgressStore.reset、ViewModels 的错题本 / 复习卡读写、
 * BackupUtil 的备份范围都从这里取，避免三处各写一份文件名而漏删。
 */
object UserDataFiles {

    const val MISTAKES = "pyneon_mistakes.json"
    const val REVIEW_CARDS = "pyneon_review_cards.json"

    val names: List<String> = listOf(MISTAKES, REVIEW_CARDS)

    /** 错题本文件；ViewModels 与 BackupUtil 共用同一读写实现。 */
    fun mistakesStore(context: Context): JsonListFile<MistakeRecord> =
        JsonListFile(File(context.filesDir, MISTAKES), MistakeRecord.serializer(), "Mistakes")

    /** 复习卡文件；ViewModels 与 BackupUtil 共用同一读写实现。 */
    fun reviewCardsStore(context: Context): JsonListFile<ReviewCard> =
        JsonListFile(File(context.filesDir, REVIEW_CARDS), ReviewCard.serializer(), "ReviewCards")

    /**
     * 删除 [filesDir] 下的全部用户数据文件。
     * 返回删除失败的文件名（文件本来就不存在不算失败）。
     */
    fun deleteAll(filesDir: File): List<String> =
        names.filter { name ->
            val f = File(filesDir, name)
            f.exists() && !f.delete()
        }
}

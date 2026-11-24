package com.pyneon.academy.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 练习草稿的本地落盘。
 *
 * `rememberSaveable` 只能扛住旋转和系统主动保存的 Activity 状态；用户在后台
 * 被系统杀掉、或者从最近任务里划掉，草稿就没了。这里用 SharedPreferences
 * 做一层兜底，输入停下半秒后写盘，不跟 DataStore 的进度写抢锁。
 */
object DraftStore {

    private const val PREFS_NAME = "pyneon_drafts"
    private const val KEY_PREFIX = "draft_"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读取某道练习的上次输入；没有草稿或草稿为空时返回 null。 */
    fun load(context: Context, key: String): String? {
        return prefs(context).getString(KEY_PREFIX + key, null)?.takeIf { it.isNotBlank() }
    }

    /** 输入停顿后写入；空内容直接删除，避免下次打开还是空白。 */
    suspend fun save(context: Context, key: String, code: String) {
        withContext(Dispatchers.IO) {
            prefs(context).edit {
                if (code.isBlank()) {
                    remove(KEY_PREFIX + key)
                } else {
                    putString(KEY_PREFIX + key, code)
                }
            }
        }
    }

    /** 离开页面时补一次同步写入，网络/进程被杀前尽量把最后几行留住。 */
    @SuppressLint("ApplySharedPref")
    fun saveNow(context: Context, key: String, code: String) {
        // 这里是离开页面时的最后一次写入，必须同步落盘，不能等 apply 的异步队列。
        prefs(context).edit(commit = true) {
            if (code.isBlank()) {
                remove(KEY_PREFIX + key)
            } else {
                putString(KEY_PREFIX + key, code)
            }
        }
    }

    /** 清空学习数据时一起清掉，不然重置后还会恢复旧草稿。 */
    fun clearAll(context: Context) {
        prefs(context).edit { clear() }
    }
}

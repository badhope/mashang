package com.pyneon.academy.data

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal val UserDataJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/**
 * 读写用户数据列表。写入时先写临时文件再替换，避免进程被杀后留下半截 JSON。
 */
class JsonListFile<T>(
    private val file: File,
    private val elementSerializer: KSerializer<T>,
    private val logTag: String
) {
    private val listSerializer = ListSerializer(elementSerializer)

    fun read(): List<T> = try {
        if (!file.exists()) emptyList()
        else UserDataJson.decodeFromString(listSerializer, file.readText())
    } catch (e: Exception) {
        Log.e(logTag, "读取失败: ${file.name}", e)
        emptyList()
    }

    fun write(items: List<T>) {
        val text = UserDataJson.encodeToString(listSerializer, items)
        val target = file.absoluteFile
        val dir = target.parentFile ?: error("文件没有父目录: $file")
        if (!dir.exists()) dir.mkdirs()

        val tmp = File(dir, "${target.name}.tmp")
        try {
            tmp.writeText(text)
            if (!tmp.renameTo(target)) {
                // 有些文件系统不允许 rename 覆盖已有文件，只能直接写。
                target.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }
}

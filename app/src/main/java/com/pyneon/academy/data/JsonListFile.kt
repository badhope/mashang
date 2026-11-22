package com.pyneon.academy.data

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** 用户数据 JSON 的统一序列化配置（错题本 / 复习卡 / 备份共用）。 */
internal val UserDataJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/**
 * 带原子替换的 JSON 列表文件。
 *
 * 2026-11 去重：ViewModels 与 BackupUtil 曾各写一套「读文件→解析→写文件」，
 * 字段映射重复且写入不是原子的（写到一半被杀进程会留下半截 JSON）。
 * 这里统一为：先写同目录 tmp，再 rename 覆盖；失败才回退直接写。
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
        val dir = file.absoluteFile.parentFile ?: throw IllegalStateException("无父目录: $file")
        if (!dir.exists()) dir.mkdirs()
        val tmp = File(dir, "${file.name}.tmp")
        try {
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                // 少数文件系统不允许 rename 覆盖已存在文件：回退到直接写，再清理 tmp
                file.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }
}

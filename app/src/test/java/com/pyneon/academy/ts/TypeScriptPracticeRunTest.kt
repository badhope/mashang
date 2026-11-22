package com.pyneon.academy.ts

import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** TS 课程里的 practice 块也要能真跑：不能只是「看起来像代码」的静态片段。 */
class TypeScriptPracticeRunTest {

    private fun packs(): List<JSONArray> {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "content_packs").exists()) dir = dir.parentFile ?: error("找不到仓库根目录")
        return dir.resolve("content_packs").listFiles()
            ?.filter { it.name.matches(Regex("""ts-(basics|core|advanced|expert)\.json""")) }
            ?.sortedBy { it.name }
            ?.map { JSONArray(it.readText()) } ?: emptyList()
    }

    @Test
    fun `every practice block runs`() {
        val failures = mutableListOf<String>()
        var total = 0
        for (pack in packs()) {
            for (i in 0 until pack.length()) {
                val lesson = pack.getJSONObject(i)
                val blocks = lesson.getJSONArray("blocks")
                for (j in 0 until blocks.length()) {
                    val b = blocks.getJSONObject(j)
                    if (b.optString("type") != "practice") continue
                    total++
                    val r = TsBridge.runCode(b.getString("code"), emptyList(), 5.0)
                    if (!r.ok) failures.add("${lesson.getString("id")}: ${r.errorMessage}")
                }
            }
        }
        assertTrue("应有 practice 块", total > 0)
        assertTrue("以下 practice 跑不通（$total 个中 ${failures.size} 个）：$failures", failures.isEmpty())
    }
}

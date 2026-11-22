package com.pyneon.academy

import com.pyneon.academy.c.CInterpreter
import com.pyneon.academy.lua.LuaBridge
import com.pyneon.academy.ts.TsBridge
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 多语言挑战题门禁（纯 JVM）。
 *
 * 2026-11 之前挑战页只有 Python / JavaScript，C / Lua / SQL / TypeScript
 * 学完没有无提示实战。新增的挑战必须真的能被对应运行时判过：
 * 参考答案全过、起始代码不能直接过。SQL 由 tests/test_sql_lessons.py 用
 * sqlite3 覆盖，这里负责 C / Lua / TypeScript。
 */
class MultiLangChallengeTest {

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            if (File(dir, "app/src/main/assets/challenges.json").isFile) return dir
            dir = dir.parentFile ?: return@repeat
        }
        error("找不到仓库根目录（challenges.json）")
    }

    private fun challenges(lang: String): List<JSONObject> {
        val arr = JSONArray(File(repoRoot(), "app/src/main/assets/challenges.json").readText())
        return (0 until arr.length())
            .map { arr.getJSONObject(it) }
            .filter { it.optString("lang", "python") == lang }
    }

    private fun tests(obj: JSONObject): List<String> =
        obj.getJSONArray("tests").let { arr -> (0 until arr.length()).map { arr.getString(it) } }

    private fun assertChallenge(lang: String, solutionField: String, check: (String, List<String>) -> Boolean) {
        val items = challenges(lang)
        assertTrue("$lang 至少应有 1 道挑战", items.isNotEmpty())
        for (item in items) {
            val id = item.getString("id")
            val solution = item.getString(solutionField)
            val tests = tests(item)
            assertTrue("[$id] 参考答案未通过：$lang", check(solution, tests))
            assertFalse("[$id] 起始代码不应直接通过：$lang", check(item.getString("starterCode"), tests))
        }
    }

    @Test
    fun `c challenges pass on the real interpreter`() {
        assertChallenge("c", "solution") { code, tests -> CInterpreter.check(code, tests).ok }
    }

    @Test
    fun `lua challenges pass on luaj`() {
        assertChallenge("lua", "solution") { code, tests ->
            LuaBridge.checkExercise(code, tests, emptyList()).passed == true
        }
    }

    @Test
    fun `typescript challenges pass through the transpiler`() {
        assertChallenge("typescript", "solution") { code, tests ->
            TsBridge.checkExercise(code, tests, emptyList()).passed == true
        }
    }
}

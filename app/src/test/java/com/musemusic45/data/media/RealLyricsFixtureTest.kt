package com.musemusic45.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 用两首真实歌曲的内嵌歌词做回归测试。
 *
 * 这两份是用户提供的「蓝本」，格式和合成的小样例不一样，专门防住
 * 「按理想格式写解析器、遇到真歌就崩」这类问题：
 *
 *  - `holiday.lrc`（Holiday∞Holiday）
 *      逐字用**方括号**：`[行时间]字[字时间]字…`
 *      译文是**独立一行、时间戳与原文相同**
 *  - `mus.lrc`（僕らのLIVE 君とのLIFE）
 *      原文与译文在**同一行**，用**细空格 U+2009** 分隔
 */
class RealLyricsFixtureTest {

    private fun load(name: String): String {
        val stream = javaClass.getResourceAsStream("/lyrics/$name")
        assertNotNull("测试资源缺失: $name", stream)
        return stream!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    // ------------------------------------------------- 通用：不能有标签残留

    private fun assertNoTagResidue(lines: List<LrcLine>, label: String) {
        for (line in lines) {
            assertFalse(
                "$label 里残留了时间标签: '${line.text}'",
                line.text.contains('[') || line.text.contains(']') || line.text.contains('<'),
            )
            val translation = line.translation
            if (translation != null) {
                assertFalse(
                    "$label 的译文里残留了时间标签: '$translation'",
                    translation.contains('[') || translation.contains(']'),
                )
            }
        }
    }

    private fun assertSorted(lines: List<LrcLine>, label: String) {
        for (i in 1 until lines.size) {
            assertTrue(
                "$label 第 $i 行时间倒退了: ${lines[i - 1].timeMs} -> ${lines[i].timeMs}",
                lines[i].timeMs >= lines[i - 1].timeMs,
            )
        }
    }

    // ------------------------------------------- 蓝本一：方括号逐字 + 同时间戳译文

    @Test
    fun `holiday 有歌词且没有标签残留`() {
        val lines = parseLrc(load("holiday.lrc"))
        assertTrue("没解析出歌词", lines.size > 0)
        assertNoTagResidue(lines, "holiday")
        assertSorted(lines, "holiday")
    }

    @Test
    fun `holiday 每一行都是逐字`() {
        val lines = parseLrc(load("holiday.lrc"))
        val withoutWords = lines.filter { !it.hasWords }
        assertTrue("有 ${withoutWords.size} 行没有逐字信息: ${withoutWords.take(2).map { it.text }}", withoutWords.isEmpty())
    }

    /**
     * 源文件是 98 行 = 50 行逐字 + 48 行译文。
     *
     * 有两行 `Oh oh oh yeah` 后面**没有**译文 —— 这是原文件本身就缺，
     * 不是解析丢失。这里把事实钉住，将来改解析器时不会误判成 bug。
     */
    @Test
    fun `holiday 的记录数与源文件逐字行数一致`() {
        val content = load("holiday.lrc")
        val lines = parseLrc(content)
        assertEquals(98, content.lines().count { it.isNotBlank() })
        assertEquals(50, lines.size)
        assertEquals(48, lines.count { it.hasTranslation })
        assertEquals(2, lines.count { !it.hasTranslation })
        assertTrue(
            "没译文的应该是那两行 Oh oh oh yeah",
            lines.filter { !it.hasTranslation }.all { it.text.startsWith("Oh") },
        )
    }

    @Test
    fun `holiday 解析出来的第一行内容正确`() {
        val first = parseLrc(load("holiday.lrc")).first()
        // 原文是「予想出来ない一日と君はとても似ている」，译文是「难以预料的一天 就和你一样」
        assertEquals("予想出来ない一日と君はとても似ている", first.text)
        assertEquals("难以预料的一天 就和你一样", first.translation)
        assertEquals(12_372L, first.timeMs)
        assertEquals("予", first.words.first().text)
        assertEquals(12_372L, first.words.first().timeMs)
    }

    @Test
    fun `holiday 逐字片段拼起来等于整行文本`() {
        for (line in parseLrc(load("holiday.lrc"))) {
            assertEquals("逐字片段和整行文本对不上: '${line.text}'", line.text, line.words.joinToString("") { it.text })
        }
    }

    @Test
    fun `holiday 译文行不会各自成为一条记录`() {
        val lines = parseLrc(load("holiday.lrc"))
        // 48 行译文全部挂到了逐字行上，没有一条变成独立记录
        val looksLikeTranslation = lines.filter { !it.hasWords && !it.hasTranslation }
        assertTrue("有译文行变成了独立记录: ${looksLikeTranslation.map { it.text }}", looksLikeTranslation.isEmpty())
        assertEquals(48, lines.count { it.hasTranslation })
    }

    // ------------------------------------- 蓝本二：同一行里用细空格分隔原文与译文

    @Test
    fun `mus 有歌词且没有标签残留`() {
        val lines = parseLrc(load("mus.lrc"))
        assertTrue("没解析出歌词", lines.size > 0)
        assertNoTagResidue(lines, "mus")
        assertSorted(lines, "mus")
    }

    @Test
    fun `mus 细空格分隔的原文与译文被拆开`() {
        // 文件开头是作词/作曲/编曲三行，所以按时间戳定位
        val target = parseLrc(load("mus.lrc")).firstOrNull { it.timeMs == 26_970L }
        assertNotNull("没找到 00:26.97 那行", target)
        assertEquals("確かな今よりも", target!!.text)
        assertEquals("比起安于现状", target.translation)
    }

    @Test
    fun `mus 所有原文里都不再残留细空格`() {
        val lines = parseLrc(load("mus.lrc"))
        val withThinSpace = lines.filter { it.text.contains('\u2009') }
        assertTrue("还有 ${withThinSpace.size} 行残留细空格: ${withThinSpace.take(2).map { it.text }}", withThinSpace.isEmpty())
    }

    @Test
    fun `mus 带细空格的歌词行都得到了译文`() {
        val content = load("mus.lrc")
        val dotted = content.lines().count { it.contains('\u2009') }
        val withTranslation = parseLrc(content).count { it.hasTranslation }
        assertEquals("细空格行数与译文条数对不上", dotted, withTranslation)
    }

    @Test
    fun `mus 的元数据行没有被当成歌词`() {
        val lines = parseLrc(load("mus.lrc"))
        val bogus = lines.filter { it.text.contains("河北离天堂很远") || it.text.contains("BY") }
        assertTrue("元数据被当成歌词了: ${bogus.map { it.text }}", bogus.isEmpty())
    }

    @Test
    fun `mus 的作词作曲编曲行被保留为歌词`() {
        val lines = parseLrc(load("mus.lrc"))
        assertTrue("作词行丢了", lines.any { it.text.startsWith("作词") })
        assertTrue("作曲行丢了", lines.any { it.text.startsWith("作曲") })
        assertTrue("编曲行丢了", lines.any { it.text.startsWith("编曲") })
    }
}

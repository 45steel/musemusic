package com.localmusic.player.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `解析标准格式`() {
        val lines = parseLrc(
            """
            [ti:测试歌曲]
            [ar:周杰伦]
            [00:00.00]第一行
            [00:05.50]第二行
            [00:10.25]第三行
            """.trimIndent(),
        )
        assertEquals(3, lines.size)
        assertEquals(0L, lines[0].timeMs)
        assertEquals("第一行", lines[0].text)
        assertEquals(5_500L, lines[1].timeMs)
        assertEquals("第二行", lines[1].text)
        assertEquals(10_250L, lines[2].timeMs)
    }

    @Test
    fun `一行多个时间戳会展开成多条`() {
        val lines = parseLrc("[00:01.00][00:05.00]重复的一句")
        assertEquals(2, lines.size)
        assertEquals(1_000L, lines[0].timeMs)
        assertEquals(5_000L, lines[1].timeMs)
        assertEquals("重复的一句", lines[0].text)
        assertEquals("重复的一句", lines[1].text)
    }

    @Test
    fun `元数据行不会被当成歌词`() {
        val lines = parseLrc(
            """
            [ti:标题]
            [ar:歌手]
            [al:专辑]
            [by:某人]
            [00:03.00]真正的歌词
            """.trimIndent(),
        )
        assertEquals(1, lines.size)
        assertEquals("真正的歌词", lines[0].text)
    }

    @Test
    fun `毫秒位数一到三位都能解析`() {
        // 注意 00:01.5 表示 1.5 秒，也就是 1500 毫秒
        assertEquals(1_500L, parseLrc("[00:01.5]行")[0].timeMs)
        assertEquals(1_500L, parseLrc("[00:01.50]行")[0].timeMs)
        assertEquals(1_500L, parseLrc("[00:01.500]行")[0].timeMs)
        // 不足一秒的情况
        assertEquals(500L, parseLrc("[00:00.5]行")[0].timeMs)
        assertEquals(50L, parseLrc("[00:00.05]行")[0].timeMs)
    }

    @Test
    fun `冒号分隔的毫秒也支持`() {
        assertEquals(1_500L, parseLrc("[00:01:500]行")[0].timeMs)
    }

    @Test
    fun `offset 会整体平移时间`() {
        // offset 为正表示歌词提前，也就是同一句出现得更早
        val lines = parseLrc("[offset:+500]\n[00:10.00]某个字")
        assertEquals(9_500L, lines[0].timeMs)

        val later = parseLrc("[offset:-500]\n[00:10.00]某个字")
        assertEquals(10_500L, later[0].timeMs)
    }

    @Test
    fun `分钟超过两位也能解析`() {
        assertEquals(120_000L, parseLrc("[02:00.00]行")[0].timeMs)
        assertEquals(6_000_000L, parseLrc("[100:00.00]行")[0].timeMs)
    }

    @Test
    fun `结果按时间排序`() {
        val lines = parseLrc(
            """
            [00:10.00]后面
            [00:01.00]前面
            [00:05.00]中间
            """.trimIndent(),
        )
        assertEquals(listOf("前面", "中间", "后面"), lines.map { it.text })
    }

    @Test
    fun `空内容与乱码不崩溃`() {
        assertEquals(0, parseLrc("").size)
        assertEquals(0, parseLrc("   ").size)
        assertEquals(0, parseLrc("没有任何时间戳的纯文本").size)
        assertEquals(0, parseLrc("[bad:tag]").size)
    }

    @Test
    fun `时间戳但没有歌词文本也能处理`() {
        val lines = parseLrc("[00:01.00]")
        assertEquals(1, lines.size)
        assertEquals("", lines[0].text)
    }

    @Test
    fun `负时间会被夹到零`() {
        val lines = parseLrc("[offset:+90000]\n[00:10.00]行")
        assertEquals(0L, lines[0].timeMs)
    }

    @Test
    fun `识别内容是否为 LRC`() {
        assertTrue(looksLikeLrc("[00:01.00]行"))
        assertFalse(looksLikeLrc("这是纯文本歌词\n没有时间轴"))
    }

    // ------------------------------------------------------ 当前行查找

    @Test
    fun `查找当前行`() {
        val doc = LrcDocument(
            listOf(
                LrcLine(0, "A"),
                LrcLine(5_000, "B"),
                LrcLine(10_000, "C"),
                LrcLine(15_000, "D"),
            ),
        )
        assertEquals(0, doc.indexAt(0))
        assertEquals(0, doc.indexAt(4_999))
        assertEquals(1, doc.indexAt(5_000))
        assertEquals(1, doc.indexAt(9_999))
        assertEquals(2, doc.indexAt(10_000))
        assertEquals(3, doc.indexAt(99_999))
    }

    @Test
    fun `位置在首行之前返回第一行`() {
        val doc = LrcDocument(listOf(LrcLine(5_000, "A"), LrcLine(10_000, "B")))
        assertEquals(0, doc.indexAt(0))
        assertEquals(0, doc.indexAt(1_000))
    }

    @Test
    fun `没有歌词时返回负一`() {
        assertEquals(-1, LrcDocument.EMPTY.indexAt(1_000))
        assertEquals(-1, LrcDocument(emptyList()).indexAt(0))
    }

    @Test
    fun `只有一行歌词时始终返回零`() {
        val doc = LrcDocument(listOf(LrcLine(1_000, "唯一")))
        assertEquals(0, doc.indexAt(0))
        assertEquals(0, doc.indexAt(999_999))
    }
}

package com.localmusic.player.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `无效时长显示占位符`() {
        assertEquals("--:--", formatDuration(0L))
        assertEquals("--:--", formatDuration(-1L))
        assertEquals("--:--", formatDuration(-1000L))
    }

    @Test
    fun `不足一秒按零秒显示`() {
        assertEquals("0:00", formatDuration(1L))
        assertEquals("0:00", formatDuration(999L))
    }

    @Test
    fun `不足一小时显示 分冒号秒`() {
        assertEquals("0:01", formatDuration(1_000L))
        assertEquals("0:59", formatDuration(59_000L))
        assertEquals("1:00", formatDuration(60_000L))
        assertEquals("3:45", formatDuration(225_000L))
        assertEquals("59:59", formatDuration(3_599_000L))
    }

    @Test
    fun `秒数向下取整`() {
        // 59.999 秒不应该进位成 1:00
        assertEquals("0:59", formatDuration(59_999L))
        // 60.999 秒应该是 1:00
        assertEquals("1:00", formatDuration(60_999L))
    }

    @Test
    fun `一小时及以上显示 时冒号分冒号秒`() {
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("1:01:01", formatDuration(3_661_000L))
        assertEquals("23:59:59", formatDuration(86_399_000L))
    }

    @Test
    fun `年份为零时显示空串`() {
        assertEquals("", formatYear(0))
        assertEquals("", formatYear(-5))
        assertEquals("2001", formatYear(2001))
    }

    @Test
    fun `专辑副标题在有年份和无年份两种情况下都正确`() {
        assertEquals("2001 · 12 首", formatAlbumSubtitle(2001, 12))
        assertEquals("8 首", formatAlbumSubtitle(0, 8))
    }

    @Test
    fun `歌手副标题格式正确`() {
        assertEquals("8 张专辑 · 96 首歌", formatArtistSubtitle(8, 96))
        assertEquals("1 张专辑 · 1 首歌", formatArtistSubtitle(1, 1))
    }

    @Test
    fun `轮次提示格式正确`() {
        assertEquals("《范特西》· 第 3 / 37 张", formatRoundLabel("范特西", 3, 37))
        assertEquals("《未知专辑》· 第 1 / 1 张", formatRoundLabel("未知专辑", 1, 1))
    }
}

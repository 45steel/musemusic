package com.musemusic45.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackNumbersTest {

    @Test
    fun `多碟专辑的音轨号会被反算回真实值`() {
        // 实测：MediaProvider 把碟 1 轨 1 存成 1001，碟 2 轨 1 存成 2001
        assertEquals(1, normalizeTrackNumber(rawTrack = 1001, discNumber = 1))
        assertEquals(2, normalizeTrackNumber(rawTrack = 1002, discNumber = 1))
        assertEquals(3, normalizeTrackNumber(rawTrack = 1003, discNumber = 1))
        assertEquals(1, normalizeTrackNumber(rawTrack = 2001, discNumber = 2))
        assertEquals(2, normalizeTrackNumber(rawTrack = 2002, discNumber = 2))
        assertEquals(3, normalizeTrackNumber(rawTrack = 2003, discNumber = 2))
    }

    @Test
    fun `单碟专辑的音轨号原样保留`() {
        assertEquals(5, normalizeTrackNumber(rawTrack = 5, discNumber = 0))
        assertEquals(10, normalizeTrackNumber(rawTrack = 10, discNumber = 0))
        assertEquals(5, normalizeTrackNumber(rawTrack = 5, discNumber = 1))
    }

    @Test
    fun `没有音轨号时返回零`() {
        assertEquals(0, normalizeTrackNumber(rawTrack = 0, discNumber = 0))
        assertEquals(0, normalizeTrackNumber(rawTrack = 0, discNumber = 1))
        assertEquals(0, normalizeTrackNumber(rawTrack = -1, discNumber = 1))
        assertEquals(0, normalizeTrackNumber(rawTrack = -1001, discNumber = 1))
    }

    @Test
    fun `恰好等于碟号一千倍时不误判`() {
        // 1000 反算后余数是 0，不是一个合法音轨号，应当原样保留
        assertEquals(1000, normalizeTrackNumber(rawTrack = 1000, discNumber = 1))
        assertEquals(2000, normalizeTrackNumber(rawTrack = 2000, discNumber = 2))
    }

    @Test
    fun `碟号比音轨号大时不误算成负数`() {
        // 碟 3、音轨 5 → 5 小于 3000，不应该被反算
        assertEquals(5, normalizeTrackNumber(rawTrack = 5, discNumber = 3))
    }

    @Test
    fun `碟号归一化`() {
        assertEquals(1, normalizeDiscNumber(0))
        assertEquals(1, normalizeDiscNumber(-1))
        assertEquals(1, normalizeDiscNumber(1))
        assertEquals(2, normalizeDiscNumber(2))
    }
}

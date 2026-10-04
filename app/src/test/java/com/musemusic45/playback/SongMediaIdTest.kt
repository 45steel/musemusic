package com.musemusic45.playback

import com.musemusic45.data.model.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongMediaIdTest {

    @Test
    fun `歌曲 ID 与 mediaId 互相转换`() {
        val song = testSong(12345L)
        val mediaId = SongMediaId.of(song)
        assertEquals("12345", mediaId)
        assertEquals(12345L, SongMediaId.parse(mediaId))
    }

    @Test
    fun `非数字的 mediaId 解析为 null`() {
        assertNull(SongMediaId.parse("abc"))
        assertNull(SongMediaId.parse(null))
        assertNull(SongMediaId.parse(""))
    }

    @Test
    fun `在队列里查找下标`() {
        val ids = listOf("1", "2", "3", "4")
        assertEquals(0, queueIndexOf(ids, "1"))
        assertEquals(3, queueIndexOf(ids, "4"))
        assertEquals(-1, queueIndexOf(ids, "99"))
        assertEquals(-1, queueIndexOf(ids, null))
        assertEquals(-1, queueIndexOf(emptyList(), "1"))
    }

    @Test
    fun `播放位置夹取`() {
        assertEquals(-1, clampIndex(0, 0))
        assertEquals(-1, clampIndex(5, 0))
        assertEquals(0, clampIndex(-3, 5))
        assertEquals(0, clampIndex(0, 5))
        assertEquals(4, clampIndex(4, 5))
        assertEquals(4, clampIndex(99, 5))
    }
}

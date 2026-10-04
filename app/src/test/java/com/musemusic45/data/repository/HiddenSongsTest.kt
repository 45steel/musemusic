package com.musemusic45.data.repository

import com.musemusic45.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 手动移除（隐藏）歌曲的过滤。
 *
 * 要点：只影响 App 显示与播放队列，**不动任何文件**。
 */
class HiddenSongsTest {

    private fun song(id: Long, title: String) = Song(
        id = id,
        title = title,
        artist = "歌手",
        album = "专辑",
        albumId = 1L,
        discNumber = 1,
        trackNumber = id.toInt(),
        year = 2020,
        durationMs = 100_000L,
        dateAddedSec = 0L,
        path = "/music/$id.flac",
        mimeType = "audio/flac",
        sizeBytes = 1L,
    )

    private val songs = listOf(song(1, "A"), song(2, "B"), song(3, "C"))

    @Test
    fun `没有隐藏时原样返回`() {
        assertEquals(songs, HiddenSongs.visible(songs, emptySet()))
    }

    @Test
    fun `隐藏的歌被滤掉`() {
        val visible = HiddenSongs.visible(songs, setOf(2L))
        assertEquals(listOf("A", "C"), visible.map { it.title })
    }

    @Test
    fun `隐藏多首`() {
        val visible = HiddenSongs.visible(songs, setOf(1L, 3L))
        assertEquals(listOf("B"), visible.map { it.title })
    }

    @Test
    fun `不存在的隐藏 ID 不影响结果`() {
        val visible = HiddenSongs.visible(songs, setOf(99L))
        assertEquals(3, visible.size)
    }

    @Test
    fun `全部隐藏时得到空列表`() {
        assertTrue(HiddenSongs.visible(songs, setOf(1L, 2L, 3L)).isEmpty())
    }

    @Test
    fun `能列出仍在库里的隐藏歌曲`() {
        val hidden = HiddenSongs.hiddenInLibrary(songs, setOf(2L, 99L))
        assertEquals(listOf("B"), hidden.map { it.title })
    }

    @Test
    fun `没有隐藏时列出空`() {
        assertTrue(HiddenSongs.hiddenInLibrary(songs, emptySet()).isEmpty())
    }
}

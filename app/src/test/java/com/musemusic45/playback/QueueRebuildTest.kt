package com.musemusic45.playback

import com.musemusic45.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 后台被清掉后，从播放器重建「队列 → Song」的映射。
 *
 * 这个功能失效的表现很具体：音乐还在响，但迷你播放器上一个字都没有。
 */
class QueueRebuildTest {

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

    private val library = listOf(song(1, "A"), song(2, "B"), song(3, "C")).associateBy { it.id }

    /** 真实实现的兜底：用播放项自带的元数据造一条。 */
    private fun fallback(index: Int, id: Long?) = song(id ?: -1L, "占位$index")

    @Test
    fun `按播放项把队列重建回来`() {
        val queue = QueueRebuild.rebuild(listOf(1L, 2L, 3L), library, ::fallback)
        assertEquals(listOf("A", "B", "C"), queue!!.map { it.title })
    }

    @Test
    fun `顺序按播放器里的顺序来`() {
        val queue = QueueRebuild.rebuild(listOf(3L, 1L, 2L), library, ::fallback)
        assertEquals(listOf("C", "A", "B"), queue!!.map { it.title })
    }

    @Test
    fun `库里找不到的歌用占位补齐 保证下标对齐`() {
        // 第 2 首在界面上被用户移除了，但服务的队列里还有
        val queue = QueueRebuild.rebuild(listOf(1L, 99L, 3L), library, ::fallback)
        assertEquals(3, queue!!.size)
        assertEquals("A", queue[0].title)
        assertEquals("占位1", queue[1].title)
        assertEquals("C", queue[2].title)
    }

    @Test
    fun `mediaId 解析不出来时也走占位`() {
        val queue = QueueRebuild.rebuild(listOf(1L, null, 3L), library, ::fallback)
        assertEquals(3, queue!!.size)
        assertEquals("C", queue[2].title)
    }

    @Test
    fun `空队列不重建`() {
        assertNull(QueueRebuild.rebuild(emptyList(), library, ::fallback))
    }

    @Test
    fun `连占位都造不出来时宁可不重建`() {
        // 给出对不上号的队列比空着更糟：下标会指到错误的歌
        val queue = QueueRebuild.rebuild(listOf(1L, 99L), library) { _, _ -> null }
        assertNull(queue)
    }

    @Test
    fun `全部都能在库里找到`() {
        val ids = listOf(1L, 2L, 3L)
        val queue = QueueRebuild.rebuild(ids, library) { _, _ -> null }
        assertEquals(3, queue!!.size)
    }
}

package com.musemusic45.ui.albums

import com.musemusic45.data.model.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumRowsTest {

    private fun tracks(rows: List<AlbumRow>) =
        rows.filterIsInstance<AlbumRow.Track>().map { it.song.title }

    private fun discs(rows: List<AlbumRow>) =
        rows.filterIsInstance<AlbumRow.DiscHeader>().map { it.disc }

    @Test
    fun `单碟专辑不产生碟号标题`() {
        val songs = listOf(
            testSong(1, disc = 1, track = 1, title = "A"),
            testSong(2, disc = 1, track = 2, title = "B"),
            testSong(3, disc = 1, track = 3, title = "C"),
        )
        val rows = buildAlbumRows(songs)

        assertEquals(emptyList<Int>(), discs(rows))
        assertEquals(listOf("A", "B", "C"), tracks(rows))
    }

    @Test
    fun `没有碟号信息的专辑也不显示分节`() {
        val songs = listOf(
            testSong(1, disc = 0, track = 1, title = "A"),
            testSong(2, disc = 0, track = 2, title = "B"),
        )
        assertEquals(emptyList<Int>(), discs(buildAlbumRows(songs)))
    }

    @Test
    fun `多碟专辑产生碟号标题且顺序正确`() {
        val songs = listOf(
            testSong(4, disc = 2, track = 1, title = "D2T1"),
            testSong(1, disc = 1, track = 1, title = "D1T1"),
            testSong(5, disc = 2, track = 2, title = "D2T2"),
            testSong(2, disc = 1, track = 2, title = "D1T2"),
        )
        val rows = buildAlbumRows(songs)

        assertEquals(listOf(1, 2), discs(rows))
        assertEquals(listOf("D1T1", "D1T2", "D2T1", "D2T2"), tracks(rows))
    }

    @Test
    fun `曲目下标对应排序后的完整列表`() {
        val songs = listOf(
            testSong(4, disc = 2, track = 1, title = "D2T1"),
            testSong(1, disc = 1, track = 1, title = "D1T1"),
            testSong(2, disc = 1, track = 2, title = "D1T2"),
        )
        val rows = buildAlbumRows(songs)

        // 排序后应当是 D1T1(0), D1T2(1), D2T1(2)
        val pairs = rows.filterIsInstance<AlbumRow.Track>().map { it.song.title to it.index }
        assertEquals(listOf("D1T1" to 0, "D1T2" to 1, "D2T1" to 2), pairs)
    }

    @Test
    fun `每一行的 key 唯一`() {
        val songs = (1L..10L).map { testSong(it, disc = if (it > 5) 2 else 1, track = ((it - 1) % 5 + 1).toInt()) }
        val keys = buildAlbumRows(songs).map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun `没有音轨号的曲目排在所属碟的最后`() {
        val songs = listOf(
            testSong(1, disc = 1, track = 0, title = "无音轨"),
            testSong(2, disc = 1, track = 1, title = "轨1"),
            testSong(3, disc = 2, track = 1, title = "碟2轨1"),
        )
        val rows = buildAlbumRows(songs)
        assertEquals(listOf("轨1", "无音轨", "碟2轨1"), tracks(rows))
    }

    @Test
    fun `空专辑返回空列表`() {
        assertTrue(buildAlbumRows(emptyList()).isEmpty())
    }
}

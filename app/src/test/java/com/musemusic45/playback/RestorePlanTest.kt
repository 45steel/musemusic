package com.musemusic45.playback

import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 「上次播到哪儿」的恢复方案。
 *
 * 恢复最怕的边界：歌没了、位置超出时长、按专辑播放时要装回那张专辑。
 */
class RestorePlanTest {

    private fun song(
        id: Long,
        title: String,
        albumId: Long = 1L,
        durationMs: Long = 200_000L,
    ) = Song(
        id = id,
        title = title,
        artist = "歌手",
        album = "专辑$albumId",
        albumId = albumId,
        discNumber = 1,
        trackNumber = id.toInt(),
        year = 2020,
        durationMs = durationMs,
        dateAddedSec = 0L,
        path = "/music/$id.flac",
        mimeType = "audio/flac",
        sizeBytes = 1000L,
    )

    private val library = listOf(
        song(1, "A", albumId = 10),
        song(2, "B", albumId = 10),
        song(3, "C", albumId = 20),
        song(4, "D", albumId = 20),
    )

    private val albums: (Long) -> List<Song> = { albumId ->
        library.filter { it.albumId == albumId }
    }

    @Test
    fun `列表模式下装回整个音乐库`() {
        val plan = RestorePlan.plan(PlayMode.LIST_LOOP, 3L, 5_000L, library, albums)
        assertNotNull(plan)
        assertEquals(4, plan!!.songs.size)
        assertEquals(2, plan.index)
        assertEquals("C", plan.songs[plan.index].title)
        assertEquals(5_000L, plan.positionMs)
    }

    @Test
    fun `随机模式下也装回整个音乐库`() {
        val plan = RestorePlan.plan(PlayMode.SHUFFLE, 2L, 0L, library, albums)
        assertEquals(4, plan!!.songs.size)
        assertEquals(1, plan.index)
    }

    @Test
    fun `按专辑播放时装回那张专辑`() {
        val plan = RestorePlan.plan(PlayMode.ALBUM_SHUFFLE, 4L, 1_000L, library, albums)
        assertNotNull(plan)
        assertEquals(2, plan!!.songs.size)
        assertEquals(1, plan.index)
        assertEquals("D", plan.songs[plan.index].title)
    }

    @Test
    fun `歌曲已经不在库里时返回空`() {
        assertNull(RestorePlan.plan(PlayMode.LIST_LOOP, 999L, 0L, library, albums))
    }

    @Test
    fun `空音乐库返回空`() {
        assertNull(RestorePlan.plan(PlayMode.LIST_LOOP, 1L, 0L, emptyList(), albums))
    }

    @Test
    fun `播放位置超过时长时被夹住`() {
        val short = listOf(song(7, "短歌", durationMs = 30_000L))
        val plan = RestorePlan.plan(PlayMode.LIST_LOOP, 7L, 999_999L, short) { emptyList() }
        assertEquals(30_000L, plan!!.positionMs)
    }

    @Test
    fun `时长未知时不夹位置只保证非负`() {
        val unknown = listOf(song(8, "未知时长", durationMs = 0L))
        val plan = RestorePlan.plan(PlayMode.LIST_LOOP, 8L, 12_345L, unknown) { emptyList() }
        assertEquals(12_345L, plan!!.positionMs)

        val negative = RestorePlan.plan(PlayMode.LIST_LOOP, 8L, -5L, unknown) { emptyList() }
        assertEquals(0L, negative!!.positionMs)
    }

    @Test
    fun `专辑取不到曲目时退回整个音乐库`() {
        val plan = RestorePlan.plan(PlayMode.ALBUM_SHUFFLE, 1L, 0L, library) { emptyList() }
        assertNotNull(plan)
        assertEquals(4, plan!!.songs.size)
        assertEquals(0, plan.index)
    }
}

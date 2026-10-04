package com.musemusic45.data.repository

import com.musemusic45.data.model.Song
import com.musemusic45.data.model.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAggregatorTest {

    // ---------- 专辑聚合 ----------

    @Test
    fun `同一张专辑的歌会被聚合到一起`() {
        val songs = listOf(
            testSong(1, albumId = 10, album = "范特西", artist = "周杰伦", track = 1),
            testSong(2, albumId = 10, album = "范特西", artist = "周杰伦", track = 2),
            testSong(3, albumId = 20, album = "叶惠美", artist = "周杰伦", track = 1),
        )
        val albums = LibraryAggregator.albums(songs)

        assertEquals(2, albums.size)
        val fantacy = albums.first { it.id == 10L }
        assertEquals("范特西", fantacy.name)
        assertEquals(2, fantacy.songCount)
        assertEquals("周杰伦", fantacy.artist)
    }

    @Test
    fun `专辑年份取最早的有效年份`() {
        val songs = listOf(
            testSong(1, albumId = 10, year = 0),
            testSong(2, albumId = 10, year = 2003),
            testSong(3, albumId = 10, year = 2001),
        )
        assertEquals(2001, LibraryAggregator.albums(songs).first().year)
    }

    @Test
    fun `专辑全部没有年份时年份为零`() {
        val songs = listOf(testSong(1, albumId = 10, year = 0), testSong(2, albumId = 10, year = 0))
        assertEquals(0, LibraryAggregator.albums(songs).first().year)
    }

    @Test
    fun `专辑内歌手不一致时显示多位歌手`() {
        val songs = listOf(
            testSong(1, albumId = 10, artist = "周杰伦"),
            testSong(2, albumId = 10, artist = "费玉清"),
        )
        assertEquals(LibraryAggregator.VARIOUS_ARTISTS, LibraryAggregator.albums(songs).first().artist)
    }

    @Test
    fun `专辑歌手标签一致时优先用它`() {
        // 合辑里每首歌的歌手都不同，但专辑歌手相同，应当显示专辑歌手
        val songs = listOf(
            testSong(1, albumId = 10, artist = "周杰伦", albumArtist = "群星"),
            testSong(2, albumId = 10, artist = "林俊杰", albumArtist = "群星"),
        )
        assertEquals("群星", LibraryAggregator.albums(songs).first().artist)
    }

    @Test
    fun `专辑歌手标签缺失时退回单曲歌手`() {
        val songs = listOf(
            testSong(1, albumId = 10, artist = "周杰伦", albumArtist = ""),
            testSong(2, albumId = 10, artist = "周杰伦", albumArtist = ""),
        )
        assertEquals("周杰伦", LibraryAggregator.albums(songs).first().artist)
    }

    @Test
    fun `专辑添加时间取专辑内最早的那首`() {
        val songs = listOf(
            testSong(1, albumId = 10, dateAddedSec = 1_700_000_500L),
            testSong(2, albumId = 10, dateAddedSec = 1_700_000_100L),
        )
        assertEquals(1_700_000_100L, LibraryAggregator.albums(songs).first().dateAddedSec)
    }

    // ---------- 歌手聚合 ----------

    @Test
    fun `歌手聚合统计专辑数与歌曲数`() {
        val songs = listOf(
            testSong(1, artist = "周杰伦", albumId = 10),
            testSong(2, artist = "周杰伦", albumId = 10),
            testSong(3, artist = "周杰伦", albumId = 20),
            testSong(4, artist = "王菲", albumId = 30),
        )
        val artists = LibraryAggregator.artists(songs)

        assertEquals(2, artists.size)
        val jay = artists.first { it.name == "周杰伦" }
        assertEquals(2, jay.albumCount)
        assertEquals(3, jay.songCount)

        val faye = artists.first { it.name == "王菲" }
        assertEquals(1, faye.albumCount)
        assertEquals(1, faye.songCount)
    }

    @Test
    fun `同名专辑但专辑 ID 不同会算成两张专辑`() {
        val songs = listOf(
            testSong(1, artist = "多人", album = "精选集", albumId = 10),
            testSong(2, artist = "多人", album = "精选集", albumId = 20),
        )
        assertEquals(2, LibraryAggregator.artists(songs).first().albumCount)
    }

    // ---------- 专辑内排序 ----------

    @Test
    fun `专辑内先按碟号再按音轨号排序`() {
        val songs = listOf(
            testSong(4, disc = 2, track = 1, title = "碟2轨1"),
            testSong(1, disc = 1, track = 2, title = "碟1轨2"),
            testSong(5, disc = 2, track = 2, title = "碟2轨2"),
            testSong(2, disc = 1, track = 3, title = "碟1轨3"),
            testSong(3, disc = 1, track = 1, title = "碟1轨1"),
        )
        val sorted = LibraryAggregator.sortAlbumTracks(songs).map { it.title }

        assertEquals(listOf("碟1轨1", "碟1轨2", "碟1轨3", "碟2轨1", "碟2轨2"), sorted)
    }

    @Test
    fun `没有音轨号的歌排在所属碟的最后`() {
        val songs = listOf(
            testSong(1, disc = 1, track = 0, title = "无音轨"),
            testSong(2, disc = 1, track = 1, title = "轨1"),
            testSong(3, disc = 1, track = 5, title = "轨5"),
        )
        val sorted = LibraryAggregator.sortAlbumTracks(songs).map { it.title }

        assertEquals(listOf("轨1", "轨5", "无音轨"), sorted)
    }

    @Test
    fun `没有碟号的歌当作第一碟`() {
        // 无碟号的歌（track=5）应当和真正的碟 1 歌（track=9）排在同一组里，
        // 并且按音轨号排在它前面，而不是被排到碟 2 之后。
        val songs = listOf(
            testSong(1, disc = 0, track = 5, title = "无碟号"),
            testSong(2, disc = 2, track = 1, title = "碟2"),
            testSong(3, disc = 1, track = 9, title = "碟1"),
        )
        val sorted = LibraryAggregator.sortAlbumTracks(songs).map { it.title }

        assertEquals(listOf("无碟号", "碟1", "碟2"), sorted)
    }

    @Test
    fun `碟号和音轨号都相同时按歌名兜底`() {
        val songs = listOf(
            testSong(1, disc = 1, track = 1, title = "B歌"),
            testSong(2, disc = 1, track = 1, title = "A歌"),
        )
        val sorted = LibraryAggregator.sortAlbumTracks(songs).map { it.title }
        assertEquals(listOf("A歌", "B歌"), sorted)
    }

    @Test
    fun `空列表不会出错`() {
        assertEquals(emptyList<Song>(), LibraryAggregator.sortAlbumTracks(emptyList()))
        assertEquals(0, LibraryAggregator.albums(emptyList()).size)
        assertEquals(0, LibraryAggregator.artists(emptyList()).size)
    }

    // ---------- 碟号分节 ----------

    @Test
    fun `单碟专辑不需要碟号标题`() {
        val songs = listOf(testSong(1, disc = 1, track = 1), testSong(2, disc = 1, track = 2))
        assertFalse(LibraryAggregator.needsDiscHeaders(songs))
    }

    @Test
    fun `没有碟号信息的专辑不需要碟号标题`() {
        val songs = listOf(testSong(1, disc = 0, track = 1), testSong(2, disc = 0, track = 2))
        assertFalse(LibraryAggregator.needsDiscHeaders(songs))
    }

    @Test
    fun `多碟专辑需要碟号标题`() {
        val songs = listOf(testSong(1, disc = 1, track = 1), testSong(2, disc = 2, track = 1))
        assertTrue(LibraryAggregator.needsDiscHeaders(songs))
    }

    @Test
    fun `按碟号分组且组内有序`() {
        val songs = listOf(
            testSong(4, disc = 2, track = 2, title = "D2T2"),
            testSong(1, disc = 1, track = 2, title = "D1T2"),
            testSong(3, disc = 2, track = 1, title = "D2T1"),
            testSong(2, disc = 1, track = 1, title = "D1T1"),
        )
        val groups = LibraryAggregator.groupByDisc(songs)
        assertEquals(listOf(1, 2), groups.map { it.first })
        assertEquals(listOf("D1T1", "D1T2"), groups[0].second.map { it.title })
        assertEquals(listOf("D2T1", "D2T2"), groups[1].second.map { it.title })
    }

    // ---------- 歌手详情排序 ----------

    @Test
    fun `歌手歌曲按年份再按专辑排列`() {
        val songs = listOf(
            testSong(1, title = "新专辑的歌", album = "B专辑", albumId = 2, year = 2010, track = 1),
            testSong(2, title = "旧专辑的歌", album = "A专辑", albumId = 1, year = 2001, track = 1),
            testSong(3, title = "无年份的歌", album = "C专辑", albumId = 3, year = 0, track = 1),
        )
        val sorted = LibraryAggregator.sortArtistSongs(songs).map { it.title }
        assertEquals(listOf("旧专辑的歌", "新专辑的歌", "无年份的歌"), sorted)
    }

    @Test
    fun `同年份的歌手歌曲按专辑名再按音轨号`() {
        val songs = listOf(
            testSong(1, title = "B-2", album = "B", albumId = 2, year = 2005, track = 2),
            testSong(2, title = "A-2", album = "A", albumId = 1, year = 2005, track = 2),
            testSong(3, title = "A-1", album = "A", albumId = 1, year = 2005, track = 1),
        )
        assertEquals(
            listOf("A-1", "A-2", "B-2"),
            LibraryAggregator.sortArtistSongs(songs).map { it.title },
        )
    }

    // ------------------------------------ 第二版：多歌手拆分与归一化

    @Test
    fun `多歌手歌曲分别计入每一位歌手`() {
        val songs = listOf(
            testSong(1, artist = "周杰伦、费玉清"),
            testSong(2, artist = "周杰伦"),
        )
        val artists = LibraryAggregator.artists(songs)

        assertEquals(setOf("周杰伦", "费玉清"), artists.map { it.name }.toSet())
        assertEquals(2, artists.first { it.name == "周杰伦" }.songCount)
        assertEquals(1, artists.first { it.name == "费玉清" }.songCount)
    }

    @Test
    fun `多歌手不会生成合并条目`() {
        val songs = listOf(testSong(1, artist = "周杰伦、费玉清"))
        val artists = LibraryAggregator.artists(songs)
        assertTrue(artists.none { it.name == "周杰伦、费玉清" })
        assertEquals(2, artists.size)
    }

    @Test
    fun `歌手名括号内容被忽略后会合并`() {
        val songs = listOf(
            testSong(1, artist = "某某（2019）"),
            testSong(2, artist = "某某"),
        )
        val artists = LibraryAggregator.artists(songs)

        assertEquals(listOf("某某"), artists.map { it.name })
        assertEquals(2, artists.first().songCount)
    }

    @Test
    fun `拆分后的每项都会去括号`() {
        val songs = listOf(testSong(1, artist = "A（x）、B（y）"))
        assertEquals(setOf("A", "B"), LibraryAggregator.artists(songs).map { it.name }.toSet())
    }

    @Test
    fun `多歌手歌曲的专辑数在每位歌手名下都计入`() {
        val songs = listOf(
            testSong(1, artist = "A、B", albumId = 10),
            testSong(2, artist = "A、B", albumId = 20),
        )
        val artists = LibraryAggregator.artists(songs)
        assertEquals(2, artists.first { it.name == "A" }.albumCount)
        assertEquals(2, artists.first { it.name == "B" }.albumCount)
    }

    @Test
    fun `专辑歌手标签的括号内容被忽略`() {
        val songs = listOf(
            testSong(1, artist = "x", albumArtist = "某某（合辑）"),
            testSong(2, artist = "y", albumArtist = "某某"),
        )
        // 两者归一化后都是「某某」，属于同一张专辑的同一歌手
        assertEquals("某某", LibraryAggregator.albumArtist(songs))
    }

    @Test
    fun `专辑歌手只有括号差异时不再算多位歌手`() {
        val songs = listOf(
            testSong(1, artist = "区别1", albumArtist = "组合（早期）"),
            testSong(2, artist = "区别2", albumArtist = "组合（后期）"),
        )
        assertEquals("组合", LibraryAggregator.albumArtist(songs))
    }

    @Test
    fun `多歌手标签的专辑歌手仍按标签取`() {
        val songs = listOf(
            testSong(1, artist = "A、B", albumArtist = "合辑名"),
            testSong(2, artist = "C", albumArtist = "合辑名"),
        )
        assertEquals("合辑名", LibraryAggregator.albumArtist(songs))
    }

    @Test
    fun `空歌手名归入未知歌手`() {
        val songs = listOf(testSong(1, artist = ""))
        assertEquals(listOf(Song.UNKNOWN_ARTIST), LibraryAggregator.artists(songs).map { it.name })
    }

    // ------------------------------------ 第二版：歌手头像取最早年份的专辑

    @Test
    fun `歌手头像取发布年份最早的专辑`() {
        val songs = listOf(
            testSong(1, artist = "周杰伦", album = "新专辑", albumId = 20, year = 2016),
            testSong(2, artist = "周杰伦", album = "老专辑", albumId = 10, year = 2001),
            testSong(3, artist = "周杰伦", album = "中间", albumId = 30, year = 2008),
        )
        val jay = LibraryAggregator.artists(songs).first { it.name == "周杰伦" }

        assertEquals(10L, jay.coverAlbumId)
        assertEquals(2001, jay.coverYear)
    }

    @Test
    fun `有年份的专辑优先于没有年份的`() {
        val songs = listOf(
            testSong(1, artist = "A", album = "无年份", albumId = 5, year = 0, dateAddedSec = 100),
            testSong(2, artist = "A", album = "有年份", albumId = 9, year = 2020, dateAddedSec = 900),
        )
        // 无年份那张加入得更早，但它没有年份，应当排在后面
        assertEquals(9L, LibraryAggregator.artists(songs).first().coverAlbumId)
    }

    @Test
    fun `全部专辑都没有年份时按加入时间兜底`() {
        val songs = listOf(
            testSong(1, artist = "A", albumId = 7, year = 0, dateAddedSec = 500),
            testSong(2, artist = "A", albumId = 3, year = 0, dateAddedSec = 100),
        )
        val a = LibraryAggregator.artists(songs).first()
        assertEquals(3L, a.coverAlbumId)
        assertEquals(0, a.coverYear)
    }

    @Test
    fun `多歌手歌曲的两位歌手拿到同一个封面`() {
        val songs = listOf(
            testSong(1, artist = "A、B", albumId = 10, year = 2010),
            testSong(2, artist = "A、B", albumId = 20, year = 2005),
        )
        val artists = LibraryAggregator.artists(songs)
        assertEquals(20L, artists.first { it.name == "A" }.coverAlbumId)
        assertEquals(20L, artists.first { it.name == "B" }.coverAlbumId)
    }

    @Test
    fun `专辑 ID 为零时没有封面可用`() {
        val songs = listOf(testSong(1, artist = "A", albumId = 0L, year = 2000))
        assertEquals(0L, LibraryAggregator.artists(songs).first().coverAlbumId)
    }

    @Test
    fun `earliestAlbum 对空列表返回 null`() {
        assertEquals(null, LibraryAggregator.earliestAlbum(emptyList()))
    }

    @Test
    fun `earliestAlbum 年份相同时按加入时间再按名字`() {
        val albums = listOf(
            com.musemusic45.data.model.Album(2, "B", "x", 2000, 1, 5),
            com.musemusic45.data.model.Album(1, "A", "x", 2000, 1, 5),
        )
        assertEquals(1L, LibraryAggregator.earliestAlbum(albums)?.id)
    }
}

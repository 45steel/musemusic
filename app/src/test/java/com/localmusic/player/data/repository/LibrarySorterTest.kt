package com.localmusic.player.data.repository

import com.localmusic.player.data.model.SortField
import com.localmusic.player.data.model.SortOrder
import com.localmusic.player.data.model.SortSpec
import com.localmusic.player.data.model.defaultOrderFor
import com.localmusic.player.data.model.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySorterTest {

    // ------------------------------------------------------------ 中文拼音

    @Test
    fun `中文按拼音排序而不是按码位`() {
        // 码位上 阿(U+963F) < 张(U+5F20) 不成立，拼音上 a < zhang 成立
        val songs = listOf(
            testSong(1, title = "张灯结彩"),
            testSong(2, title = "阿婆"),
            testSong(3, title = "白天"),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.NAME, SortOrder.ASCENDING))
        assertEquals(listOf("阿婆", "白天", "张灯结彩"), sorted.map { it.title })
    }

    @Test
    fun `名称降序`() {
        val songs = listOf(
            testSong(1, title = "阿婆"),
            testSong(2, title = "张灯结彩"),
            testSong(3, title = "白天"),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.NAME, SortOrder.DESCENDING))
        assertEquals(listOf("张灯结彩", "白天", "阿婆"), sorted.map { it.title })
    }

    @Test
    fun `中英文混排也能排`() {
        val songs = listOf(
            testSong(1, title = "Beyond"),
            testSong(2, title = "阿"),
            testSong(3, title = "Zoo"),
        )
        // 不关心字母和汉字谁前谁后，只要求结果稳定且可重复
        val first = LibrarySorter.songs(songs, SortSpec(SortField.NAME, SortOrder.ASCENDING)).map { it.title }
        val second = LibrarySorter.songs(songs, SortSpec(SortField.NAME, SortOrder.ASCENDING)).map { it.title }
        assertEquals(first, second)
        assertEquals(3, first.size)
    }

    // ------------------------------------------------------------ 添加时间

    @Test
    fun `添加时间降序是最新在前`() {
        val songs = listOf(
            testSong(1, title = "旧", dateAddedSec = 1_600_000_000L),
            testSong(2, title = "新", dateAddedSec = 1_800_000_000L),
            testSong(3, title = "中", dateAddedSec = 1_700_000_000L),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.DATE_ADDED, SortOrder.DESCENDING))
        assertEquals(listOf("新", "中", "旧"), sorted.map { it.title })
    }

    @Test
    fun `添加时间升序是最早在前`() {
        val songs = listOf(
            testSong(1, title = "旧", dateAddedSec = 1_600_000_000L),
            testSong(2, title = "新", dateAddedSec = 1_800_000_000L),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.DATE_ADDED, SortOrder.ASCENDING))
        assertEquals(listOf("旧", "新"), sorted.map { it.title })
    }

    // ------------------------------------------------------------ 发布年份

    @Test
    fun `发布年份升序`() {
        val songs = listOf(
            testSong(1, title = "B", year = 2003),
            testSong(2, title = "A", year = 1993),
            testSong(3, title = "C", year = 2016),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.YEAR, SortOrder.ASCENDING))
        assertEquals(listOf("A", "B", "C"), sorted.map { it.title })
    }

    @Test
    fun `没有年份的歌在升序时排最后`() {
        val songs = listOf(
            testSong(1, title = "无年份", year = 0),
            testSong(2, title = "有年份", year = 2001),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.YEAR, SortOrder.ASCENDING))
        assertEquals(listOf("有年份", "无年份"), sorted.map { it.title })
    }

    @Test
    fun `没有年份的歌在降序时同样排最后`() {
        // 这是关键：切降序不能把缺数据的顶到最前面
        val songs = listOf(
            testSong(1, title = "无年份", year = 0),
            testSong(2, title = "老歌", year = 1993),
            testSong(3, title = "新歌", year = 2016),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.YEAR, SortOrder.DESCENDING))
        assertEquals(listOf("新歌", "老歌", "无年份"), sorted.map { it.title })
    }

    @Test
    fun `多首无年份的歌保持相对顺序`() {
        val songs = listOf(
            testSong(1, title = "甲", year = 0),
            testSong(2, title = "乙", year = 2001),
            testSong(3, title = "丙", year = 0),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.YEAR, SortOrder.ASCENDING))
        assertEquals(listOf("乙", "甲", "丙"), sorted.map { it.title })
    }

    // -------------------------------------------------------------- 边缘

    @Test
    fun `标题为空的歌排在最后`() {
        val songs = listOf(
            testSong(1, title = ""),
            testSong(2, title = "有名字"),
        )
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.NAME, SortOrder.ASCENDING))
        assertEquals(listOf("有名字", ""), sorted.map { it.title })
    }

    @Test
    fun `空列表和单元素列表`() {
        assertEquals(0, LibrarySorter.songs(emptyList(), SortSpec.DEFAULT).size)
        val one = listOf(testSong(1))
        assertEquals(one, LibrarySorter.songs(one, SortSpec.DEFAULT))
    }

    @Test
    fun `排序不会丢失或重复元素`() {
        val songs = (1L..50L).map { testSong(it, title = "歌$it", year = (it % 5).toInt() * 100) }
        val sorted = LibrarySorter.songs(songs, SortSpec(SortField.YEAR, SortOrder.DESCENDING))
        assertEquals(50, sorted.size)
        assertEquals(songs.map { it.id }.toSet(), sorted.map { it.id }.toSet())
    }

    // ------------------------------------------------------------ 专辑歌手

    @Test
    fun `专辑排序`() {
        val albums = listOf(
            com.localmusic.player.data.model.Album(1, "范特西", "周杰伦", 2001, 10, 300),
            com.localmusic.player.data.model.Album(2, "唱游", "王菲", 1998, 10, 100),
            com.localmusic.player.data.model.Album(3, "无年份专辑", "未知", 0, 1, 200),
        )
        val byName = LibrarySorter.albums(albums, SortSpec(SortField.NAME, SortOrder.ASCENDING))
        assertEquals(listOf("唱游", "范特西", "无年份专辑"), byName.map { it.name })

        val byYear = LibrarySorter.albums(albums, SortSpec(SortField.YEAR, SortOrder.DESCENDING))
        assertEquals(listOf("范特西", "唱游", "无年份专辑"), byYear.map { it.name })

        val byAdded = LibrarySorter.albums(albums, SortSpec(SortField.DATE_ADDED, SortOrder.DESCENDING))
        assertEquals(listOf("范特西", "无年份专辑", "唱游"), byAdded.map { it.name })
    }

    @Test
    fun `歌手排序`() {
        val artists = listOf(
            com.localmusic.player.data.model.Artist("周杰伦", 2, 20, 300),
            com.localmusic.player.data.model.Artist("王菲", 1, 10, 100),
        )
        val byName = LibrarySorter.artists(artists, SortSpec(SortField.NAME, SortOrder.ASCENDING))
        assertEquals(listOf("王菲", "周杰伦"), byName.map { it.name })

        val byAdded = LibrarySorter.artists(artists, SortSpec(SortField.DATE_ADDED, SortOrder.DESCENDING))
        assertEquals(listOf("周杰伦", "王菲"), byAdded.map { it.name })
    }

    // -------------------------------------------------------------- 设定

    @Test
    fun `顶栏排序文字`() {
        assertEquals("名称 A→Z", SortSpec(SortField.NAME, SortOrder.ASCENDING).displayLabel)
        assertEquals("名称 Z→A", SortSpec(SortField.NAME, SortOrder.DESCENDING).displayLabel)
        assertEquals("添加时间 最新", SortSpec(SortField.DATE_ADDED, SortOrder.DESCENDING).displayLabel)
        assertEquals("添加时间 最早", SortSpec(SortField.DATE_ADDED, SortOrder.ASCENDING).displayLabel)
        assertEquals("发布年份 最新", SortSpec(SortField.YEAR, SortOrder.DESCENDING).displayLabel)
        assertEquals("发布年份 最早", SortSpec(SortField.YEAR, SortOrder.ASCENDING).displayLabel)
    }

    @Test
    fun `切换升降序`() {
        val spec = SortSpec(SortField.NAME, SortOrder.ASCENDING)
        assertEquals(SortOrder.DESCENDING, spec.toggled().order)
        assertEquals(SortOrder.ASCENDING, spec.toggled().toggled().order)
        // 字段不能变
        assertEquals(SortField.NAME, spec.toggled().field)
    }

    @Test
    fun `字段的默认方向`() {
        assertEquals(SortOrder.ASCENDING, defaultOrderFor(SortField.NAME))
        assertEquals(SortOrder.DESCENDING, defaultOrderFor(SortField.DATE_ADDED))
        assertEquals(SortOrder.DESCENDING, defaultOrderFor(SortField.YEAR))
    }

    @Test
    fun `歌手只提供两个排序字段`() {
        assertEquals(2, SortSpec.ARTIST_FIELDS.size)
        assertTrue(SortSpec.ARTIST_FIELDS.none { it == SortField.YEAR })
        assertEquals(3, SortSpec.SONG_FIELDS.size)
        assertTrue(SortSpec.SONG_FIELDS.contains(SortField.YEAR))
    }
}

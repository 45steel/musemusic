package com.musemusic45.data.repository

import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Artist
import com.musemusic45.data.model.ArtistNames
import com.musemusic45.data.model.Song
import com.musemusic45.data.model.compareByName

/**
 * 把歌曲列表聚合成专辑和歌手。
 *
 * 全是纯函数，不依赖 Android，便于单元测试。
 */
object LibraryAggregator {

    /** 专辑内出现多个歌手时，专辑卡片上显示的歌手名。 */
    const val VARIOUS_ARTISTS = "多位歌手"

    fun albums(songs: List<Song>): List<Album> =
        songs.groupBy { it.albumId }
            .map { (albumId, group) ->
                Album(
                    id = albumId,
                    name = group.first().album,
                    artist = albumArtist(group),
                    year = albumYear(group),
                    songCount = group.size,
                    dateAddedSec = group.minOf { it.dateAddedSec },
                )
            }

    /**
     * 按歌手聚合。
     *
     * 第二版起：一首歌如果有多名歌手（`周杰伦、费玉清`），会**分别计入两位歌手**，
     * 而不是生成一个只此一家的「周杰伦、费玉清」条目。
     * 歌手名先做归一化（去掉括号内容），所以「某某（xxx）」和「某某」会合并。
     */
    fun artists(songs: List<Song>): List<Artist> {
        val byName = songs.flatMap { song -> song.artistNames.map { name -> name to song } }
            .groupBy({ it.first }, { it.second })
        val albumById = albums(songs).associateBy { it.id }

        return byName.map { (name, group) ->
            val albumIds = group.map { it.albumId }.distinct()
            val cover = earliestAlbum(albumIds.mapNotNull { albumById[it] })
            Artist(
                name = name,
                albumCount = albumIds.size,
                songCount = group.size,
                dateAddedSec = group.minOf { it.dateAddedSec },
                coverAlbumId = cover?.id ?: 0L,
                coverYear = cover?.year ?: 0,
            )
        }
    }

    /**
     * 该歌手**发布年份最早**的专辑，用作头像（第二版新增）。
     *
     * 排序键：有年份的按年份升序排在前面，年份未知的垫底；
     * 再按加入时间、最后按专辑名兜底 —— 只要名下有专辑就一定返回一张，
     * 不会因为「所有专辑都没年份」而返回 null。
     */
    fun earliestAlbum(albums: List<Album>): Album? =
        albums.minWithOrNull(
            compareBy<Album> { if (it.year > 0) it.year else Int.MAX_VALUE }
                .thenBy { it.dateAddedSec }
                .thenBy { it.name },
        )

    /**
     * 专辑的发布年份：取该专辑内所有有效年份里最早的一个。
     * 全部没有年份时返回 0。
     */
    fun albumYear(songs: List<Song>): Int =
        songs.map { it.year }.filter { it > 0 }.minOrNull() ?: 0

    /**
     * 专辑的歌手。
     *
     * 优先用专辑歌手标签（MediaStore 的 album_artist）——合辑里每首歌的歌手都不同，
     * 只有专辑歌手才能把它们归成一张专辑。标签缺失时退回单曲歌手，
     * 仍然不一致就显示「多位歌手」。
     */
    fun albumArtist(songs: List<Song>): String {
        val tagged = songs.map { ArtistNames.normalize(it.albumArtist) }
            .filter { it.isNotBlank() && it != Song.UNKNOWN_ARTIST }
            .distinct()
        if (tagged.size == 1) return tagged.first()

        // 归一化后再比，避免「A（x）」和「A（y）」被当成两位歌手
        val distinct = songs.map { ArtistNames.normalize(it.artist) }.distinct()
        return when {
            distinct.isEmpty() -> Song.UNKNOWN_ARTIST
            distinct.size == 1 -> distinct.first()
            else -> VARIOUS_ARTISTS
        }
    }

    /**
     * 专辑内曲目排序：先碟号，再音轨号，最后歌名兜底。
     *
     * 规则（对照需求）：
     *  - 没有碟号的按第 1 碟处理
     *  - **没有音轨号的排在所属碟的最后**
     */
    fun sortAlbumTracks(songs: List<Song>): List<Song> =
        songs.sortedWith(
            compareBy<Song> { normalizeDisc(it) }
                .thenBy { if (it.hasTrack) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title }
        )

    private fun normalizeDisc(song: Song): Int = if (song.hasDisc) song.discNumber else 1

    /**
     * 按碟号把曲目分组，用于专辑详情页的分节显示。
     *
     * 返回的每一组都已经按音轨号排好序；组本身按碟号升序。
     */
    fun groupByDisc(songs: List<Song>): List<Pair<Int, List<Song>>> =
        sortAlbumTracks(songs)
            .groupBy { normalizeDisc(it) }
            .toSortedMap()
            .map { (disc, group) -> disc to group }

    /**
     * 这张专辑是否需要显示碟号分节标题。
     * 单碟专辑不显示「碟 1」这种标题。
     */
    fun needsDiscHeaders(songs: List<Song>): Boolean =
        songs.map { normalizeDisc(it) }.distinct().size > 1

    /**
     * 歌手详情页里「全部歌曲」的排序：先按年份，再按专辑名，最后按碟号音轨号。
     * 这样同一张专辑的歌会聚在一起，整体呈时间顺序。
     */
    fun sortArtistSongs(songs: List<Song>): List<Song> =
        songs.sortedWith(
            compareBy<Song> { if (it.hasYear) it.year else Int.MAX_VALUE }
                .thenComparator { a, b -> compareByName(a.album, b.album) }
                .thenBy { normalizeDisc(it) }
                .thenBy { if (it.hasTrack) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title }
        )
}

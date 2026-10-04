package com.musemusic45.ui.albums

import com.musemusic45.data.model.Song
import com.musemusic45.data.repository.LibraryAggregator

/**
 * 专辑详情里的一行。
 *
 * 预先摊平成一维列表，避免在 LazyColumn 的懒组合闭包里读取可变下标
 * （闭包会在真正组合时才求值，那时累加变量已经是最终值了）。
 */
sealed interface AlbumRow {
    val key: String

    data class DiscHeader(val disc: Int) : AlbumRow {
        override val key = "disc-$disc"
    }

    data class Track(val song: Song, val index: Int) : AlbumRow {
        override val key = "track-${song.id}"
    }
}

/**
 * 把专辑曲目摊平成界面行：碟号分节标题 + 曲目。
 *
 * - 单碟专辑**不产生**分节标题
 * - 曲目按碟号、音轨号升序
 * - [AlbumRow.Track.index] 是它在排序后完整列表里的下标，正好对应播放队列的下标
 *
 * 纯函数，便于单元测试。
 */
fun buildAlbumRows(songs: List<Song>): List<AlbumRow> {
    val sorted = LibraryAggregator.sortAlbumTracks(songs)
    val indexById = sorted.withIndex().associate { (index, song) -> song.id to index }
    val groups = LibraryAggregator.groupByDisc(sorted)
    val showHeaders = LibraryAggregator.needsDiscHeaders(sorted)

    return buildList {
        groups.forEach { (disc, discSongs) ->
            if (showHeaders) add(AlbumRow.DiscHeader(disc))
            discSongs.forEach { song ->
                add(AlbumRow.Track(song, indexById[song.id] ?: 0))
            }
        }
    }
}

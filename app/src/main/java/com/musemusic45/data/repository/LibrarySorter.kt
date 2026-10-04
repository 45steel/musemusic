package com.musemusic45.data.repository

import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Artist
import com.musemusic45.data.model.Song
import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortOrder
import com.musemusic45.data.model.SortSpec
import com.musemusic45.data.model.compareByName

/**
 * 三个分类页的排序。
 *
 * 统一规则：**缺数据的永远排在最后**，而且和升降序无关 ——
 * 需求里写明了"无年份的排在最后"，不能因为切了降序就跑到最前面。
 *
 * 全是纯函数，便于单元测试。
 */
object LibrarySorter {

    fun songs(songs: List<Song>, spec: SortSpec): List<Song> {
        val missing: (Song) -> Boolean = when (spec.field) {
            SortField.NAME -> { it -> it.title.isBlank() }
            SortField.DATE_ADDED -> { it -> it.dateAddedSec <= 0L }
            SortField.YEAR -> { it -> !it.hasYear }
        }
        val comparator = when (spec.field) {
            SortField.NAME -> Comparator<Song> { a, b -> compareByName(a.title, b.title) }
            SortField.DATE_ADDED -> compareBy { it.dateAddedSec }
            SortField.YEAR -> compareBy { it.year }
        }
        return arrange(songs, missing, comparator, spec.order)
    }

    fun albums(albums: List<Album>, spec: SortSpec): List<Album> {
        val missing: (Album) -> Boolean = when (spec.field) {
            SortField.NAME -> { it -> it.name.isBlank() }
            SortField.DATE_ADDED -> { it -> it.dateAddedSec <= 0L }
            SortField.YEAR -> { it -> it.year <= 0 }
        }
        val comparator = when (spec.field) {
            SortField.NAME -> Comparator<Album> { a, b -> compareByName(a.name, b.name) }
            SortField.DATE_ADDED -> compareBy { it.dateAddedSec }
            SortField.YEAR -> compareBy { it.year }
        }
        return arrange(albums, missing, comparator, spec.order)
    }

    fun artists(artists: List<Artist>, spec: SortSpec): List<Artist> {
        val missing: (Artist) -> Boolean = when (spec.field) {
            SortField.NAME -> { it -> it.name.isBlank() }
            SortField.DATE_ADDED -> { it -> it.dateAddedSec <= 0L }
            // 歌手没有年份字段，理论上不会走到这里
            SortField.YEAR -> { _: Artist -> false }
        }
        val comparator = when (spec.field) {
            SortField.NAME -> Comparator<Artist> { a, b -> compareByName(a.name, b.name) }
            SortField.DATE_ADDED -> compareBy { it.dateAddedSec }
            SortField.YEAR -> compareBy { it.name }
        }
        return arrange(artists, missing, comparator, spec.order)
    }

    /**
     * 把「有值的」按给定方向排好，再把「缺值的」原样接到最后。
     */
    private fun <T> arrange(
        items: List<T>,
        isMissing: (T) -> Boolean,
        comparator: Comparator<T>,
        order: SortOrder,
    ): List<T> {
        if (items.size < 2) return items
        val present = items.filterNot(isMissing)
        val missing = items.filter(isMissing)
        val sorted = present.sortedWith(comparator)
        val ordered = if (order == SortOrder.DESCENDING) sorted.asReversed() else sorted
        return ordered + missing
    }
}

package com.musemusic45.data.repository

import com.musemusic45.data.model.Song

/**
 * 用户手动移除（隐藏）的歌曲。
 *
 * 只影响 App 里显示什么，**不动用户的任何文件** —— 这是整个应用的底线。
 * 隐藏的歌曲不进列表、不进专辑/歌手统计、不进搜索，也不会进播放队列。
 */
object HiddenSongs {

    /** 过滤掉被隐藏的歌曲。 */
    fun visible(songs: List<Song>, hiddenIds: Set<Long>): List<Song> =
        if (hiddenIds.isEmpty()) songs else songs.filterNot { it.id in hiddenIds }

    /** 当前库里被隐藏的歌曲（隐藏后又不在库里的不算）。 */
    fun hiddenInLibrary(songs: List<Song>, hiddenIds: Set<Long>): List<Song> =
        if (hiddenIds.isEmpty()) emptyList() else songs.filter { it.id in hiddenIds }
}

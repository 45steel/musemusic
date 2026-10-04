package com.musemusic45.data.repository

import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Artist
import com.musemusic45.data.model.Song

/**
 * 音乐库的对外状态。
 */
data class LibraryState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isScanning: Boolean = false,
    /** 是否已经完整扫描过一次（用来区分"还在扫"和"确实一首歌都没有"） */
    val hasScanned: Boolean = false,
    val lastScanMillis: Long = 0,
) {
    val isEmpty: Boolean get() = hasScanned && songs.isEmpty()
}

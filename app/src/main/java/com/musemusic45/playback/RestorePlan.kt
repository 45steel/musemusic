package com.musemusic45.playback

import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song

/**
 * 「上次播到哪儿」的恢复方案：把哪一批歌装进队列、停在第几首、哪个位置。
 *
 * 抽成不依赖播放器的不纯函数以外的东西，是为了能单测 ——
 * 恢复逻辑最怕的边界是「歌没了（被移除或换了手机）」「位置超出时长」。
 */
data class RestorePlan(
    val songs: List<Song>,
    val index: Int,
    val positionMs: Long,
) {
    companion object {
        /**
         * 算出恢复方案。
         *
         * @param mode 退出时的播放方式。按专辑播放要把**那张专辑**装回队列，
         *   否则切回专辑模式时会拿到整个库。
         * @param songId 退出时正在播的歌
         * @param positionMs 退出时的播放位置
         * @param allSongs 完整音乐库
         * @param albumTracks 取某张专辑的曲目（按碟号、音轨号排好序）
         * @return 这首歌已经不在库里（被扫描移除、或用户移除了）时返回 null
         */
        fun plan(
            mode: PlayMode,
            songId: Long,
            positionMs: Long,
            allSongs: List<Song>,
            albumTracks: (Long) -> List<Song>,
        ): RestorePlan? {
            if (allSongs.isEmpty()) return null
            val song = allSongs.firstOrNull { it.id == songId } ?: return null

            val songs = if (mode == PlayMode.ALBUM_SHUFFLE) {
                albumTracks(song.albumId).ifEmpty { allSongs }
            } else {
                allSongs
            }

            val index = songs.indexOfFirst { it.id == songId }
            if (index < 0) return null

            // 位置不能超过时长：换了音频文件、或者记录在销毁前写早了都可能超
            val duration = song.durationMs
            val safePosition = if (duration > 0L) {
                positionMs.coerceIn(0L, duration)
            } else {
                positionMs.coerceAtLeast(0L)
            }

            return RestorePlan(songs = songs, index = index, positionMs = safePosition)
        }
    }
}

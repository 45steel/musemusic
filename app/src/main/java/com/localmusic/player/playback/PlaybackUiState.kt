package com.localmusic.player.playback

import com.localmusic.player.data.model.PlayMode
import com.localmusic.player.data.model.Song

/**
 * 播放状态，供界面观察。
 */
data class PlaybackUiState(
    val isConnected: Boolean = false,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val mode: PlayMode = PlayMode.LIST_LOOP,
    /** 按专辑播放时：当前是本轮第几张专辑。其他模式为 0。 */
    val albumRoundIndex: Int = 0,
    /** 按专辑播放时：本轮总共有多少张专辑。其他模式为 0。 */
    val albumRoundTotal: Int = 0,
) {
    val hasCurrent: Boolean get() = currentSong != null

    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

package com.musemusic45.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musemusic45.data.media.LyricsReader
import com.musemusic45.data.media.LyricsState
import com.musemusic45.data.media.TagReader
import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song
import com.musemusic45.data.prefs.SettingsStore
import com.musemusic45.playback.PlaybackController
import com.musemusic45.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 播放状态持有者。与 [LibraryViewModel] 分开，各自管一件事。
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playback = PlaybackController(application)
    private val lyricsReader = LyricsReader(TagReader())
    private val settings = SettingsStore(application)

    val state: StateFlow<PlaybackUiState> = playback.state

    /** 恢复上次播放只做一次，之后音乐库再变也不重复恢复。 */
    private var restoreAttempted = false

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.None)
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { playback.connect() }
        }
        // 恢复上次的播放方式
        viewModelScope.launch {
            val stored = settings.playMode.first()
            if (stored != null) playback.setMode(stored)
        }
        // 当前歌变化时加载它的歌词
        viewModelScope.launch {
            playback.state
                .map { it.currentSong?.id }
                .distinctUntilChanged()
                .collect {
                    val song = playback.state.value.currentSong
                    _lyrics.value = lyricsReader.load(song)
                }
        }
        // 记住播放进度，退出后能接着播。
        // 位置每 500ms 就跳一次，不能每次都写 DataStore（会写爆），
        // 所以换了歌立刻写、同一首歌最多每 5 秒写一次。
        viewModelScope.launch {
            var lastSongId = -1L
            var lastSavedAt = 0L
            playback.state.collect { snapshot ->
                val songId = snapshot.currentSong?.id ?: return@collect
                val now = SystemClock.elapsedRealtime()
                if (songId != lastSongId || now - lastSavedAt >= SAVE_INTERVAL_MS) {
                    lastSongId = songId
                    lastSavedAt = now
                    settings.saveLastPlayed(songId, snapshot.positionMs)
                }
            }
        }
    }

    /** 扫描完成后把音乐库交给播放引擎，用于「按专辑播放」，并尝试恢复上次的播放。 */
    fun configureLibrary(songs: List<Song>) {
        playback.configureLibrary(songs)
        if (restoreAttempted) return
        restoreAttempted = true
        viewModelScope.launch {
            val last = settings.lastPlayed.first() ?: return@launch
            playback.restoreLastPlayed(last.songId, last.positionMs)
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int) = playback.playSongs(songs, startIndex)

    /** 切换播放方式，同时记住选择。 */
    fun setMode(mode: PlayMode) {
        playback.setMode(mode)
        viewModelScope.launch { settings.savePlayMode(mode) }
    }

    fun togglePlayPause() = playback.togglePlayPause()

    fun next() = playback.next()

    fun previous() = playback.previous()

    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    fun skipToQueueIndex(index: Int) = playback.skipToQueueIndex(index)

    fun clearQueue() = playback.clearQueue()

    override fun onCleared() {
        // 退出前再记一次，尽量精确
        playback.currentSnapshot()?.let { (songId, position) ->
            runCatching {
                kotlinx.coroutines.runBlocking { settings.saveLastPlayed(songId, position) }
            }
        }
        playback.release()
        super.onCleared()
    }

    private companion object {
        /** 同一首歌最多多久写一次播放位置。 */
        const val SAVE_INTERVAL_MS = 5_000L
    }
}

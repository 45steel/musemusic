package com.localmusic.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.media.LyricsReader
import com.localmusic.player.data.media.LyricsState
import com.localmusic.player.data.media.TagReader
import com.localmusic.player.data.model.PlayMode
import com.localmusic.player.data.model.Song
import com.localmusic.player.data.prefs.SettingsStore
import com.localmusic.player.playback.PlaybackController
import com.localmusic.player.playback.PlaybackUiState
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
    }

    /** 扫描完成后把音乐库交给播放引擎，用于「按专辑播放」。 */
    fun configureLibrary(songs: List<Song>) = playback.configureLibrary(songs)

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
        playback.release()
        super.onCleared()
    }
}

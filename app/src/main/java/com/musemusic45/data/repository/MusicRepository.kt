package com.musemusic45.data.repository

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.musemusic45.data.media.MediaStoreScanner
import com.musemusic45.data.media.TagReader
import com.musemusic45.data.model.ArtistParsingConfig
import com.musemusic45.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * 音乐库仓库：负责扫描、聚合、以及补齐 MediaStore 缺失的字段。
 */
class MusicRepository(private val context: Context) {

    private val scanner = MediaStoreScanner(context)
    private val tagReader = TagReader()

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /**
     * 重新扫描整个音乐库。
     *
     * 分两步：
     *  1. 先从 MediaStore 拿到全部歌曲并**立即发布**，保证首屏马上有内容
     *  2. 再在后台补齐年份（MediaStore 对 FLAC 不提供年份）
     */
    suspend fun refresh(artistConfig: ArtistParsingConfig = ArtistParsingConfig()) {
        _state.update { it.copy(isScanning = true) }

        val started = SystemClock.elapsedRealtime()
        val songs = runCatching { scanner.scan() }
            .onFailure { Log.e(TAG, "扫描音乐库失败", it) }
            .getOrDefault(emptyList())
        val elapsed = SystemClock.elapsedRealtime() - started

        apply(songs, elapsed, artistConfig)

        Log.i(
            TAG,
            "扫描完成: 歌曲=${songs.size} 专辑=${_state.value.albums.size} " +
                "歌手=${_state.value.artists.size} 生效分隔符='${artistConfig.separators}' " +
                "用时=${elapsed}ms",
        )

        enrichYears(songs, artistConfig)
    }

    /**
     * 只按新的歌手解析配置**重新聚合**，不重新扫描 MediaStore。
     *
     * 用户在设置里改了开关或分隔符时走这条路 —— 整库重新聚合只要几十毫秒，
     * 重新扫描媒体库则要一百多毫秒且毫无必要（歌曲没变）。
     */
    fun reaggregate(artistConfig: ArtistParsingConfig) {
        Log.i(TAG, "按新配置重新聚合, 生效分隔符='${artistConfig.separators}'")
        apply(_state.value.songs, _state.value.lastScanMillis, artistConfig)
    }

    private fun apply(songs: List<Song>, elapsedMillis: Long, artistConfig: ArtistParsingConfig) {
        _state.update {
            it.copy(
                songs = songs,
                albums = LibraryAggregator.albums(songs, artistConfig),
                artists = LibraryAggregator.artists(songs, artistConfig),
                isScanning = false,
                hasScanned = true,
                lastScanMillis = elapsedMillis,
            )
        }
    }

    /**
     * 补齐年份。
     *
     * MediaStore 的 `year` 列对 FLAC 一律为 NULL（实测结论），所以对 `year == 0`
     * 的歌逐个读文件标签。这一步放在首屏发布之后，不阻塞列表显示。
     */
    private suspend fun enrichYears(songs: List<Song>, artistConfig: ArtistParsingConfig) {
        val pending = songs.filter { it.year == 0 && it.path.isNotBlank() }
        if (pending.isEmpty()) return

        val started = SystemClock.elapsedRealtime()
        val years = withContext(Dispatchers.IO) {
            val found = HashMap<Long, Int>(pending.size)
            for (song in pending) {
                val year = tagReader.readYear(song.path)
                if (year > 0) found[song.id] = year
            }
            found
        }
        if (years.isEmpty()) return

        val enriched = _state.value.songs.map { song ->
            years[song.id]?.let { song.copy(year = it) } ?: song
        }
        apply(enriched, _state.value.lastScanMillis, artistConfig)

        Log.i(
            TAG,
            "补齐年份: ${years.size}/${pending.size} 首, 用时=${SystemClock.elapsedRealtime() - started}ms",
        )
    }

    companion object {
        private const val TAG = MediaStoreScanner.TAG
    }
}

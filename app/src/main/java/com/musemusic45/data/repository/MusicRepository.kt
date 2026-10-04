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
 *
 * 内部保留**扫描到的完整歌曲列表**（[rawSongs]），对外发布的 [LibraryState.songs]
 * 是滤掉用户手动移除之后的可见列表。分开存的原因：取消隐藏时要能拿回原列表，
 * 只留过滤后的列表就再也找不回来了。
 */
class MusicRepository(private val context: Context) {

    private val scanner = MediaStoreScanner(context)
    private val tagReader = TagReader()

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /** 当前库里被用户手动移除的歌曲数（用于设置页显示）。 */
    private val _hiddenCount = MutableStateFlow(0)
    val hiddenCount: StateFlow<Int> = _hiddenCount.asStateFlow()

    /** 扫描到的完整列表（未过滤）。 */
    private var rawSongs: List<Song> = emptyList()

    /** 用户手动移除的歌曲。 */
    private var hiddenIds: Set<Long> = emptySet()

    private var artistConfig: ArtistParsingConfig = ArtistParsingConfig()
    private var lastScanMillis: Long = 0L

    /**
     * 重新扫描整个音乐库。
     *
     * 分两步：
     *  1. 先从 MediaStore 拿到全部歌曲并**立即发布**，保证首屏马上有内容
     *  2. 再在后台补齐年份（MediaStore 对 FLAC 不提供年份）
     */
    suspend fun refresh(config: ArtistParsingConfig = ArtistParsingConfig()) {
        artistConfig = config
        _state.update { it.copy(isScanning = true) }

        val started = SystemClock.elapsedRealtime()
        val songs = runCatching { scanner.scan() }
            .onFailure { Log.e(TAG, "扫描音乐库失败", it) }
            .getOrDefault(emptyList())
        lastScanMillis = SystemClock.elapsedRealtime() - started

        rawSongs = songs
        publish()

        Log.i(
            TAG,
            "扫描完成: 歌曲=${songs.size} 可见=${_state.value.songs.size} " +
                "专辑=${_state.value.albums.size} 歌手=${_state.value.artists.size} " +
                "生效分隔符='${artistConfig.separators}' 用时=${lastScanMillis}ms",
        )

        enrichYears(songs)
    }

    /**
     * 只按新的歌手解析配置**重新聚合**，不重新扫描 MediaStore。
     *
     * 用户在设置里改了开关或分隔符时走这条路 —— 整库重新聚合只要几十毫秒，
     * 重新扫描媒体库则要一百多毫秒且毫无必要（歌曲没变）。
     */
    fun reaggregate(config: ArtistParsingConfig) {
        Log.i(TAG, "按新配置重新聚合, 生效分隔符='${config.separators}'")
        artistConfig = config
        publish()
    }

    /**
     * 更新「手动移除」的集合。集合真的变了才重新聚合。
     *
     * @return 是否发生了变化
     */
    fun setHiddenSongs(ids: Set<Long>): Boolean {
        if (ids == hiddenIds) return false
        hiddenIds = ids
        Log.i(TAG, "手动移除的歌曲: ${ids.size} 首")
        publish()
        return true
    }

    /** 扫描到的完整列表（含被手动移除的），供设置页统计用。 */
    fun allScannedSongs(): List<Song> = rawSongs

    /** 把内部状态重新算一遍并发布。 */
    private fun publish() {
        val visible = HiddenSongs.visible(rawSongs, hiddenIds)
        _hiddenCount.value = HiddenSongs.hiddenInLibrary(rawSongs, hiddenIds).size
        _state.update {
            it.copy(
                songs = visible,
                albums = LibraryAggregator.albums(visible, artistConfig),
                artists = LibraryAggregator.artists(visible, artistConfig),
                isScanning = false,
                hasScanned = true,
                lastScanMillis = lastScanMillis,
            )
        }
    }

    /**
     * 补齐年份。
     *
     * MediaStore 的 `year` 列对 FLAC 一律为 NULL（实测结论），所以对 `year == 0`
     * 的歌逐个读文件标签。这一步放在首屏发布之后，不阻塞列表显示。
     */
    private suspend fun enrichYears(songs: List<Song>) {
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

        rawSongs = rawSongs.map { song ->
            years[song.id]?.let { song.copy(year = it) } ?: song
        }
        publish()

        Log.i(
            TAG,
            "补齐年份: ${years.size}/${pending.size} 首, 用时=${SystemClock.elapsedRealtime() - started}ms",
        )
    }

    companion object {
        private const val TAG = MediaStoreScanner.TAG
    }
}

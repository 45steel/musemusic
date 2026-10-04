package com.musemusic45.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musemusic45.data.model.ArtistParsingConfig
import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortOrder
import com.musemusic45.data.model.SortSpec
import com.musemusic45.data.model.SortTarget
import com.musemusic45.data.model.defaultOrderFor
import com.musemusic45.data.prefs.SettingsStore
import com.musemusic45.data.repository.LibraryState
import com.musemusic45.data.repository.MusicRepository
import com.musemusic45.data.search.PinyinProvider
import com.musemusic45.data.search.SearchIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 音乐库、排序、搜索索引、文件夹筛选的状态持有者。
 * 三个分类页共享同一份扫描结果，各自维护自己的排序。
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val settings = SettingsStore(application)

    val state: StateFlow<LibraryState> = repository.state

    private val _sorts = MutableStateFlow(DEFAULT_SORTS)
    val sorts: StateFlow<Map<SortTarget, SortSpec>> = _sorts.asStateFlow()

    /** 搜索索引。扫描完成后在后台线程构建，未就绪时搜索页显示提示。 */
    private val _searchIndex = MutableStateFlow<SearchIndex?>(null)
    val searchIndex: StateFlow<SearchIndex?> = _searchIndex.asStateFlow()

    /** 用户手动添加的文件夹（系统绝对路径）。 */
    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    /** 是否只显示已添加文件夹里的音乐。 */
    private val _onlyFolders = MutableStateFlow(false)
    val onlyFolders: StateFlow<Boolean> = _onlyFolders.asStateFlow()

    /**
     * 歌手解析配置（拆多歌手 / 忽略括号 / 分隔符）。
     *
     * 它参与聚合，所以改动后要重新归类 —— 见下面的 collect。
     */
    private val _artistConfig = MutableStateFlow(ArtistParsingConfig())
    val artistConfig: StateFlow<ArtistParsingConfig> = _artistConfig.asStateFlow()

    /** 从最近任务列表划掉 App 后是否停止播放（默认 false = 继续播）。 */
    private val _stopOnTaskRemoved = MutableStateFlow(false)
    val stopOnTaskRemoved: StateFlow<Boolean> = _stopOnTaskRemoved.asStateFlow()

    /** 是否启用预测式返回（默认 true）。 */
    private val _predictiveBack = MutableStateFlow(true)
    val predictiveBack: StateFlow<Boolean> = _predictiveBack.asStateFlow()

    /** 当前库里被手动移除的歌曲数。 */
    val hiddenCount: StateFlow<Int> = repository.hiddenCount

    init {
        // 恢复上次的排序偏好
        viewModelScope.launch {
            val stored = settings.sorts.first()
            if (stored.isNotEmpty()) {
                _sorts.update { DEFAULT_SORTS + stored }
            }
        }
        viewModelScope.launch { settings.folders.collect { _folders.value = it } }
        viewModelScope.launch { settings.onlyFolders.collect { _onlyFolders.value = it } }
        viewModelScope.launch {
            settings.stopOnTaskRemoved.collect { _stopOnTaskRemoved.value = it }
        }
        viewModelScope.launch {
            settings.predictiveBack.collect { _predictiveBack.value = it }
        }

        // 手动移除的集合一变，立刻重新聚合（不重新扫描媒体库）并重建搜索索引。
        // 首次拿到的值也要走一遍：库里可能有上次会话移除过的歌。
        viewModelScope.launch {
            settings.hiddenSongIds.collect { ids ->
                if (repository.setHiddenSongs(ids)) {
                    rebuildSearchIndex()
                }
            }
        }

        // 歌手配置一改，立刻按新规则重新归类（不重新扫描媒体库），并重建搜索索引
        viewModelScope.launch {
            combine(
                settings.splitArtists,
                settings.ignoreArtistParens,
                settings.extraArtistSeparators,
            ) { split, ignoreParens, extraSeparators ->
                ArtistParsingConfig(split, ignoreParens, extraSeparators)
            }.collect { config ->
                if (config != _artistConfig.value) {
                    Log.i(TAG, "歌手配置变化: $config")
                    _artistConfig.value = config
                    repository.reaggregate(config)
                    rebuildSearchIndex()
                }
            }
        }
    }

    /**
     * 首次进入或用户手动触发时调用。
     *
     * **必须自己先把配置读出来**：`viewModelScope` 是 Main.immediate，
     * 这个 launch 体会在调用点同步跑起来，而 init 里那个 collect 还没拿到
     * DataStore 的值。不自己读的话，首次扫描会用默认配置发布结果，
     * 之后的 reaggregate 又发生在歌曲还为空的时候 —— 两边都白做。
     */
    fun refresh() {
        viewModelScope.launch {
            val config = loadArtistConfig()
            _artistConfig.value = config
            repository.refresh(config)
            rebuildSearchIndex()
        }
    }

    private suspend fun loadArtistConfig(): ArtistParsingConfig = ArtistParsingConfig(
        splitMultiArtist = settings.splitArtists.first(),
        ignoreParentheses = settings.ignoreArtistParens.first(),
        extraSeparators = settings.extraArtistSeparators.first(),
    )

    private suspend fun rebuildSearchIndex() {
        _searchIndex.value = null
        val snapshot = state.value
        if (snapshot.songs.isEmpty()) return

        val started = android.os.SystemClock.elapsedRealtime()
        val provider = PinyinProvider()
        val index = withContext(Dispatchers.Default) {
            SearchIndex.build(snapshot.songs, snapshot.albums, snapshot.artists, provider)
        }
        _searchIndex.value = index
        Log.i(
            TAG,
            "搜索索引就绪: 歌曲=${index.songs.size} 专辑=${index.albums.size} " +
                "歌手=${index.artists.size} 拼音可用=${provider.available} " +
                "用时=${android.os.SystemClock.elapsedRealtime() - started}ms",
        )
    }

    /**
     * 选中某个排序字段。已经选中的字段保持不变（切换方向请用 [toggleSortOrder]）。
     */
    fun selectSortField(target: SortTarget, field: SortField) {
        _sorts.update { current ->
            val old = current[target] ?: SortSpec.DEFAULT
            if (old.field == field) current else current + (target to SortSpec(field, defaultOrderFor(field)))
        }
        persistSorts()
    }

    /** 切换升序 / 降序。 */
    fun toggleSortOrder(target: SortTarget) {
        _sorts.update { current ->
            val old = current[target] ?: SortSpec.DEFAULT
            current + (target to old.toggled())
        }
        persistSorts()
    }

    private fun persistSorts() {
        val snapshot = _sorts.value
        viewModelScope.launch { settings.saveSorts(snapshot) }
    }

    // ------------------------------------------------------------ 文件夹

    fun addFolder(path: String) {
        viewModelScope.launch { settings.addFolder(path) }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch { settings.removeFolder(path) }
    }

    fun setOnlyFolders(enabled: Boolean) {
        viewModelScope.launch { settings.setOnlyFolders(enabled) }
    }

    // ------------------------------------------------------------ 歌手归类

    fun setSplitArtists(enabled: Boolean) {
        viewModelScope.launch { settings.setSplitArtists(enabled) }
    }

    fun setIgnoreArtistParens(enabled: Boolean) {
        viewModelScope.launch { settings.setIgnoreArtistParens(enabled) }
    }

    fun setArtistSeparators(separators: String) {
        viewModelScope.launch { settings.addArtistSeparators(separators) }
    }

    fun removeArtistSeparator(separator: Char) {
        viewModelScope.launch { settings.removeArtistSeparator(separator) }
    }

    // -------------------------------------------------------- 手动移除歌曲

    /** 把一首歌从 App 里移除（**只影响显示与播放队列，不删文件**）。 */
    fun hideSong(songId: Long) {
        viewModelScope.launch { settings.hideSong(songId) }
    }

    /** 恢复全部被移除的歌曲。 */
    fun unhideAllSongs() {
        viewModelScope.launch { settings.unhideAllSongs() }
    }

    // ------------------------------------------------------------ 后台行为

    fun setStopOnTaskRemoved(enabled: Boolean) {
        viewModelScope.launch { settings.setStopOnTaskRemoved(enabled) }
    }

    fun setPredictiveBack(enabled: Boolean) {
        viewModelScope.launch { settings.setPredictiveBack(enabled) }
    }

    companion object {
        private const val TAG = "MuseMusic"

        /** 三个页面的初始排序，与设计稿一致。 */
        val DEFAULT_SORTS: Map<SortTarget, SortSpec> = mapOf(
            SortTarget.SONGS to SortSpec(SortField.NAME, SortOrder.ASCENDING),
            SortTarget.ALBUMS to SortSpec(SortField.DATE_ADDED, SortOrder.DESCENDING),
            SortTarget.ARTISTS to SortSpec(SortField.NAME, SortOrder.ASCENDING),
        )
    }
}

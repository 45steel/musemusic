package com.musemusic45.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    }

    /** 首次进入或用户手动触发时调用。 */
    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
            rebuildSearchIndex()
        }
    }

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

package com.musemusic45.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musemusic45.data.model.ArtistParsingConfig
import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortOrder
import com.musemusic45.data.model.SortSpec
import com.musemusic45.data.model.SortTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_music_settings",
)

/** 上次播放位置的记录。 */
data class LastPlayed(val songId: Long, val positionMs: Long)

/**
 * 应用自己的偏好设置。
 *
 * 存在应用私有目录里，**不动用户的任何音乐文件**。
 * 卸载 App 会丢，但重建代价很小。
 */
class SettingsStore(private val context: Context) {

    // ------------------------------------------------------------ 排序

    val sorts: Flow<Map<SortTarget, SortSpec>> = context.settingsDataStore.data.map { prefs ->
        buildMap {
            SortTarget.entries.forEach { target ->
                val field = prefs[sortFieldKey(target)]?.let { name ->
                    runCatching { SortField.valueOf(name) }.getOrNull()
                }
                val order = prefs[sortOrderKey(target)]?.let { name ->
                    runCatching { SortOrder.valueOf(name) }.getOrNull()
                }
                if (field != null && order != null) {
                    put(target, SortSpec(field, order))
                }
            }
        }
    }

    suspend fun saveSorts(sorts: Map<SortTarget, SortSpec>) {
        context.settingsDataStore.edit { prefs ->
            sorts.forEach { (target, spec) ->
                prefs[sortFieldKey(target)] = spec.field.name
                prefs[sortOrderKey(target)] = spec.order.name
            }
        }
    }

    // -------------------------------------------------------- 播放方式

    val playMode: Flow<PlayMode?> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_PLAY_MODE]?.let { name -> runCatching { PlayMode.valueOf(name) }.getOrNull() }
    }

    suspend fun savePlayMode(mode: PlayMode) {
        context.settingsDataStore.edit { it[KEY_PLAY_MODE] = mode.name }
    }

    // ---------------------------------------------------------- 上次播放

    val lastPlayed: Flow<LastPlayed?> = context.settingsDataStore.data.map { prefs ->
        val id = prefs[KEY_LAST_SONG_ID]
        val position = prefs[KEY_LAST_POSITION] ?: 0L
        if (id == null || id <= 0L) null else LastPlayed(id, position)
    }

    suspend fun saveLastPlayed(songId: Long, positionMs: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_LAST_SONG_ID] = songId
            prefs[KEY_LAST_POSITION] = positionMs
        }
    }

    // ------------------------------------------------------------ 文件夹

    /** 已添加的文件夹（系统绝对路径）。 */
    val folders: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_FOLDERS] ?: emptySet()).sorted()
    }

    suspend fun addFolder(path: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_FOLDERS] = (prefs[KEY_FOLDERS] ?: emptySet()) + path
        }
    }

    suspend fun removeFolder(path: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_FOLDERS] = (prefs[KEY_FOLDERS] ?: emptySet()) - path
        }
    }

    /** 是否只显示已添加文件夹里的音乐。 */
    val onlyFolders: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ONLY_FOLDERS] ?: false
    }

    suspend fun setOnlyFolders(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ONLY_FOLDERS] = enabled }
    }

    // ---------------------------------------------------------- 歌手归类

    /** 是否把「周杰伦、费玉清」拆成两位歌手。 */
    val splitArtists: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SPLIT_ARTISTS] ?: true
    }

    suspend fun setSplitArtists(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SPLIT_ARTISTS] = enabled }
    }

    /** 是否忽略歌手名里的括号内容（「某某（xxx）」→「某某」）。 */
    val ignoreArtistParens: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_IGNORE_PARENS] ?: true
    }

    suspend fun setIgnoreArtistParens(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_IGNORE_PARENS] = enabled }
    }

    /**
     * **额外添加**的歌手分隔符（默认的 、；/ 始终生效，这里是追加）。
     * 每个字符都算一个。
     */
    val extraArtistSeparators: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_EXTRA_SEPARATORS] ?: ""
    }

    /** 追加分隔符。默认已覆盖的字符会被忽略，不会重复堆在列表里。 */
    suspend fun addArtistSeparators(text: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_EXTRA_SEPARATORS] ?: ""
            prefs[KEY_EXTRA_SEPARATORS] = (current + text)
                .filterNot { it in ArtistParsingConfig.DEFAULT_SEPARATORS }
                .toList()
                .distinct()
                .joinToString("")
        }
    }

    /** 移除一个额外分隔符。 */
    suspend fun removeArtistSeparator(separator: Char) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_EXTRA_SEPARATORS] ?: ""
            prefs[KEY_EXTRA_SEPARATORS] = current.filterNot { it == separator }
        }
    }

    // ---------------------------------------------------------- 后台行为

    /**
     * 从最近任务列表划掉 App 后是否停止播放。
     *
     * **默认 false（继续播放）** —— 用户划掉界面通常只是想关掉窗口，
     * 音乐不该跟着断。想让它停的人可以打开这个开关。
     */
    val stopOnTaskRemoved: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_STOP_ON_TASK_REMOVED] ?: false
    }

    suspend fun setStopOnTaskRemoved(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_STOP_ON_TASK_REMOVED] = enabled }
    }

    // ------------------------------------------------------ 手动移除的歌曲

    /** 用户手动移除（隐藏）的歌曲 ID。只影响显示与播放队列，不动任何文件。 */
    val hiddenSongIds: Flow<Set<Long>> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_HIDDEN_SONGS] ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    suspend fun hideSong(songId: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_HIDDEN_SONGS] = (prefs[KEY_HIDDEN_SONGS] ?: emptySet()) + songId.toString()
        }
    }

    suspend fun unhideAllSongs() {
        context.settingsDataStore.edit { it.remove(KEY_HIDDEN_SONGS) }
    }

    // -------------------------------------------------------------- 键

    private companion object {
        val KEY_PLAY_MODE = stringPreferencesKey("play_mode")
        val KEY_LAST_SONG_ID = longPreferencesKey("last_song_id")
        val KEY_LAST_POSITION = longPreferencesKey("last_position_ms")
        val KEY_FOLDERS = stringSetPreferencesKey("added_folders")
        val KEY_ONLY_FOLDERS = booleanPreferencesKey("only_added_folders")

        val KEY_SPLIT_ARTISTS = booleanPreferencesKey("split_artists")
        val KEY_IGNORE_PARENS = booleanPreferencesKey("ignore_artist_parens")
        val KEY_EXTRA_SEPARATORS = stringPreferencesKey("artist_extra_separators")

        val KEY_STOP_ON_TASK_REMOVED = booleanPreferencesKey("stop_on_task_removed")
        val KEY_HIDDEN_SONGS = stringSetPreferencesKey("hidden_song_ids")

        fun sortFieldKey(target: SortTarget) = stringPreferencesKey("sort_field_${target.name}")

        fun sortOrderKey(target: SortTarget) = stringPreferencesKey("sort_order_${target.name}")
    }
}

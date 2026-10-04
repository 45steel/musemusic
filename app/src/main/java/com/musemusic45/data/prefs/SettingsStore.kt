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

    // -------------------------------------------------------------- 键

    private companion object {
        val KEY_PLAY_MODE = stringPreferencesKey("play_mode")
        val KEY_LAST_SONG_ID = longPreferencesKey("last_song_id")
        val KEY_LAST_POSITION = longPreferencesKey("last_position_ms")
        val KEY_FOLDERS = stringSetPreferencesKey("added_folders")
        val KEY_ONLY_FOLDERS = booleanPreferencesKey("only_added_folders")

        fun sortFieldKey(target: SortTarget) = stringPreferencesKey("sort_field_${target.name}")

        fun sortOrderKey(target: SortTarget) = stringPreferencesKey("sort_order_${target.name}")
    }
}

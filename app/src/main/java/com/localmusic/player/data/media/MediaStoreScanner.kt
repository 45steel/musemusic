package com.localmusic.player.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.localmusic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 读取系统媒体库里的全部音乐。
 *
 * 关于 `disc_number`：它不在 [MediaStore.Audio.Media] 的公开常量里，
 * 但 MediaProvider 确实提供了这一列（实测 Android 15 上可用），
 * 所以这里用字面量列名，并且在查询失败时自动降级到不含碟号的查询。
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val songs = runCatching { query(withDiscNumber = true) }
            .getOrElse { error ->
                android.util.Log.w(TAG, "含碟号的查询失败，降级重试: ${error.message}")
                query(withDiscNumber = false)
            }
        songs
    }

    private fun query(withDiscNumber: Boolean): List<Song> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(COL_ALBUM_ARTIST)
            if (withDiscNumber) add(COL_DISC_NUMBER)
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = ArrayList<Song>()
        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val discIdx = cursor.getColumnIndex(COL_DISC_NUMBER)
                val albumArtistIdx = cursor.getColumnIndex(COL_ALBUM_ARTIST)

                while (cursor.moveToNext()) {
                    val rawDisc = if (discIdx >= 0) cursor.getIntOrZero(discIdx) else 0
                    val disc = normalizeDiscNumber(rawDisc)
                    val track = normalizeTrackNumber(cursor.getIntOrZero(trackIdx), rawDisc)

                    result += Song(
                        id = cursor.getLong(idIdx),
                        title = cursor.getStringOrNull(titleIdx)?.ifBlank { null } ?: "(未知曲目)",
                        artist = normalizeArtist(cursor.getStringOrNull(artistIdx)),
                        album = normalizeAlbum(cursor.getStringOrNull(albumIdx)),
                        albumId = cursor.getLong(albumIdIdx),
                        discNumber = disc,
                        trackNumber = track,
                        year = cursor.getIntOrZero(yearIdx),
                        durationMs = cursor.getLong(durationIdx),
                        dateAddedSec = cursor.getLong(dateAddedIdx),
                        path = cursor.getStringOrNull(dataIdx).orEmpty(),
                        mimeType = cursor.getStringOrNull(mimeIdx).orEmpty(),
                        sizeBytes = cursor.getLong(sizeIdx),
                        albumArtist = if (albumArtistIdx >= 0) {
                            cursor.getStringOrNull(albumArtistIdx)
                                ?.takeIf { it.isNotBlank() && it != UNKNOWN_TAG }
                                .orEmpty()
                        } else {
                            ""
                        },
                    )
                }
            }
        return result
    }

    companion object {
        const val TAG = "LocalMusic"

        /** MediaProvider 的碟号列，不在公开常量里。 */
        const val COL_DISC_NUMBER = "disc_number"

        /** MediaProvider 的专辑歌手列。 */
        const val COL_ALBUM_ARTIST = "album_artist"

        /** MediaStore 对未知歌手的占位值。 */
        private const val UNKNOWN_TAG = "<unknown>"

        fun normalizeArtist(raw: String?): String =
            if (raw.isNullOrBlank() || raw == UNKNOWN_TAG) Song.UNKNOWN_ARTIST else raw

        fun normalizeAlbum(raw: String?): String =
            if (raw.isNullOrBlank() || raw == UNKNOWN_TAG) Song.UNKNOWN_ALBUM else raw

        /** 专辑封面的 content URI。 */
        fun albumArtUri(albumId: Long) =
            ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)

        private val ALBUM_ART_BASE =
            android.net.Uri.parse("content://media/external/audio/albumart")
    }
}

private fun android.database.Cursor.getIntOrZero(index: Int): Int =
    if (isNull(index)) 0 else getInt(index)

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

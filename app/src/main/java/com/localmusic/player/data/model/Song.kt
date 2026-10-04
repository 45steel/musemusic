package com.localmusic.player.data.model

/**
 * 一首歌。
 *
 * 字段来源是系统媒体库（MediaStore.Audio.Media）；碟号等媒体库不提供的字段
 * 由 TagReader 兜底读取（见 M1 的 Spike 结论）。
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val discNumber: Int,
    val trackNumber: Int,
    val year: Int,
    val durationMs: Long,
    /** 加入媒体库的时间，单位秒（MediaStore 的 DATE_ADDED） */
    val dateAddedSec: Long,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    /**
     * 专辑歌手（MediaStore 的 album_artist）。
     *
     * 合辑里每首歌的 [artist] 各不相同，但 [albumArtist] 相同，
     * 用它才能把合辑正确归为一张专辑。取不到时为空串。
     */
    val albumArtist: String = "",
) {
    val hasYear: Boolean get() = year > 0
    val hasTrack: Boolean get() = trackNumber > 0
    val hasDisc: Boolean get() = discNumber > 0

    companion object {
        const val UNKNOWN_ALBUM = "未知专辑"
        const val UNKNOWN_ARTIST = "未知歌手"
    }
}

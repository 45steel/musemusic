package com.localmusic.player.data.model

/**
 * 单元测试用的 Song 构造器。
 */
internal fun testSong(
    id: Long,
    title: String = "歌$id",
    artist: String = "测试歌手",
    album: String = "测试专辑",
    albumId: Long = 1L,
    disc: Int = 1,
    track: Int = 0,
    year: Int = 0,
    durationMs: Long = 180_000L,
    dateAddedSec: Long = 1_700_000_000L,
    path: String = "/sdcard/Music/test$id.flac",
    albumArtist: String = "",
): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    discNumber = disc,
    trackNumber = track,
    year = year,
    durationMs = durationMs,
    dateAddedSec = dateAddedSec,
    path = path,
    mimeType = "audio/flac",
    sizeBytes = 1_000_000L,
    albumArtist = albumArtist,
)

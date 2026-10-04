package com.musemusic45.playback

import android.content.ContentUris
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.musemusic45.data.media.MediaStoreScanner
import com.musemusic45.data.model.Song

/**
 * Song → MediaItem。
 *
 * 音频用 MediaStore 的 content URI 而不是文件路径，这样在分区存储下也能稳定读取；
 * 封面交给 MediaStore 的 albumart 通道，通知栏和锁屏都能拿到。
 */
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(SongMediaId.of(this))
    .setUri(audioContentUri(id))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(MediaStoreScanner.albumArtUri(albumId))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build(),
    )
    .build()

/** 某首歌在系统媒体库里的音频 content URI。 */
fun audioContentUri(songId: Long) =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

/** 从 MediaItem 的 mediaId 反查歌曲 ID。 */
fun MediaItem.songIdOrNull(): Long? = SongMediaId.parse(mediaId)

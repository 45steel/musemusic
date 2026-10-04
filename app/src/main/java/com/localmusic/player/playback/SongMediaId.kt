package com.localmusic.player.playback

import com.localmusic.player.data.model.Song

/**
 * 播放队列里 MediaItem 与 Song 的对应关系。
 *
 * 用歌曲 ID 作为 MediaItem 的 mediaId，队列重建后仍能把播放项映射回歌曲。
 * 纯函数，便于单元测试。
 */
object SongMediaId {

    fun of(song: Song): String = song.id.toString()

    fun parse(mediaId: String?): Long? = mediaId?.toLongOrNull()

    fun ofId(id: Long): String = id.toString()
}

/**
 * 在队列里找出某个 mediaId 对应的下标，找不到返回 -1。
 */
fun queueIndexOf(mediaIds: List<String>, targetMediaId: String?): Int {
    if (targetMediaId == null) return -1
    return mediaIds.indexOf(targetMediaId)
}

/**
 * 把播放位置夹到合法范围内，避免越界崩溃。
 */
fun clampIndex(index: Int, size: Int): Int = when {
    size <= 0 -> -1
    index < 0 -> 0
    index >= size -> size - 1
    else -> index
}

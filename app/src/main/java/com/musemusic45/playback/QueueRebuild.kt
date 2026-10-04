package com.musemusic45.playback

import com.musemusic45.data.model.Song

/**
 * 从播放器里的播放项把「队列 → Song」的映射重建回来。
 *
 * **为什么需要它**：`PlaybackController.queueSongs` 是控制器自己的内存字段。
 * 后台被清掉后播放服务还活着、音乐还在放，但重新打开界面时是**新的控制器**，
 * 这个映射是空的 —— 于是迷你播放器上一个字都没有，明明歌还在响。
 *
 * 播放项的 mediaId 就是歌曲 ID（见 [SongMediaId]），所以可以反查回 Song。
 *
 * 纯函数，便于单元测试。
 */
object QueueRebuild {

    /**
     * @param mediaIds 播放器里每个播放项的歌曲 ID，解析不出来的位置为 null
     * @param library 歌曲 ID → Song
     * @param fallback 库里找不到这首歌时（被用户移除、文件被删、换了手机）用它造一条占位。
     *   返回 null 表示宁可不重建，也不要给出对不上号的队列。
     * @return 重建出来的队列；长度为 0，或某一项连占位都造不出来时返回 null
     */
    fun rebuild(
        mediaIds: List<Long?>,
        library: Map<Long, Song>,
        fallback: (index: Int, songId: Long?) -> Song?,
    ): List<Song>? {
        if (mediaIds.isEmpty()) return null

        val result = ArrayList<Song>(mediaIds.size)
        mediaIds.forEachIndexed { index, id ->
            val song = id?.let { library[it] } ?: fallback(index, id) ?: return null
            result += song
        }
        return result
    }
}

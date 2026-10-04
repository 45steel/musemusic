package com.musemusic45.playback

import com.musemusic45.data.model.PlayMode

/**
 * 切换播放方式时对队列的处理规则（第二版新增）。
 *
 * 抽成纯函数是为了能单元测试 —— 这里每一条规则背后都对应一个真实缺陷：
 *
 *  - 切到「按专辑播放」时必须把当前专辑装进队列，并且**保留原播放位置**。
 *    第一版把起始位置写成 0，导致切模式时当前歌从头重播。
 *  - 离开「按专辑播放」时必须把队列换回完整音乐库。第一版只改了 repeatMode，
 *    队列还停在那一张专辑上，配合 REPEAT_MODE_ALL 会变成"单张专辑无限循环"。
 */
object ModeSwitch {

    /** 是否需要把「当前歌所在专辑」装进队列。 */
    fun needsAlbumQueue(to: PlayMode): Boolean = to == PlayMode.ALBUM_SHUFFLE

    /** 是否需要在切模式前把当前队列换成完整音乐库。 */
    fun needsFullQueueRestore(from: PlayMode, to: PlayMode): Boolean =
        from == PlayMode.ALBUM_SHUFFLE && to != PlayMode.ALBUM_SHUFFLE

    /**
     * 切换后应当恢复到的播放位置。
     *
     * 负数一律当作 0 —— 有些播放器在未就绪时会返回负值。
     */
    fun resumePosition(positionMs: Long): Long = positionMs.coerceAtLeast(0L)

    /**
     * 切模式时当前歌在完整音乐库里的下标。
     *
     * 找不到（比如这首歌已经不在库里了）时返回 0。
     */
    fun indexInFullQueue(songs: List<com.musemusic45.data.model.Song>, songId: Long): Int =
        songs.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: 0
}

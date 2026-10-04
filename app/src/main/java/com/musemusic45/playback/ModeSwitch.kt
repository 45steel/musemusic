package com.musemusic45.playback

import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song

/**
 * 切换播放方式时对队列的处理规则（第二版新增）。
 *
 * 抽成纯函数是为了能单元测试 —— 这里每一条规则背后都对应一个真实缺陷：
 *
 *  - 切到「按专辑播放」时必须把当前专辑装进队列，并且**保留原播放位置**。
 *    第一版把起始位置写成 0，导致切模式时当前歌从头重播。
 *  - 离开「按专辑播放」时必须把队列换回完整音乐库。第一版只改了 repeatMode，
 *    队列还停在那一张专辑上，配合 REPEAT_MODE_ALL 会变成"单张专辑无限循环"。
 *  - **[applyTiming]：控制器还没连上时也必须把选择记下来。**
 *    第十批加单曲循环时踩到：`connect()` 是挂起的而 `setMode()` 不是，
 *    两个一起 launch 时 setMode 先跑完，那会儿 controller 还是 null，
 *    `controller ?: return` 一返回，**存档的播放方式就被静默丢掉了**。
 *    表现是"设了单曲循环，重启后变回列表循环"。
 */
object ModeSwitch {

    /** 一次「切换播放方式」请求该怎么处理。 */
    enum class Timing {
        /** 播放器已就绪，立即应用。 */
        NOW,

        /** 播放器还没连上：先把选择记下来，连上后再应用。 */
        DEFER,
    }

    /**
     * 判定这次切换是立即应用还是先记下来。
     *
     * @return 为 null 表示模式没变化、什么都不用做
     */
    fun applyTiming(mode: PlayMode, current: PlayMode, playerReady: Boolean): Timing? = when {
        mode == current -> null
        !playerReady -> Timing.DEFER
        else -> Timing.NOW
    }

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
    fun indexInFullQueue(songs: List<Song>, songId: Long): Int =
        songs.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: 0
}

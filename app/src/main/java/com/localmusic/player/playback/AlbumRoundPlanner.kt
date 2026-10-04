package com.localmusic.player.playback

import kotlin.random.Random

/**
 * 「按专辑播放」的轮次算法 —— 整个项目的核心逻辑。
 *
 * 规则（对照 PRD 第五章）：
 *  - 一轮之内同一张专辑不重复
 *  - 当前专辑播完后，从**本轮没播过的专辑**里随机抽下一张
 *  - 一轮全部播完后重新开始新一轮
 *  - 手动点歌时，那首歌所在专辑成为当前专辑；
 *    如果它本轮已经播过，**不消耗**本轮剩余的专辑池
 *
 * 刻意做成不依赖 Android 的纯逻辑，方便把边界情况全部用单元测试覆盖。
 */
class AlbumRoundPlanner(private val random: Random = Random.Default) {

    private var allAlbums: List<Long> = emptyList()
    private var unplayed: MutableList<Long> = mutableListOf()
    private var current: Long? = null
    private var total: Int = 0

    val currentAlbumId: Long? get() = current

    /** 本轮总共有多少张专辑。 */
    val roundTotal: Int get() = total

    /** 本轮还剩多少张没播。 */
    val remaining: Int get() = unplayed.size

    /** 本轮已经播过多少张。 */
    val played: Int get() = total - unplayed.size

    /**
     * 用全部专辑重建轮次状态。切进「按专辑播放」时调用。
     */
    fun reset(albumIds: List<Long>) {
        allAlbums = albumIds.distinct()
        startNewRound()
        current = null
    }

    /** 手动点歌：把某首歌所在专辑设为当前专辑。 */
    fun setCurrentAlbum(albumId: Long) {
        current = albumId
        // 本轮已经播过的专辑不在池里，remove 不会有副作用；
        // 没播过的会被移出池子，计为已播。
        unplayed.remove(albumId)
    }

    /**
     * 当前专辑播完，抽取下一张。
     *
     * 池子空了说明一轮走完，自动开启新一轮再抽。
     * 没有任何专辑时返回 null（调用方不能崩）。
     */
    fun nextAlbum(): Long? {
        if (allAlbums.isEmpty()) return null
        if (unplayed.isEmpty()) startNewRound()

        val index = random.nextInt(unplayed.size)
        val picked = unplayed.removeAt(index)
        current = picked
        return picked
    }

    private fun startNewRound() {
        unplayed = allAlbums.toMutableList()
        total = allAlbums.size
        current = null
    }
}

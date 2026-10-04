package com.musemusic45.data.model

/**
 * 四种播放方式。
 *
 * - [LIST_LOOP] 列表循环：按当前列表顺序放完一遍，再从头
 * - [SINGLE_LOOP] 单曲循环：一直重复当前这首歌
 * - [SHUFFLE] 随机播放：打乱当前列表随机放
 * - [ALBUM_SHUFFLE] 按专辑播放：随机抽一张专辑整张放完，再从没播过的专辑里抽下一张
 */
enum class PlayMode {
    LIST_LOOP,
    SINGLE_LOOP,
    SHUFFLE,
    ALBUM_SHUFFLE,
    ;

    val label: String
        get() = when (this) {
            LIST_LOOP -> "列表循环"
            SINGLE_LOOP -> "单曲循环"
            SHUFFLE -> "随机播放"
            ALBUM_SHUFFLE -> "按专辑播放"
        }

    val description: String
        get() = when (this) {
            LIST_LOOP -> "按顺序放完一遍，再从头"
            SINGLE_LOOP -> "一直重复当前这首歌"
            SHUFFLE -> "打乱当前列表随机放"
            ALBUM_SHUFFLE -> "整张专辑放完再随机下一张"
        }

    /** 播放页顶栏的标题文字。 */
    val playerTitle: String
        get() = when (this) {
            LIST_LOOP -> "正在播放"
            SINGLE_LOOP -> "单曲循环中"
            SHUFFLE -> "随机播放中"
            ALBUM_SHUFFLE -> "正在按专辑播放"
        }

    /**
     * 手动「下一首 / 上一首」是否仍然切歌。
     *
     * 单曲循环下**要切** —— 用户主动点下一首时，期望的是换歌，
     * 而不是被"单曲"困住。真正的"自动重播"由播放器的循环模式负责，
     * 与手动切歌是两回事。
     */
    val manualSkipChangesTrack: Boolean get() = true
}

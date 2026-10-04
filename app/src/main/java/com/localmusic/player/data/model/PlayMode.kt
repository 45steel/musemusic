package com.localmusic.player.data.model

/**
 * 三种播放方式。
 *
 * - [LIST_LOOP] 列表循环：按当前列表顺序放完一遍，再从头
 * - [SHUFFLE] 随机播放：打乱当前列表随机放
 * - [ALBUM_SHUFFLE] 按专辑播放：随机抽一张专辑整张放完，再从没播过的专辑里抽下一张
 */
enum class PlayMode {
    LIST_LOOP,
    SHUFFLE,
    ALBUM_SHUFFLE,
    ;

    val label: String
        get() = when (this) {
            LIST_LOOP -> "列表循环"
            SHUFFLE -> "随机播放"
            ALBUM_SHUFFLE -> "按专辑播放"
        }

    val description: String
        get() = when (this) {
            LIST_LOOP -> "按顺序放完一遍，再从头"
            SHUFFLE -> "打乱当前列表随机放"
            ALBUM_SHUFFLE -> "整张专辑放完再随机下一张"
        }

    /** 播放页顶栏的标题文字。 */
    val playerTitle: String
        get() = when (this) {
            LIST_LOOP -> "正在播放"
            SHUFFLE -> "随机播放中"
            ALBUM_SHUFFLE -> "正在按专辑播放"
        }
}

package com.musemusic45.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一的圆角与间距令牌。
 *
 * 抽出来是为了让"同一类元素用同一个圆角"有**单一出处** ——
 * 之前列表小封面 6dp、网格封面 8dp、播放页封面 16dp 各不相同，
 * 看起来像三个人拼出来的界面。
 *
 * 纯数据 + 纯函数，可以直接单测。
 */
object AppShapes {

    /** 列表里的小封面（44dp 上下）。 */
    val coverSmall = 12.dp

    /** 网格与详情页的中等封面（110–120dp）。 */
    val coverMedium = 16.dp

    /** 大卡片：播放页封面、控制卡片。 */
    val cardLarge = 28.dp

    /** 迷你播放器：比大卡片小一号，因为它浮在底部条上。 */
    val miniPlayer = 20.dp

    /** 搜索条、占位搜索框的胶囊圆角。 */
    val searchBar = 22.dp

    /**
     * 按封面尺寸给圆角。
     *
     * 圆角要和尺寸成比例：44dp 的小图配 28dp 圆角会变成药丸，
     * 260dp 的大图配 8dp 圆角又像没做过圆角。
     */
    fun forCoverSize(size: Dp): Dp = when {
        size < 56.dp -> coverSmall
        size < 180.dp -> coverMedium
        else -> cardLarge
    }
}

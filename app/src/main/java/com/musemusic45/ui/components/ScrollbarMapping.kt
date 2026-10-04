package com.musemusic45.ui.components

import kotlin.math.roundToInt

/**
 * 列表滚动条的位置换算。
 *
 * 抽成纯函数的理由：这段是最容易"差一位"的地方 ——
 * 分段标题在懒加载列表里**占用一个下标**，做换算时很容易只按条目数算，
 * 结果拖动滚动条总是偏一段。单测把边界（第一项、最后一项、空列表、
 * 只有一段）都钉住。
 */
object ScrollbarMapping {

    /**
     * 滚动条上的比例（0..1）→ 扁平列表下标。
     *
     * 比例 0 对应第一项，1 对应最后一项。
     */
    fun indexForRatio(ratio: Float, itemCount: Int): Int {
        if (itemCount <= 0) return 0
        val safe = ratio.coerceIn(0f, 1f)
        return (safe * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
    }

    /**
     * 扁平列表下标 → 滚动条上的比例（0..1）。用来画滑块的位置。
     */
    fun ratioForIndex(index: Int, itemCount: Int): Float {
        if (itemCount <= 1) return 0f
        return (index.toFloat() / (itemCount - 1)).coerceIn(0f, 1f)
    }

    /**
     * 滑块的长度占比。
     *
     * 可见项占总项数的比例，并设一个下限 —— 否则长列表里滑块会细得看不见、也抓不住。
     */
    fun thumbRatio(visibleCount: Int, itemCount: Int, minRatio: Float = MIN_THUMB_RATIO): Float {
        if (itemCount <= 0) return 1f
        val raw = visibleCount.toFloat() / itemCount
        return raw.coerceIn(minRatio, 1f)
    }

    /**
     * [flatIndex] 落在第几段。
     *
     * [sectionStarts] 是每段的起始下标（升序）。找不到时返回 0。
     */
    fun sectionIndexFor(flatIndex: Int, sectionStarts: List<Int>): Int {
        if (sectionStarts.isEmpty()) return -1
        var answer = 0
        for (index in sectionStarts.indices) {
            if (sectionStarts[index] <= flatIndex) answer = index else break
        }
        return answer
    }

    /** 滑块最短占比，保证还抓得住。 */
    const val MIN_THUMB_RATIO = 0.06f
}

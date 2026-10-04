package com.musemusic45.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 滚动条位置换算。
 *
 * 最容易出错的是"差一位"和"拖动总是偏一段"，所以边界全部钉住。
 */
class ScrollbarMappingTest {

    @Test
    fun `比例零对应第一项 比例一对应最后一项`() {
        assertEquals(0, ScrollbarMapping.indexForRatio(0f, 100))
        assertEquals(99, ScrollbarMapping.indexForRatio(1f, 100))
    }

    @Test
    fun `中间比例落在中间位置`() {
        assertEquals(50, ScrollbarMapping.indexForRatio(0.5f, 101))
        // 0.5 x 99 = 49.5，roundToInt 四舍五入到 50
        assertEquals(50, ScrollbarMapping.indexForRatio(0.5f, 100))
    }

    @Test
    fun `比例越界会被夹住`() {
        assertEquals(0, ScrollbarMapping.indexForRatio(-5f, 100))
        assertEquals(99, ScrollbarMapping.indexForRatio(9f, 100))
    }

    @Test
    fun `空列表不会崩 返回零`() {
        assertEquals(0, ScrollbarMapping.indexForRatio(0.5f, 0))
        assertEquals(0, ScrollbarMapping.indexForRatio(0.5f, -3))
    }

    @Test
    fun `只有一项时任何比例都指向它`() {
        assertEquals(0, ScrollbarMapping.indexForRatio(0f, 1))
        assertEquals(0, ScrollbarMapping.indexForRatio(1f, 1))
        assertEquals(0f, ScrollbarMapping.ratioForIndex(0, 1), 1e-6f)
    }

    @Test
    fun `下标与比例互为逆运算`() {
        val count = 200
        for (index in listOf(0, 1, 57, 120, 199)) {
            val ratio = ScrollbarMapping.ratioForIndex(index, count)
            assertEquals(index, ScrollbarMapping.indexForRatio(ratio, count))
        }
    }

    @Test
    fun `滑块占比按可见比例算`() {
        assertEquals(0.1f, ScrollbarMapping.thumbRatio(10, 100), 1e-4f)
        assertEquals(0.5f, ScrollbarMapping.thumbRatio(50, 100), 1e-4f)
    }

    @Test
    fun `滑块有最短长度 否则抓不住`() {
        // 一万项里只看得到 5 项，按比例只有 0.0005，会细得看不见
        assertEquals(ScrollbarMapping.MIN_THUMB_RATIO, ScrollbarMapping.thumbRatio(5, 10_000), 1e-6f)
    }

    @Test
    fun `滑块不会超过整条`() {
        assertEquals(1f, ScrollbarMapping.thumbRatio(200, 100), 1e-6f)
        assertEquals(1f, ScrollbarMapping.thumbRatio(0, 0), 1e-6f)
    }

    @Test
    fun `按段起始下标找出所属段`() {
        // 三段：A 从 0 开始，B 从 5 开始，C 从 12 开始
        val starts = listOf(0, 5, 12)
        assertEquals(0, ScrollbarMapping.sectionIndexFor(0, starts))
        assertEquals(0, ScrollbarMapping.sectionIndexFor(4, starts))
        assertEquals(1, ScrollbarMapping.sectionIndexFor(5, starts))
        assertEquals(1, ScrollbarMapping.sectionIndexFor(11, starts))
        assertEquals(2, ScrollbarMapping.sectionIndexFor(12, starts))
        assertEquals(2, ScrollbarMapping.sectionIndexFor(999, starts))
    }

    @Test
    fun `没有分段时返回负一`() {
        assertEquals(-1, ScrollbarMapping.sectionIndexFor(3, emptyList()))
    }

    @Test
    fun `段起始下标必须单调不减`() {
        val starts = listOf(0, 5, 12, 20)
        for (index in 1 until starts.size) {
            assertTrue(starts[index] > starts[index - 1])
        }
    }
}

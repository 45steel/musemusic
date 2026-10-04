package com.musemusic45.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 圆角与间距令牌。
 *
 * 这里唯一有逻辑的是 [AppShapes.forCoverSize]：圆角要随尺寸成比例。
 * 出错的典型表现是"某个页面的封面圆角明显跟别处不一样"，
 * 而肉眼在小尺寸下很难一眼发现，所以钉住。
 */
class AppShapesTest {

    @Test
    fun `小封面走小圆角`() {
        assertEquals(AppShapes.coverSmall, AppShapes.forCoverSize(44.dp))
        assertEquals(AppShapes.coverSmall, AppShapes.forCoverSize(48.dp))
        assertEquals(AppShapes.coverSmall, AppShapes.forCoverSize(55.dp))
    }

    @Test
    fun `中等封面走中等圆角`() {
        assertEquals(AppShapes.coverMedium, AppShapes.forCoverSize(56.dp))
        assertEquals(AppShapes.coverMedium, AppShapes.forCoverSize(110.dp))
        assertEquals(AppShapes.coverMedium, AppShapes.forCoverSize(120.dp))
        assertEquals(AppShapes.coverMedium, AppShapes.forCoverSize(179.dp))
    }

    @Test
    fun `大封面走大圆角`() {
        assertEquals(AppShapes.cardLarge, AppShapes.forCoverSize(180.dp))
        assertEquals(AppShapes.cardLarge, AppShapes.forCoverSize(260.dp))
    }

    @Test
    fun `圆角随尺寸单调不减`() {
        val samples = listOf(24.dp, 44.dp, 56.dp, 110.dp, 179.dp, 180.dp, 260.dp, 400.dp)
        val radii = samples.map { AppShapes.forCoverSize(it) }
        for (index in 1 until radii.size) {
            assertTrue(
                "尺寸变大时圆角不该变小：${samples[index - 1]} -> ${radii[index - 1]}，" +
                    "${samples[index]} -> ${radii[index]}",
                radii[index] >= radii[index - 1],
            )
        }
    }

    @Test
    fun `圆角永远不超过封面边长的一半`() {
        // 超过一半就不再是圆角，而是药丸了
        val samples = listOf(24.dp, 44.dp, 56.dp, 110.dp, 179.dp, 180.dp, 260.dp)
        for (size in samples) {
            val radius = AppShapes.forCoverSize(size)
            assertTrue(
                "封面 $size 配了 $radius 的圆角，超过了边长的一半",
                radius.value * 2 <= size.value,
            )
        }
    }

    @Test
    fun `层级关系保持`() {
        // 小 < 中 < 大，且迷你播放器介于中与大之间
        assertTrue(AppShapes.coverSmall < AppShapes.coverMedium)
        assertTrue(AppShapes.coverMedium < AppShapes.miniPlayer)
        assertTrue(AppShapes.miniPlayer < AppShapes.cardLarge)
    }
}

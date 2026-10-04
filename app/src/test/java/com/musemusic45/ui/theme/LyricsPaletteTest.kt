package com.musemusic45.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 歌词配色。
 *
 * 重点钉住一件事：**非当前行的颜色必须还在主题色与背景色之间** ——
 * 也就是"同色系的低饱和"，而不是被混成了灰色。混成灰色是最容易犯的错，
 * 而且肉眼在小字号下不容易立刻发现。
 */
class LyricsPaletteTest {

    private val primary = Color(0xFF3F51B5)      // 靛蓝
    private val background = Color(0xFFFBF8FF)   // 近白

    private fun channels(c: Color) = listOf(c.red, c.green, c.blue)

    /** 判断 [value] 是否落在 [a] 与 [b] 之间（含端点）。 */
    private fun between(value: Float, a: Float, b: Float): Boolean =
        value >= minOf(a, b) - 1e-4f && value <= maxOf(a, b) + 1e-4f

    @Test
    fun `当前行就是主题色本身`() {
        assertEquals(primary, LyricsPalette.current(primary))
    }

    @Test
    fun `混合比例为零时等于主题色`() {
        assertEquals(primary, LyricsPalette.idle(primary, background, 0f))
    }

    @Test
    fun `混合比例为一等于背景色`() {
        assertEquals(background, LyricsPalette.idle(primary, background, 1f))
    }

    @Test
    fun `非当前行的三个通道都落在主题色与背景色之间`() {
        val idle = LyricsPalette.idle(primary, background)
        val p = channels(primary)
        val b = channels(background)
        val i = channels(idle)
        for (index in 0..2) {
            assertTrue(
                "通道 $index 跑出了范围：${i[index]} 不在 ${p[index]} 与 ${b[index]} 之间",
                between(i[index], p[index], b[index]),
            )
        }
    }

    @Test
    fun `非当前行比当前行更接近背景`() {
        val idle = LyricsPalette.idle(primary, background)
        fun distanceToBackground(c: Color) =
            abs(c.red - background.red) + abs(c.green - background.green) + abs(c.blue - background.blue)

        assertTrue(
            "非当前行应该更淡",
            distanceToBackground(idle) < distanceToBackground(primary),
        )
    }

    @Test
    fun `非当前行没有被混成灰色`() {
        // 灰色意味着三个通道几乎相等；莫奈风格下应当仍能看出色相
        val idle = LyricsPalette.idle(primary, background)
        val spread = maxOf(idle.red, idle.green, idle.blue) - minOf(idle.red, idle.green, idle.blue)
        assertTrue("色相被混没了（三通道几乎相等，spread=$spread）", spread > 0.02f)
    }

    @Test
    fun `半透明只作用于非当前行`() {
        // Color 的通道是 8 位量化的：0.75 存进去会变成 191/255 = 0.74902，
        // 所以容差取 1/255 而不是 1e-4
        val tolerance = 1f / 255f
        val faded = LyricsPalette.idleFaded(primary, background)
        assertEquals(LyricsPalette.IDLE_ALPHA, faded.alpha, tolerance)
        // 叠了 alpha 之后，RGB 本身仍然是混出来的那个颜色
        assertEquals(LyricsPalette.idle(primary, background), faded.copy(alpha = 1f))
    }

    @Test
    fun `比例越界会被夹住`() {
        val tolerance = 1f / 255f
        assertEquals(primary, LyricsPalette.idle(primary, background, -3f))
        assertEquals(background, LyricsPalette.idle(primary, background, 9f))
        assertEquals(1f, LyricsPalette.idleFaded(primary, background, alpha = 4f).alpha, tolerance)
        assertEquals(0f, LyricsPalette.idleFaded(primary, background, alpha = -4f).alpha, tolerance)
    }

    @Test
    fun `未唱到的部分比非当前行更靠近主题色`() {
        val unsung = LyricsPalette.unsung(primary, background)
        val idle = LyricsPalette.idle(primary, background)
        // 未唱到 → 更接近主题色；已弱化 → 更接近背景
        assertTrue(
            "未唱到的部分应当比非当前行更醒目",
            abs(unsung.red - primary.red) < abs(idle.red - primary.red),
        )
    }

    @Test
    fun `深色主题下同样成立`() {
        val darkPrimary = Color(0xFFBAC3FF)
        val darkBackground = Color(0xFF131318)
        val idle = LyricsPalette.idle(darkPrimary, darkBackground)
        val p = channels(darkPrimary)
        val b = channels(darkBackground)
        val i = channels(idle)
        for (index in 0..2) {
            assertTrue(between(i[index], p[index], b[index]))
        }
    }
}

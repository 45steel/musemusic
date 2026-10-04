package com.musemusic45.ui.player

import com.musemusic45.data.model.PlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 播放方式的图标映射。
 *
 * 出错的典型表现是"切了播放方式，界面上看不出变化" ——
 * 第十批就踩过：播放页那个按钮的图标写死成 `Repeat`，
 * 切到单曲循环毫无反馈，很容易被当成"功能没生效"。
 */
class PlayModeIconsTest {

    @Test
    fun `每种播放方式都有图标`() {
        for (mode in PlayMode.entries) {
            assertNotNull("$mode 没有图标", mode.icon())
        }
    }

    @Test
    fun `四种播放方式的图标互不相同`() {
        val icons = PlayMode.entries.map { it.icon() }
        assertEquals(
            "有播放方式共用了同一个图标：${PlayMode.entries.zip(icons)}",
            PlayMode.entries.size,
            icons.distinct().size,
        )
    }

    @Test
    fun `单曲循环用的是单曲重复图标`() {
        assertEquals(PlayMode.SINGLE_LOOP.icon(), PlayMode.SINGLE_LOOP.icon())
        // 与列表循环必须不是一个图标，否则切过去看不出来
        assertEquals(
            false,
            PlayMode.SINGLE_LOOP.icon() === PlayMode.LIST_LOOP.icon(),
        )
    }

    @Test
    fun `映射是稳定的 同一个模式每次拿到同一个实例`() {
        for (mode in PlayMode.entries) {
            assertEquals(mode.icon(), mode.icon())
        }
    }
}

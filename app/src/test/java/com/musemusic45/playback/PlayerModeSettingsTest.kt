package com.musemusic45.playback

import androidx.media3.common.Player
import com.musemusic45.data.model.PlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 播放方式 → 播放器循环/随机设置的映射。
 *
 * 这段映射以前散在 `when` 里，加单曲循环时最容易漏掉或写错。
 * 尤其是「按专辑播放」必须关循环 —— 单专辑队列一旦自己转圈，
 * 就永远播不完，也就永远触发不了"换下一张专辑"。
 */
class PlayerModeSettingsTest {

    @Test
    fun `列表循环是全部循环且不随机`() {
        val s = PlayerModeSettings.of(PlayMode.LIST_LOOP)
        assertEquals(Player.REPEAT_MODE_ALL, s.repeatMode)
        assertFalse(s.shuffle)
    }

    @Test
    fun `单曲循环是单曲重复且不随机`() {
        val s = PlayerModeSettings.of(PlayMode.SINGLE_LOOP)
        assertEquals(Player.REPEAT_MODE_ONE, s.repeatMode)
        assertFalse(s.shuffle)
    }

    @Test
    fun `随机播放是全部循环且开启随机`() {
        val s = PlayerModeSettings.of(PlayMode.SHUFFLE)
        assertEquals(Player.REPEAT_MODE_ALL, s.repeatMode)
        assertTrue(s.shuffle)
    }

    @Test
    fun `按专辑播放必须关掉循环`() {
        val s = PlayerModeSettings.of(PlayMode.ALBUM_SHUFFLE)
        assertEquals(Player.REPEAT_MODE_OFF, s.repeatMode)
        assertFalse(s.shuffle)
    }

    @Test
    fun `每种播放方式都有设置 没有遗漏`() {
        for (mode in PlayMode.entries) {
            val s = PlayerModeSettings.of(mode)
            assertTrue(
                "$mode 的循环模式不合法：${s.repeatMode}",
                s.repeatMode in setOf(
                    Player.REPEAT_MODE_ALL,
                    Player.REPEAT_MODE_ONE,
                    Player.REPEAT_MODE_OFF,
                ),
            )
        }
    }

    @Test
    fun `只有单曲循环用单曲重复`() {
        val withOne = PlayMode.entries.filter {
            PlayerModeSettings.of(it).repeatMode == Player.REPEAT_MODE_ONE
        }
        assertEquals(listOf(PlayMode.SINGLE_LOOP), withOne)
    }

    @Test
    fun `只有随机播放开启随机`() {
        val shuffling = PlayMode.entries.filter { PlayerModeSettings.of(it).shuffle }
        assertEquals(listOf(PlayMode.SHUFFLE), shuffling)
    }

    @Test
    fun `只有按专辑播放关掉循环`() {
        val off = PlayMode.entries.filter {
            PlayerModeSettings.of(it).repeatMode == Player.REPEAT_MODE_OFF
        }
        assertEquals(listOf(PlayMode.ALBUM_SHUFFLE), off)
    }
}

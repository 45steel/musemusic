package com.musemusic45.playback

import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.testSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeSwitchTest {

    // ------------------------------------------------ 是否需要重建专辑队列

    @Test
    fun `切到按专辑播放需要重建专辑队列`() {
        assertTrue(ModeSwitch.needsAlbumQueue(PlayMode.ALBUM_SHUFFLE))
    }

    @Test
    fun `切到列表循环或随机播放不需要重建专辑队列`() {
        assertFalse(ModeSwitch.needsAlbumQueue(PlayMode.LIST_LOOP))
        assertFalse(ModeSwitch.needsAlbumQueue(PlayMode.SHUFFLE))
    }

    // ------------------------------------------------ 是否需要恢复完整队列

    @Test
    fun `从按专辑播放切到列表循环要恢复完整队列`() {
        assertTrue(ModeSwitch.needsFullQueueRestore(PlayMode.ALBUM_SHUFFLE, PlayMode.LIST_LOOP))
    }

    @Test
    fun `从按专辑播放切到随机播放要恢复完整队列`() {
        assertTrue(ModeSwitch.needsFullQueueRestore(PlayMode.ALBUM_SHUFFLE, PlayMode.SHUFFLE))
    }

    @Test
    fun `在列表与随机之间互切不需要恢复队列`() {
        assertFalse(ModeSwitch.needsFullQueueRestore(PlayMode.LIST_LOOP, PlayMode.SHUFFLE))
        assertFalse(ModeSwitch.needsFullQueueRestore(PlayMode.SHUFFLE, PlayMode.LIST_LOOP))
    }

    @Test
    fun `切到按专辑播放不需要恢复完整队列`() {
        assertFalse(ModeSwitch.needsFullQueueRestore(PlayMode.LIST_LOOP, PlayMode.ALBUM_SHUFFLE))
        assertFalse(ModeSwitch.needsFullQueueRestore(PlayMode.SHUFFLE, PlayMode.ALBUM_SHUFFLE))
    }

    @Test
    fun `按专辑播放切给自己不算恢复队列`() {
        assertFalse(ModeSwitch.needsFullQueueRestore(PlayMode.ALBUM_SHUFFLE, PlayMode.ALBUM_SHUFFLE))
    }

    // ------------------------------------------------ 切换时机（第十批补的坑）

    @Test
    fun `模式没变化时什么都不用做`() {
        assertEquals(null, ModeSwitch.applyTiming(PlayMode.LIST_LOOP, PlayMode.LIST_LOOP, playerReady = true))
        assertEquals(null, ModeSwitch.applyTiming(PlayMode.SINGLE_LOOP, PlayMode.SINGLE_LOOP, playerReady = false))
    }

    @Test
    fun `播放器就绪时立即应用`() {
        assertEquals(
            ModeSwitch.Timing.NOW,
            ModeSwitch.applyTiming(PlayMode.SINGLE_LOOP, PlayMode.LIST_LOOP, playerReady = true),
        )
    }

    /**
     * 这是第十批那个真实缺陷：启动时 `connect()` 还在挂起，
     * `setMode(存档模式)` 已经跑完了，那会儿 controller 是 null。
     * 如果这时直接返回而不记录，存档的播放方式就被静默丢掉 ——
     * 表现是"设了单曲循环，重启后变回列表循环"。
     */
    @Test
    fun `播放器还没连上时必须先记下来而不是丢掉`() {
        assertEquals(
            ModeSwitch.Timing.DEFER,
            ModeSwitch.applyTiming(PlayMode.SINGLE_LOOP, PlayMode.LIST_LOOP, playerReady = false),
        )
    }

    @Test
    fun `四种模式在未连接时都会被推迟而不是丢弃`() {
        for (mode in PlayMode.entries) {
            if (mode == PlayMode.LIST_LOOP) continue
            assertEquals(
                "启动时恢复 $mode 会被丢掉",
                ModeSwitch.Timing.DEFER,
                ModeSwitch.applyTiming(mode, PlayMode.LIST_LOOP, playerReady = false),
            )
        }
    }

    // ------------------------------------------------------ 播放位置保留

    @Test
    fun `正常位置原样返回`() {
        assertEquals(12_345L, ModeSwitch.resumePosition(12_345L))
        assertEquals(0L, ModeSwitch.resumePosition(0L))
    }

    @Test
    fun `负数位置归零`() {
        assertEquals(0L, ModeSwitch.resumePosition(-1L))
        assertEquals(0L, ModeSwitch.resumePosition(Long.MIN_VALUE))
    }

    // ---------------------------------------------------- 完整队列里定位

    @Test
    fun `能在完整队列里按 ID 找到下标`() {
        val songs = listOf(testSong(10), testSong(20), testSong(30))
        assertEquals(2, ModeSwitch.indexInFullQueue(songs, 30L))
    }

    @Test
    fun `找不到时退回 0`() {
        val songs = listOf(testSong(10), testSong(20))
        assertEquals(0, ModeSwitch.indexInFullQueue(songs, 999L))
    }

    @Test
    fun `空队列也退回 0`() {
        assertEquals(0, ModeSwitch.indexInFullQueue(emptyList(), 10L))
    }
}

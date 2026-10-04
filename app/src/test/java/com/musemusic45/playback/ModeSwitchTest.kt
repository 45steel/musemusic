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

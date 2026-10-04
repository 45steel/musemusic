package com.localmusic.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistAvatarTest {

    @Test
    fun `同一个歌手名总是得到同一个下标`() {
        val names = listOf("周杰伦", "林俊杰", "王菲", "Beyond", "未知歌手", "A")
        for (name in names) {
            val first = avatarColorIndexFor(name)
            repeat(50) {
                assertEquals("歌手 $name 的颜色下标不稳定", first, avatarColorIndexFor(name))
            }
        }
    }

    @Test
    fun `下标始终落在配色板范围内`() {
        val samples = listOf(
            "周杰伦", "林俊杰", "王菲", "Beyond", "未知歌手",
            "AC/DC", "", "a", "凤凰传奇", "五月天", "陈奕迅", "邓紫棋",
            "很长的歌手名字用来看会不会越界很长的歌手名字用来看会不会越界",
        )
        for (name in samples) {
            val index = avatarColorIndexFor(name)
            assertTrue("歌手 $name 的下标 $index 越界", index in 0 until AVATAR_PALETTE_SIZE)
        }
    }

    @Test
    fun `空名字落到第一个颜色`() {
        assertEquals(0, avatarColorIndexFor(""))
    }

    @Test
    fun `不同歌手不会全部挤在同一个颜色上`() {
        val names = listOf("周杰伦", "林俊杰", "王菲", "Beyond", "五月天", "陈奕迅", "邓紫棋", "许嵩")
        val distinct = names.map { avatarColorIndexFor(it) }.toSet()
        assertTrue("8 位歌手只用了 ${distinct.size} 种颜色，配色板可能失效", distinct.size >= 3)
    }

    @Test
    fun `负的哈希值不会产生负下标`() {
        // 找一个 hashCode 为负的名字来覆盖取模分支
        val negative = generateSequence(0) { it + 1 }
            .map { "歌手$it" }
            .first { it.hashCode() < 0 }
        assertTrue(avatarColorIndexFor(negative) >= 0)
    }
}

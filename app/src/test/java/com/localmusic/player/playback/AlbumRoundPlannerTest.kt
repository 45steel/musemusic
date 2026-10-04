package com.localmusic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlbumRoundPlannerTest {

    private fun planner(seed: Int = 42) = AlbumRoundPlanner(Random(seed))

    // -------------------------------------------------------- 基本轮次

    @Test
    fun `一轮之内专辑不重复`() {
        val ids = (1L..5L).toList()
        val p = planner()
        p.reset(ids)
        p.setCurrentAlbum(ids.first())

        val picked = mutableListOf<Long>()
        while (p.remaining > 0) {
            picked += p.nextAlbum()!!
        }
        assertEquals(4, picked.size)
        assertEquals("抽到的专辑有重复: $picked", picked.size, picked.distinct().size)
        assertTrue("抽到了本轮已播过的专辑", picked.none { it == ids.first() })
    }

    @Test
    fun `一轮抽完后自动开启新一轮`() {
        val ids = (1L..3L).toList()
        val p = planner()
        p.reset(ids)
        p.setCurrentAlbum(ids[0])

        val round1 = listOfNotNull(p.nextAlbum(), p.nextAlbum())
        assertEquals(2, round1.size)
        assertEquals(0, p.remaining)
        assertEquals(3, p.played)

        // 再抽一次应当进入新一轮并给出全部专辑
        val firstOfRound2 = p.nextAlbum()
        assertNotNull(firstOfRound2)
        assertEquals("新一轮剩余数不对", 2, p.remaining)
        assertEquals(3, p.roundTotal)

        val round2 = listOfNotNull(firstOfRound2, p.nextAlbum(), p.nextAlbum())
        assertEquals(3, round2.distinct().size)
        assertEquals(ids.toSet(), round2.toSet())
    }

    @Test
    fun `roundTotal 始终是专辑总数`() {
        val p = planner()
        p.reset((1L..7L).toList())
        assertEquals(7, p.roundTotal)
        p.setCurrentAlbum(1L)
        assertEquals(7, p.roundTotal)
        p.nextAlbum()
        assertEquals(7, p.roundTotal)
    }

    // -------------------------------------------------------- 手动点歌

    @Test
    fun `手动点本轮已播过的专辑不消耗专辑池`() {
        val ids = (1L..4L).toList()
        val p = planner()
        p.reset(ids)
        p.setCurrentAlbum(1L)

        val second = p.nextAlbum()!!
        val third = p.nextAlbum()!!
        val remainingBefore = p.remaining

        // 手动点回第一张（本轮已播过）
        p.setCurrentAlbum(1L)

        assertEquals("已播专辑不该消耗池子", remainingBefore, p.remaining)
        assertEquals(1L, p.currentAlbumId)
        // 已播张数也不该增加
        assertEquals(3, p.played)
    }

    @Test
    fun `手动点本轮没播过的专辑会消耗专辑池`() {
        val ids = (1L..4L).toList()
        val p = planner()
        p.reset(ids)
        p.setCurrentAlbum(1L)

        val remainingBefore = p.remaining
        val notPlayedYet = ids.first { it != 1L }
        p.setCurrentAlbum(notPlayedYet)

        assertEquals("未播专辑应当被移出池子", remainingBefore - 1, p.remaining)
        assertEquals(notPlayedYet, p.currentAlbumId)
    }

    @Test
    fun `手动点歌后继续抽不会抽到已播专辑`() {
        val ids = (1L..5L).toList()
        val p = planner()
        p.reset(ids)

        p.setCurrentAlbum(2L)
        val picked = p.nextAlbum()!!

        assertTrue("抽到了当前专辑", picked != 2L)
        assertEquals(3, p.remaining)
    }

    // -------------------------------------------------------- 边界情况

    @Test
    fun `只有一张专辑时不死循环也不崩溃`() {
        val p = planner()
        p.reset(listOf(9L))
        assertEquals(1, p.roundTotal)
        assertEquals(1, p.remaining)

        repeat(20) {
            val picked = p.nextAlbum()
            assertEquals("唯一一张专辑应当一直被抽到", 9L, picked)
        }
    }

    @Test
    fun `只有一张专辑时手动点它也不会出问题`() {
        val p = planner()
        p.reset(listOf(9L))
        p.setCurrentAlbum(9L)
        assertEquals(0, p.remaining)
        assertEquals(9L, p.nextAlbum())
        assertEquals(9L, p.nextAlbum())
    }

    @Test
    fun `零张专辑时返回 null`() {
        val p = planner()
        p.reset(emptyList())
        assertEquals(0, p.roundTotal)
        assertEquals(0, p.remaining)
        assertNull(p.nextAlbum())
        assertNull(p.nextAlbum())
    }

    @Test
    fun `专辑列表里的重复项会被去重`() {
        val p = planner()
        p.reset(listOf(1L, 1L, 2L, 2L, 3L))
        assertEquals(3, p.roundTotal)
        assertEquals(3, p.remaining)
    }

    @Test
    fun `reset 之后回到全新一轮`() {
        val p = planner()
        p.reset((1L..5L).toList())
        p.setCurrentAlbum(1L)
        repeat(4) { p.nextAlbum() }
        assertEquals(0, p.remaining)

        p.reset((1L..5L).toList())
        assertEquals(5, p.remaining)
        assertEquals(0, p.played)
        assertNull(p.currentAlbumId)
    }

    @Test
    fun `大量专辑反复抽样始终满足一轮不重复`() {
        val ids = (1L..50L).toList()
        val p = planner(seed = 7)
        p.reset(ids)

        repeat(3) { round ->
            val seen = mutableListOf<Long>()
            // 一轮 50 张：第一次抽 + 后续 49 次
            repeat(50) { seen += p.nextAlbum()!! }
            assertEquals("第 $round 轮有重复", 50, seen.distinct().size)
            assertEquals("第 $round 轮没覆盖全部专辑", ids.toSet(), seen.toSet())
        }
    }
}

package com.musemusic45.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 分段在扁平列表里的位置。
 *
 * 关键点：**分段标题在懒加载列表里占用一个下标**。
 * 做滚动条换算时如果漏算标题，拖动总会偏一段。
 */
class ListSectionsPositionsTest {

    private fun section(key: String, vararg items: String) = ListSection(key, items.toList())

    @Test
    fun `每段的起始下标把标题算进去`() {
        val sections = listOf(
            section("A", "a1", "a2"),
            section("B", "b1"),
            section("C", "c1", "c2", "c3"),
        )
        // 布局：[A][a1][a2][B][b1][C][c1][c2][c3]
        //         0   1   2   3   4   5   6   7   8
        // 段起点是**标题**的下标：A=0、B=3、C=5
        assertEquals(listOf(0, 3, 5), ListSections.flatStartIndices(sections))
        assertEquals(9, ListSections.flatCount(sections))
    }

    @Test
    fun `没有标题的段不占位`() {
        val sections = listOf(
            ListSection("", listOf("x1", "x2")),
            ListSection("", listOf("y1")),
        )
        assertEquals(listOf(0, 2), ListSections.flatStartIndices(sections))
        assertEquals(3, ListSections.flatCount(sections))
    }

    @Test
    fun `混合有无标题`() {
        val sections = listOf(
            ListSection("", listOf("x1")),
            section("A", "a1", "a2"),
        )
        // [x1][A][a1][a2]
        assertEquals(listOf(0, 1), ListSections.flatStartIndices(sections))
        assertEquals(4, ListSections.flatCount(sections))
    }

    @Test
    fun `空列表得到空结果`() {
        assertEquals(emptyList<Int>(), ListSections.flatStartIndices(emptyList<ListSection<String>>()))
        assertEquals(0, ListSections.flatCount(emptyList<ListSection<String>>()))
    }

    @Test
    fun `空段也只占标题那一位`() {
        val sections = listOf(section("A"), section("B", "b1"))
        // 布局：[A][B][b1] = 3 项
        assertEquals(listOf(0, 1), ListSections.flatStartIndices(sections))
        assertEquals(3, ListSections.flatCount(sections))
    }

    @Test
    fun `起始下标与 flatIndexOf 对得上`() {
        val sections = listOf(
            section("A", "a1", "a2"),
            section("B", "b1", "b2"),
        )
        val starts = ListSections.flatStartIndices(sections)
        // 段起点是标题本身；段内第一个条目在起点 + 1
        assertEquals(starts[0] + 1, ListSections.flatIndexOf(sections) { it == "a1" })
        assertEquals(starts[1] + 1, ListSections.flatIndexOf(sections) { it == "b1" })
        assertEquals(starts[1] + 2, ListSections.flatIndexOf(sections) { it == "b2" })
    }

    @Test
    fun `有标题的段起点正好就是标题的下标`() {
        val sections = listOf(
            section("A", "a1"),
            section("B", "b1"),
        )
        val headerStarts = ListSections.headerStarts(sections)
        assertEquals(listOf("A", "B"), headerStarts.map { it.first })
        assertEquals(listOf(0, 2), headerStarts.map { it.second })
    }

    @Test
    fun `没有标题时 headerStarts 是空的`() {
        val sections = listOf(ListSection("", listOf("x1", "x2")))
        assertEquals(emptyList<Pair<String, Int>>(), ListSections.headerStarts(sections))
    }
}

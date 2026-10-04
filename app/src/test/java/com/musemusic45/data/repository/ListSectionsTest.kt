package com.musemusic45.data.repository

import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortOrder
import com.musemusic45.data.model.SortSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListSectionsTest {

    /** 测试用的假拼音：只认几个汉字，其余原样返回。 */
    private val fakePinyin: (String) -> String = { text ->
        when (text.firstOrNull()) {
            '周' -> "zhou"
            '王' -> "wang"
            '阿' -> "a"
            '张' -> "zhang"
            else -> text
        }
    }

    // ------------------------------------------------------------ 分段键

    @Test
    fun `英文首字母大写`() {
        assertEquals("A", ListSections.initialKey("adele", fakePinyin))
        assertEquals("A", ListSections.initialKey("Adele", fakePinyin))
        assertEquals("Z", ListSections.initialKey("zebra", fakePinyin))
    }

    @Test
    fun `汉字取拼音首字母`() {
        assertEquals("Z", ListSections.initialKey("周杰伦", fakePinyin))
        assertEquals("W", ListSections.initialKey("王菲", fakePinyin))
        assertEquals("A", ListSections.initialKey("阿杜", fakePinyin))
    }

    @Test
    fun `汉字不认识时归入井号`() {
        // 假拼音对「李」原样返回（仍是汉字，没有 ASCII 字母）
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("李宗盛", fakePinyin))
    }

    @Test
    fun `数字和符号开头归入井号`() {
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("123", fakePinyin))
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("-开场-", fakePinyin))
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("《精选》", fakePinyin))
    }

    @Test
    fun `空串归入井号`() {
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("", fakePinyin))
        assertEquals(ListSections.FALLBACK_KEY, ListSections.initialKey("   ", fakePinyin))
    }

    @Test
    fun `首字符前的空白被忽略`() {
        assertEquals("A", ListSections.initialKey("  Adele", fakePinyin))
    }

    // -------------------------------------------------------------- 分组

    @Test
    fun `相邻同键并成一段`() {
        val items = listOf("A1", "A2", "B1")
        val sections = ListSections.group(items) { it.take(1) }
        assertEquals(listOf("A", "B"), sections.map { it.key })
        assertEquals(listOf("A1", "A2"), sections[0].items)
        assertEquals(listOf("B1"), sections[1].items)
    }

    @Test
    fun `同键不相邻时会并到一段`() {
        // 排序用中文 Collator，和首字母顺序不完全一致，会出这种情况
        val items = listOf("A1", "#1", "A2", "#2")
        val sections = ListSections.group(items) { if (it.startsWith("#")) "#" else it.take(1) }
        assertEquals(listOf("A", "#"), sections.map { it.key })
        assertEquals(listOf("A1", "A2"), sections[0].items)
        assertEquals(listOf("#1", "#2"), sections[1].items)
    }

    @Test
    fun `空列表分组得到零段`() {
        assertTrue(ListSections.group(emptyList<String>()) { it }.isEmpty())
    }

    // -------------------------------------------------------- 按排序字段

    private fun spec(field: SortField) = SortSpec(field, SortOrder.ASCENDING)

    @Test
    fun `按名称排序时按首字母分段`() {
        val items = listOf("Adele", "adele2", "周杰伦")
        val sections = ListSections.build(
            items = items,
            spec = spec(SortField.NAME),
            nameOf = { it },
            yearOf = { 0 },
            pinyinOf = fakePinyin,
        )
        assertEquals(listOf("A", "Z"), sections.map { it.key })
    }

    @Test
    fun `按发布年份排序时按年份分段`() {
        val items = listOf(2001, 2001, 2020, 0)
        val sections = ListSections.build(
            items = items,
            spec = spec(SortField.YEAR),
            nameOf = { it.toString() },
            yearOf = { it },
            pinyinOf = fakePinyin,
        )
        assertEquals(listOf("2001 年", "2020 年", ListSections.UNKNOWN_YEAR_KEY), sections.map { it.key })
    }

    @Test
    fun `按添加时间排序时不分段`() {
        val items = listOf(1, 2, 3)
        val sections = ListSections.build(
            items = items,
            spec = spec(SortField.DATE_ADDED),
            nameOf = { it.toString() },
            yearOf = { 0 },
            pinyinOf = fakePinyin,
        )
        assertEquals(1, sections.size)
        assertEquals(listOf(1, 2, 3), sections.first().items)
        assertFalse("不分段时不应显示标题", sections.first().showHeader)
    }

    @Test
    fun `分段时段的顺序跟列表一致`() {
        val items = listOf("Zed", "Bob", "Amy")
        val sections = ListSections.build(
            items = items,
            spec = spec(SortField.NAME),
            nameOf = { it },
            yearOf = { 0 },
            pinyinOf = fakePinyin,
        )
        assertEquals(listOf("Z", "B", "A"), sections.map { it.key })
    }

    @Test
    fun `分段后不丢内容`() {
        val items = (1..30).map { "歌$it" }
        val sections = ListSections.build(
            items = items,
            spec = spec(SortField.NAME),
            nameOf = { it },
            yearOf = { 0 },
            pinyinOf = fakePinyin,
        )
        assertEquals(items, sections.flatMap { it.items })
    }

    @Test
    fun `空列表切段得到零段`() {
        val sections = ListSections.build(
            items = emptyList<String>(),
            spec = spec(SortField.NAME),
            nameOf = { it },
            yearOf = { 0 },
            pinyinOf = fakePinyin,
        )
        assertTrue(sections.isEmpty())
    }

    // ------------------------------------------------ 懒加载列表里的下标

    @Test
    fun `没有标题时下标就是元素下标`() {
        val sections = listOf(ListSection("", listOf("a", "b", "c")))
        assertEquals(0, ListSections.flatIndexOf(sections) { it == "a" })
        assertEquals(2, ListSections.flatIndexOf(sections) { it == "c" })
    }

    @Test
    fun `有标题时标题也占一行`() {
        val sections = listOf(
            ListSection("A", listOf("a1", "a2")),
            ListSection("B", listOf("b1")),
        )
        // 布局是: [标题A][a1][a2][标题B][b1] -> 下标 0..4
        assertEquals(1, ListSections.flatIndexOf(sections) { it == "a1" })
        assertEquals(2, ListSections.flatIndexOf(sections) { it == "a2" })
        assertEquals(4, ListSections.flatIndexOf(sections) { it == "b1" })
    }

    @Test
    fun `多段累加正确`() {
        val sections = listOf(
            ListSection("A", listOf("a1", "a2", "a3")),
            ListSection("B", listOf("b1", "b2")),
            ListSection("C", listOf("c1")),
        )
        // [A][a1][a2][a3][B][b1][b2][C][c1] -> a1 前面有个标题 A，所以是 1
        assertEquals(1, ListSections.flatIndexOf(sections) { it == "a1" })
        assertEquals(5, ListSections.flatIndexOf(sections) { it == "b1" })
        assertEquals(6, ListSections.flatIndexOf(sections) { it == "b2" })
        assertEquals(8, ListSections.flatIndexOf(sections) { it == "c1" })
    }

    @Test
    fun `找不到返回负一`() {
        val sections = listOf(ListSection("A", listOf("a1")))
        assertEquals(-1, ListSections.flatIndexOf(sections) { it == "不存在" })
        assertEquals(-1, ListSections.flatIndexOf(emptyList<ListSection<String>>()) { true })
    }

    @Test
    fun `空段也参与下标计算`() {
        val sections = listOf(
            ListSection("A", emptyList()),
            ListSection("B", listOf("b1")),
        )
        // [标题A][标题B][b1]
        assertEquals(2, ListSections.flatIndexOf(sections) { it == "b1" })
    }
}

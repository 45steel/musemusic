package com.musemusic45.data.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameSortKeyTest {

    /** 假的罗马化实现，用来验证规则本身（真实现是 Android 上的 ICU）。 */
    private val fake: (String) -> String = { text ->
        when {
            text.startsWith("周") -> "zhōu jié lún"
            text.startsWith("阿") -> "ā dù"
            text.startsWith("カ") -> "katakana"
            text.startsWith("あ") -> "AI"
            else -> text
        }
    }

    /** 别把全局状态留给别的测试类。 */
    @After
    fun tearDown() {
        NameSortKey.installIdentity()
    }

    // ------------------------------------------------------------ 排序键

    @Test
    fun `拼音的声调符号被去掉`() {
        assertEquals("zhoujielun", NameSortKey.build("周杰伦", fake))
        assertEquals("adu", NameSortKey.build("阿杜", fake))
    }

    @Test
    fun `转小写并去掉内部空白`() {
        assertEquals("adele", NameSortKey.build("Adele", { it }))
        assertEquals("9lives", NameSortKey.build("9 Lives", { it }))
        assertEquals("ai", NameSortKey.build("あい", fake))
    }

    @Test
    fun `假名转成罗马字`() {
        assertEquals("katakana", NameSortKey.build("カタカナ", fake))
    }

    @Test
    fun `两端空白被忽略`() {
        assertEquals("abc", NameSortKey.build("  abc  ", { it }))
    }

    @Test
    fun `空名字得到空键`() {
        assertEquals("", NameSortKey.build("", { it }))
        assertEquals("", NameSortKey.build("   ", { it }))
    }

    @Test
    fun `罗马化抛异常时退回原文而不是崩掉`() {
        val boom: (String) -> String = { throw IllegalStateException("boom") }
        assertEquals("abc", NameSortKey.build("ABC", boom))
    }

    @Test
    fun `变音符号可以直接去掉`() {
        assertEquals("cafe", NameSortKey.stripDiacritics("café"))
        assertEquals("Zou", NameSortKey.stripDiacritics("Zōu"))
    }

    // -------------------------------------------------------- 数字的处理

    @Test
    fun `数字排在字母之前`() {
        assertTrue(NameSortKey.build("123", { it }) < NameSortKey.build("A", { it }))
        assertTrue(NameSortKey.build("0", { it }) < NameSortKey.build("a", { it }))
        assertTrue(NameSortKey.build("9 Lives", { it }) < NameSortKey.build("Adele", { it }))
    }

    @Test
    fun `数字之间按 0 到 9 的顺序`() {
        val sorted = listOf("9", "123", "007", "0").sortedWith(NameSortKey.comparator)
        assertEquals(listOf("0", "007", "123", "9"), sorted)
    }

    // ------------------------------------------------------ 拉丁字母排序

    @Test
    fun `拉丁按 A 到 Z 升序`() {
        val sorted = listOf("Zanzibar", "banana", "Apple", "cherry").sortedWith(NameSortKey.comparator)
        assertEquals(listOf("Apple", "banana", "cherry", "Zanzibar"), sorted)
    }

    @Test
    fun `大小写不影响排序位置`() {
        assertEquals(0, NameSortKey.compare("ABC", "abc"))
    }

    // ------------------------------------------- 注入罗马化之后的整体排序

    @Test
    fun `汉字按拼音与拉丁混排`() {
        NameSortKey.install(fake)
        val sorted = listOf("周杰伦", "Adele", "阿杜").sortedWith(NameSortKey.comparator)
        // adele < adu < zhoujielun
        assertEquals(listOf("Adele", "阿杜", "周杰伦"), sorted)
    }

    @Test
    fun `假名按罗马音落到对应字母位置`() {
        NameSortKey.install(fake)
        val sorted = listOf("あい", "カタカナ", "Zebra", "Adele").sortedWith(NameSortKey.comparator)
        // adele < ai < katakana < zebra
        assertEquals(listOf("Adele", "あい", "カタカナ", "Zebra"), sorted)
    }

    @Test
    fun `数字 拉丁 汉字 三者顺序正确`() {
        NameSortKey.install(fake)
        val sorted = listOf("周杰伦", "007", "Adele", "阿杜", "9 Lives")
            .sortedWith(NameSortKey.comparator)
        assertEquals(listOf("007", "9 Lives", "Adele", "阿杜", "周杰伦"), sorted)
    }
}

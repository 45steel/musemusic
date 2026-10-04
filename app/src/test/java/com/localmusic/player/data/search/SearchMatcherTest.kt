package com.localmusic.player.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    // ------------------------------------------------------ 拼音首字母

    @Test
    fun `从带声调的拼音里提取首字母`() {
        // ICU 的 Han-Latin 转写结果形如 "zhōu jié lún"
        assertEquals("zjl", initialsFromPinyin("zhōu jié lún"))
        assertEquals("wf", initialsFromPinyin("wáng fēi"))
        assertEquals("b", initialsFromPinyin("Beyond"))
    }

    @Test
    fun `拼音里的分隔符都能处理`() {
        assertEquals("zjl", initialsFromPinyin("zhou-jie-lun"))
        assertEquals("zjl", initialsFromPinyin("zhou'jie'lun"))
        assertEquals("abc", initialsFromPinyin("a,b/c"))
    }

    @Test
    fun `空串与纯符号返回空`() {
        assertEquals("", initialsFromPinyin(""))
        assertEquals("", initialsFromPinyin("   "))
        assertEquals("", initialsFromPinyin("---"))
        assertEquals("", initialsFromPinyin("123"))
    }

    @Test
    fun `结果统一小写`() {
        assertEquals("zjl", initialsFromPinyin("Zhou Jie Lun"))
    }

    // ---------------------------------------------------------- 匹配

    @Test
    fun `原文包含查询串即命中`() {
        assertTrue(matchesQuery("爱在西元前", "azxyq", "西元"))
        assertTrue(matchesQuery("Beyond", "b", "bey"))
    }

    @Test
    fun `拼音首字母包含查询串即命中`() {
        assertTrue(matchesQuery("周杰伦", "zjl", "zjl"))
        assertTrue(matchesQuery("周杰伦", "zjl", "zj"))
    }

    @Test
    fun `大小写不敏感`() {
        assertTrue(matchesQuery("Beyond", "b", "BEYOND"))
        assertTrue(matchesQuery("Beyond", "b", "bEyOnD"))
        assertTrue(matchesQuery("周杰伦", "ZJL", "zjl"))
    }

    @Test
    fun `不匹配时返回 false`() {
        assertFalse(matchesQuery("周杰伦", "zjl", "ljj"))
        assertFalse(matchesQuery("爱在西元前", "azxyq", "zzz"))
    }

    @Test
    fun `空查询不命中`() {
        assertFalse(matchesQuery("周杰伦", "zjl", ""))
        assertFalse(matchesQuery("周杰伦", "zjl", "   "))
    }

    @Test
    fun `查询串首尾空格会被忽略`() {
        assertTrue(matchesQuery("周杰伦", "zjl", "  zjl  "))
    }

    @Test
    fun `没有拼音时原文匹配仍然工作`() {
        // 低版本系统上拿不到拼音，initials 为空
        assertTrue(matchesQuery("周杰伦", "", "周杰"))
        assertFalse(matchesQuery("周杰伦", "", "zjl"))
    }
}

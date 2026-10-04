package com.localmusic.player.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class YearParserTest {

    @Test
    fun `纯四位年份`() {
        assertEquals(2001, parseYear("2001"))
        assertEquals(1993, parseYear("1993"))
        assertEquals(2016, parseYear("2016"))
    }

    @Test
    fun `带分隔符的日期取年份部分`() {
        assertEquals(2001, parseYear("2001-05-01"))
        assertEquals(2001, parseYear("2001/05/01"))
        assertEquals(2001, parseYear("2001.05.01"))
        assertEquals(2001, parseYear("2001-05-01T00:00:00Z"))
    }

    @Test
    fun `年份夹在文字里也能取出来`() {
        assertEquals(2001, parseYear("(c)2001 Sony Music"))
        assertEquals(2003, parseYear("发行年份 2003 年"))
    }

    @Test
    fun `紧凑的八位日期`() {
        assertEquals(2001, parseYear("20010501"))
    }

    @Test
    fun `无效输入返回零`() {
        assertEquals(0, parseYear(null))
        assertEquals(0, parseYear(""))
        assertEquals(0, parseYear("   "))
        assertEquals(0, parseYear("abcd"))
        assertEquals(0, parseYear("0000"))
        assertEquals(0, parseYear("19"))
        assertEquals(0, parseYear("-"))
    }

    @Test
    fun `超出合理范围的数字不当作年份`() {
        assertEquals(0, parseYear("0001"))
        assertEquals(0, parseYear("3000"))
        assertEquals(0, parseYear("9999"))
    }

    @Test
    fun `不会把长数字串切出错误年份`() {
        // 12345678 既不是纯 8 位日期（首位不是 1/2 开头的合法年份），也不该乱切
        assertEquals(0, parseYear("123456789"))
    }

    @Test
    fun `前后有空格也能解析`() {
        assertEquals(1998, parseYear("  1998  "))
    }
}

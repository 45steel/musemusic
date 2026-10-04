package com.musemusic45.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `纯英文与数字不被编码`() {
        assertEquals("Beyond", percentEncode("Beyond"))
        assertEquals("abcXYZ0123", percentEncode("abcXYZ0123"))
    }

    @Test
    fun `unreserved 符号不被编码`() {
        assertEquals("-_.~", percentEncode("-_.~"))
    }

    @Test
    fun `空格编码成百分号二十`() {
        assertEquals("a%20b", percentEncode("a b"))
    }

    @Test
    fun `斜杠被编码 避免破坏路径分段`() {
        assertEquals("AC%2FDC", percentEncode("AC/DC"))
    }

    @Test
    fun `中文按 UTF-8 逐字节编码`() {
        assertEquals("%E5%91%A8%E6%9D%B0%E4%BC%A6", percentEncode("周杰伦"))
    }

    @Test
    fun `空串编码后仍是空串`() {
        assertEquals("", percentEncode(""))
    }

    @Test
    fun `专辑详情路由格式正确`() {
        assertEquals("album/101", Routes.albumDetail(101L))
        assertEquals("album/0", Routes.albumDetail(0L))
    }

    @Test
    fun `歌手详情路由会把中文歌手名编码`() {
        assertEquals("artist/%E5%91%A8%E6%9D%B0%E4%BC%A6", Routes.artistDetail("周杰伦"))
        assertEquals("artist/Beyond", Routes.artistDetail("Beyond"))
    }

    @Test
    fun `带斜杠的歌手名不会破坏路由层级`() {
        val route = Routes.artistDetail("AC/DC")
        assertEquals("artist/AC%2FDC", route)
        // 去掉前缀后不应该再出现裸斜杠
        assertEquals(-1, route.removePrefix("artist/").indexOf('/'))
    }
}

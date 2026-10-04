package com.musemusic45.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistNamesTest {

    // ------------------------------------------------------------ 归一化

    @Test
    fun `全角括号内容被忽略`() {
        assertEquals("某某", ArtistNames.normalize("某某（xxx）"))
        assertEquals("周杰伦", ArtistNames.normalize("周杰伦（Jay Chou）"))
    }

    @Test
    fun `半角括号内容被忽略`() {
        assertEquals("某某", ArtistNames.normalize("某某(xxx)"))
        assertEquals("Adele", ArtistNames.normalize("Adele (UK)"))
    }

    @Test
    fun `括号在中间时两侧内容都保留`() {
        assertEquals("AB", ArtistNames.normalize("A（x）B"))
    }

    @Test
    fun `没有括号时原样返回`() {
        assertEquals("周杰伦", ArtistNames.normalize("周杰伦"))
        assertEquals("Earth, Wind & Fire", ArtistNames.normalize("Earth, Wind & Fire"))
    }

    @Test
    fun `两端空白被去掉`() {
        assertEquals("周杰伦", ArtistNames.normalize("  周杰伦  "))
    }

    @Test
    fun `括号没闭合时从左括号截断`() {
        assertEquals("某某", ArtistNames.normalize("某某（xxx"))
        assertEquals("某某", ArtistNames.normalize("某某(xxx"))
    }

    @Test
    fun `整串只有括号时退回原串 不产生空名字`() {
        assertEquals("（xxx）", ArtistNames.normalize("（xxx）"))
    }

    @Test
    fun `空串仍然返回空串`() {
        assertEquals("", ArtistNames.normalize(""))
        assertEquals("", ArtistNames.normalize("   "))
    }

    // -------------------------------------------------------------- 拆分

    @Test
    fun `顿号分隔的多个歌手被拆开`() {
        assertEquals(listOf("周杰伦", "费玉清"), ArtistNames.split("周杰伦、费玉清"))
    }

    @Test
    fun `全角分号分隔`() {
        assertEquals(listOf("A", "B"), ArtistNames.split("A；B"))
    }

    @Test
    fun `斜杠分隔`() {
        assertEquals(listOf("A", "B"), ArtistNames.split("A/B"))
        assertEquals(listOf("A", "B"), ArtistNames.split("A／B"))
    }

    @Test
    fun `半角分号也支持`() {
        assertEquals(listOf("A", "B"), ArtistNames.split("A;B"))
    }

    @Test
    fun `逗号不当作分隔符`() {
        // 逗号在乐队名里是合法字符，不能拆
        assertEquals(listOf("Earth, Wind & Fire"), ArtistNames.split("Earth, Wind & Fire"))
    }

    @Test
    fun `拆分时每项都去括号`() {
        assertEquals(listOf("A", "B"), ArtistNames.split("A（x）、B（y）"))
        assertEquals(listOf("周杰伦", "费玉清"), ArtistNames.split("周杰伦（Jay）、费玉清（Fei）"))
    }

    @Test
    fun `重复的歌手只保留一个`() {
        assertEquals(listOf("A"), ArtistNames.split("A、A、A"))
    }

    @Test
    fun `多余空白和空项被丢掉`() {
        assertEquals(listOf("A", "B"), ArtistNames.split("  A 、 、 B  "))
        assertEquals(listOf("A", "B"), ArtistNames.split("A//B"))
    }

    @Test
    fun `多个分隔符混用`() {
        assertEquals(listOf("A", "B", "C"), ArtistNames.split("A、B；C"))
        assertEquals(listOf("A", "B", "C"), ArtistNames.split("A/B;C"))
    }

    @Test
    fun `单个歌手原样返回`() {
        assertEquals(listOf("蔡依林"), ArtistNames.split("蔡依林"))
    }

    @Test
    fun `空串返回未知歌手`() {
        assertEquals(listOf(Song.UNKNOWN_ARTIST), ArtistNames.split(""))
        assertEquals(listOf(Song.UNKNOWN_ARTIST), ArtistNames.split("   "))
    }

    @Test
    fun `只有分隔符时返回未知歌手`() {
        assertEquals(listOf(Song.UNKNOWN_ARTIST), ArtistNames.split("、、"))
        assertEquals(listOf(Song.UNKNOWN_ARTIST), ArtistNames.split("/"))
    }

    @Test
    fun `未知歌手常量本身正常通过`() {
        assertEquals(listOf(Song.UNKNOWN_ARTIST), ArtistNames.split(Song.UNKNOWN_ARTIST))
    }

    @Test
    fun `多个歌手去括号后重名会合并`() {
        assertEquals(listOf("A"), ArtistNames.split("A（1）、A（2）"))
    }
}

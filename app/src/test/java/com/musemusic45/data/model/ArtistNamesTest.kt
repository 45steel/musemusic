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

    // ------------------------------------------------ 第四批：可配置

    @Test
    fun `关掉拆分时整串算一位歌手`() {
        val config = ArtistParsingConfig(splitMultiArtist = false)
        assertEquals(listOf("周杰伦、费玉清"), config.names("周杰伦、费玉清"))
    }

    @Test
    fun `关掉拆分后括号仍按开关处理`() {
        val config = ArtistParsingConfig(splitMultiArtist = false)
        assertEquals(listOf("某某"), config.names("某某（xxx）"))
    }

    @Test
    fun `关掉括号归一化时保留括号`() {
        val config = ArtistParsingConfig(ignoreParentheses = false)
        assertEquals(listOf("某某（xxx）"), config.names("某某（xxx）"))
        assertEquals("某某（xxx）", ArtistNames.normalize("某某（xxx）", ignoreParentheses = false))
    }

    @Test
    fun `关掉括号归一化后括号里的分隔符照样拆`() {
        val config = ArtistParsingConfig(ignoreParentheses = false)
        assertEquals(listOf("A（1）", "B（2）"), config.names("A（1）、B（2）"))
    }

    @Test
    fun `额外添加分隔符生效 且默认的仍然生效`() {
        val config = ArtistParsingConfig(extraSeparators = "&")
        assertEquals(listOf("A", "B"), config.names("A&B"))
        // 默认的顿号没有被顶掉
        assertEquals(listOf("A", "B"), config.names("A、B"))
    }

    @Test
    fun `可以追加多个分隔符`() {
        val config = ArtistParsingConfig(extraSeparators = "+|")
        assertEquals(listOf("A", "B", "C"), config.names("A+B|C"))
        assertEquals(listOf("A", "B"), config.names("A、B"))
    }

    @Test
    fun `不添加额外分隔符时只有默认那些`() {
        val config = ArtistParsingConfig()
        assertEquals(ArtistParsingConfig.DEFAULT_SEPARATORS, config.separators)
        assertEquals(listOf("A&B"), config.names("A&B"))
    }

    @Test
    fun `额外分隔符里的正则特殊字符按字面处理`() {
        val config = ArtistParsingConfig(extraSeparators = ".*")
        assertEquals(listOf("A", "B"), config.names("A.B"))
        assertEquals(listOf("A", "B"), config.names("A*B"))
    }

    @Test
    fun `合并分隔符会去重并保序`() {
        assertEquals(
            "、；;/／&",
            ArtistParsingConfig.mergeSeparators("、；;/／", "&"),
        )
        // 重复添加同一个字符不会堆叠
        assertEquals(
            "、；;/／&",
            ArtistParsingConfig.mergeSeparators("、；;/／", "&&"),
        )
        // 与默认重复的字符也不重复
        assertEquals(
            "、；;/／",
            ArtistParsingConfig.mergeSeparators("、；;/／", "、"),
        )
    }

    @Test
    fun `识别出与默认重复的额外分隔符`() {
        assertEquals("、", ArtistParsingConfig.redundantSeparators("&、"))
        assertEquals("", ArtistParsingConfig.redundantSeparators("&"))
    }

    @Test
    fun `配置全部关掉时歌手字段原样保留`() {
        val config = ArtistParsingConfig(
            splitMultiArtist = false,
            ignoreParentheses = false,
        )
        assertEquals(listOf("某某（xxx）、A"), config.names("某某（xxx）、A"))
    }

    @Test
    fun `默认配置与不带参数的行为一致`() {
        val config = ArtistParsingConfig()
        assertEquals(ArtistNames.split("周杰伦、费玉清"), config.names("周杰伦、费玉清"))
        assertEquals(
            ArtistParsingConfig.DEFAULT_SEPARATORS,
            config.separators,
        )
    }
}

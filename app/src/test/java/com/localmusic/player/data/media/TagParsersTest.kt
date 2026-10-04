package com.localmusic.player.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class TagParsersTest {

    // ---------------------------------------------------------- 构造辅助

    private fun le32(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun be24(value: Int) = byteArrayOf(
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun vorbisComment(comments: List<String>): ByteArray {
        val vendor = "test".toByteArray()
        val out = ByteArrayOutputStream()
        out.write(le32(vendor.size))
        out.write(vendor)
        out.write(le32(comments.size))
        for (comment in comments) {
            val bytes = comment.toByteArray()
            out.write(le32(bytes.size))
            out.write(bytes)
        }
        return out.toByteArray()
    }

    private fun flacFile(comments: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray())
        // STREAMINFO（34 字节，非最后一块）
        out.write(byteArrayOf(0x00))
        out.write(be24(34))
        out.write(ByteArray(34))
        // VORBIS_COMMENT（最后一块）
        val payload = vorbisComment(comments)
        out.write(byteArrayOf(0x84.toByte()))
        out.write(be24(payload.size))
        out.write(payload)
        return out.toByteArray()
    }

    private fun id3v2(version: Int, frames: List<Pair<String, String>>, encoding: Int = 0): ByteArray {
        val body = ByteArrayOutputStream()
        for ((id, text) in frames) {
            val encoded = when (encoding) {
                3 -> text.toByteArray(Charsets.UTF_8)
                else -> text.toByteArray(Charsets.ISO_8859_1)
            }
            val frameBody = byteArrayOf(encoding.toByte()) + encoded
            body.write(id.toByteArray(Charsets.ISO_8859_1))
            if (version == 2) {
                body.write(be24(frameBody.size))
            } else {
                body.write(byteArrayOf(
                    ((frameBody.size shr 24) and 0xFF).toByte(),
                    ((frameBody.size shr 16) and 0xFF).toByte(),
                    ((frameBody.size shr 8) and 0xFF).toByte(),
                    (frameBody.size and 0xFF).toByte(),
                ))
                body.write(byteArrayOf(0, 0))
            }
            body.write(frameBody)
        }
        val bodyBytes = body.toByteArray()
        val size = bodyBytes.size
        val syncSafe = byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte(),
        )
        return byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) +
            syncSafe + bodyBytes
    }

    // ------------------------------------------------------------ 格式判定

    @Test
    fun `格式判定`() {
        assertTrue(TagParsers.isFlac(flacFile(listOf("DATE=2001"))))
        assertFalse(TagParsers.isFlac(byteArrayOf(1, 2, 3, 4)))
        assertTrue(TagParsers.isId3v2(id3v2(3, listOf("TYER" to "2001"))))
        assertFalse(TagParsers.isId3v2(byteArrayOf(1, 2, 3)))
    }

    // ---------------------------------------------------------------- FLAC

    @Test
    fun `解析 FLAC 的 Vorbis 注释`() {
        val bytes = flacFile(listOf("TITLE=晴天", "ARTIST=周杰伦", "DATE=2003"))
        val tags = TagParsers.parseFlacComments(bytes)

        assertEquals("晴天", tags["TITLE"])
        assertEquals("周杰伦", tags["ARTIST"])
        assertEquals("2003", tags["DATE"])
        assertEquals(2003, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `Vorbis 注释的键会统一成大写`() {
        val payload = vorbisComment(listOf("date=1998", "Title=红豆"))
        val tags = TagParsers.parseVorbisComment(payload)

        assertEquals("1998", tags["DATE"])
        assertEquals("红豆", tags["TITLE"])
        assertEquals(1998, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `Vorbis 注释里同一个键出现多次时保留第一个`() {
        val tags = TagParsers.parseVorbisComment(vorbisComment(listOf("DATE=2001", "DATE=1999")))
        assertEquals("2001", tags["DATE"])
    }

    @Test
    fun `没有 DATE 时用 YEAR 兜底`() {
        val tags = TagParsers.parseVorbisComment(vorbisComment(listOf("YEAR=1993", "TITLE=海阔天空")))
        assertEquals(1993, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `FLAC 没有注释块时返回空`() {
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray())
        out.write(byteArrayOf(0x80.toByte()))
        out.write(be24(34))
        out.write(ByteArray(34))

        assertEquals(0, TagParsers.parseFlacComments(out.toByteArray()).size)
        assertEquals(0, TagParsers.yearFromTags(TagParsers.parseFlacComments(out.toByteArray())))
    }

    @Test
    fun `截断的 FLAC 不会抛异常`() {
        val full = flacFile(listOf("DATE=2001"))
        val truncated = full.copyOfRange(0, full.size - 5)
        // 不崩溃即可
        TagParsers.parseFlacComments(truncated)
    }

    @Test
    fun `不存在的键不会被误读`() {
        val tags = TagParsers.parseVorbisComment(vorbisComment(listOf("TITLE=无年份")))
        assertEquals(0, TagParsers.yearFromTags(tags))
    }

    // --------------------------------------------------------------- ID3v2

    @Test
    fun `解析 ID3v2 3 的 TYER`() {
        val tags = TagParsers.parseId3v2(id3v2(3, listOf("TYER" to "2008", "TPE1" to "Metallica")))
        assertEquals("2008", tags["TYER"])
        assertEquals("Metallica", tags["TPE1"])
        assertEquals(2008, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `解析 ID3v2 4 的 TDRC`() {
        val tags = TagParsers.parseId3v2(id3v2(4, listOf("TDRC" to "2003-07-31")))
        assertEquals(2003, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `解析 ID3v2 2 的三字符帧号`() {
        val tags = TagParsers.parseId3v2(id3v2(2, listOf("TYE" to "1993")))
        assertEquals(1993, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `UTF-8 编码的文本帧`() {
        val tags = TagParsers.parseId3v2(id3v2(4, listOf("TDRC" to "2016", "TPE1" to "五月天"), encoding = 3))
        assertEquals("五月天", tags["TPE1"])
        assertEquals(2016, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `没有年份帧时返回零`() {
        val tags = TagParsers.parseId3v2(id3v2(3, listOf("TPE1" to "Beyond")))
        assertEquals(0, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `ID3v2 长度不足时安全返回`() {
        assertEquals(0, TagParsers.parseId3v2(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte())).size)
    }

    @Test
    fun `同步安全整数解析`() {
        // 0x00 0x00 0x02 0x01 → 2*128 + 1 = 257
        val bytes = byteArrayOf(0, 0, 2, 1)
        assertEquals(257, TagParsers.syncSafeInt(bytes, 0))
    }

    // ------------------------------------------------------------ 年份优先级

    @Test
    fun `多个年份键同时存在时按优先级取`() {
        val tags = mapOf("TDRC" to "2003", "DATE" to "2001", "TYER" to "1999")
        // DATE 排在 TDRC 之前
        assertEquals(2001, TagParsers.yearFromTags(tags))
    }

    @Test
    fun `年份键存在但值无效时继续尝试下一个`() {
        val tags = mapOf("DATE" to "不是年份", "YEAR" to "2005")
        assertEquals(2005, TagParsers.yearFromTags(tags))
    }
}

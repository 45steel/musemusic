package com.localmusic.player.data.media

/**
 * 纯字节级音频标签解析，不依赖 Android，便于单元测试。
 *
 * 为什么需要它：实测 MediaStore 的 `year` 列对 FLAC 一律为 NULL，
 * 而 `MediaMetadataRetriever.METADATA_KEY_YEAR` 同样读不到，
 * 所以年份只能自己从文件的标签块里取。
 *
 * 目前支持：
 *  - FLAC 的 Vorbis 注释（DATE / YEAR）
 *  - MP3 的 ID3v2 文本帧（TDRC / TYER / TYE）
 */
object TagParsers {

    private const val BLOCK_VORBIS_COMMENT = 4
    private const val USLT_FRAME_ID = "USLT"
    private const val LYRICS_KEY = "LYRICS"

    /** 单次读取的注释块上限，防止异常文件把内存吃光。 */
    const val MAX_COMMENT_BYTES = 1 shl 20

    fun isFlac(bytes: ByteArray, offset: Int = 0): Boolean =
        bytes.size >= offset + 4 &&
            bytes[offset] == 'f'.code.toByte() &&
            bytes[offset + 1] == 'L'.code.toByte() &&
            bytes[offset + 2] == 'a'.code.toByte() &&
            bytes[offset + 3] == 'C'.code.toByte()

    fun isId3v2(bytes: ByteArray, offset: Int = 0): Boolean =
        bytes.size >= offset + 3 &&
            bytes[offset] == 'I'.code.toByte() &&
            bytes[offset + 1] == 'D'.code.toByte() &&
            bytes[offset + 2] == '3'.code.toByte()

    // ---------------------------------------------------------------- FLAC

    /** 解析 Vorbis 注释块（不含块头的裸 payload）。 */
    fun parseVorbisComment(payload: ByteArray): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var pos = 0

        if (pos + 4 > payload.size) return result
        val vendorLength = readUInt32Le(payload, pos)
        pos += 4 + vendorLength
        if (pos + 4 > payload.size) return result

        val count = readUInt32Le(payload, pos)
        pos += 4

        var index = 0
        while (index < count && pos + 4 <= payload.size) {
            val length = readUInt32Le(payload, pos)
            pos += 4
            if (length < 0 || pos + length > payload.size) break
            val text = String(payload, pos, length, Charsets.UTF_8)
            pos += length

            val separator = text.indexOf('=')
            if (separator > 0) {
                val key = text.substring(0, separator).uppercase()
                val value = text.substring(separator + 1)
                // 同一个键出现多次时保留第一个
                result.putIfAbsent(key, value)
            }
            index++
        }
        return result
    }

    /**
     * 从完整的 FLAC 文件字节里取注释块。文件较大时请改用流式读取
     * （见 [TagReader]），这个重载主要给单元测试用。
     */
    fun parseFlacComments(bytes: ByteArray): Map<String, String> {
        if (!isFlac(bytes)) return emptyMap()
        var pos = 4
        while (pos + 4 <= bytes.size) {
            val header = bytes[pos].toInt() and 0xFF
            val isLast = header and 0x80 != 0
            val type = header and 0x7F
            val length = ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                (bytes[pos + 3].toInt() and 0xFF)
            val start = pos + 4
            val end = start + length
            if (end > bytes.size) break
            if (type == BLOCK_VORBIS_COMMENT) {
                return parseVorbisComment(bytes.copyOfRange(start, end))
            }
            if (isLast) break
            pos = end
        }
        return emptyMap()
    }

    // --------------------------------------------------------------- ID3v2

    /** 读取 ID3v2 头里的同步安全整数（每字节只用低 7 位）。 */
    fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    /**
     * 解析 ID3v2 的标签体（不含 10 字节头）。
     *
     * @param majorVersion ID3v2 的主版本号：2 / 3 / 4
     */
    fun parseId3v2Frames(body: ByteArray, majorVersion: Int): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val idLength = if (majorVersion == 2) 3 else 4
        val sizeLength = if (majorVersion == 2) 3 else 4
        val flagsLength = if (majorVersion == 2) 0 else 2
        val headerLength = idLength + sizeLength + flagsLength

        var pos = 0
        while (pos + headerLength <= body.size) {
            // 遇到填充的 0 就结束
            if (body[pos] == 0.toByte()) break

            val id = String(body, pos, idLength, Charsets.ISO_8859_1)
            if (id.any { !it.isLetterOrDigit() }) break

            val size = when (majorVersion) {
                2 -> readUInt24Be(body, pos + idLength)
                4 -> syncSafeInt(body, pos + idLength)
                // ID3v2.3 的帧长度是普通的 4 字节大端整数
                else -> readUInt32Be(body, pos + idLength)
            }
            if (size <= 0 || pos + headerLength + size > body.size) break

            val dataStart = pos + headerLength
            if (id == USLT_FRAME_ID) {
                // 内嵌歌词帧，取出后统一存到 LYRICS 键下
                val lyrics = decodeUslt(body, dataStart, size)
                if (lyrics.isNotEmpty()) result.putIfAbsent(LYRICS_KEY, lyrics)
            } else if (id.startsWith("T")) {
                val text = decodeTextFrame(body, dataStart, size)
                if (text.isNotEmpty()) result.putIfAbsent(id, text)
            }
            pos = dataStart + size
        }
        return result
    }

    /** 从完整的 MP3 文件字节里取 ID3v2 文本帧。 */
    fun parseId3v2(bytes: ByteArray): Map<String, String> {
        if (!isId3v2(bytes) || bytes.size < 10) return emptyMap()
        val major = bytes[3].toInt() and 0xFF
        val size = syncSafeInt(bytes, 6)
        if (size <= 0 || 10 + size > bytes.size) return emptyMap()
        return parseId3v2Frames(bytes.copyOfRange(10, 10 + size), major)
    }

    // ---------------------------------------------------------------- 通用

    /**
     * 按优先级从标签里找年份。
     *
     * 不同标签规范用的键不一样：Vorbis 用 DATE，ID3v2.3 用 TYER，ID3v2.4 用 TDRC。
     */
    fun yearFromTags(tags: Map<String, String>): Int {
        for (key in YEAR_KEYS) {
            val value = tags[key] ?: continue
            val year = parseYear(value)
            if (year > 0) return year
        }
        return 0
    }

    private val YEAR_KEYS = listOf(
        "DATE", "YEAR", "TDRC", "TYER", "TYE",
        "ORIGINALDATE", "ORIGINALYEAR", "TDOR",
    )

    /**
     * 内嵌歌词可能存在的键。
     * Vorbis 用 LYRICS / UNSYNCEDLYRICS，ID3v2 的 USLT 帧在解析时会归一到 LYRICS。
     */
    private val LYRICS_KEYS = listOf(
        "LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "LYRIC", "SYNCEDLYRICS",
    )

    /** 从标签里取内嵌歌词，取不到返回 null。 */
    fun lyricsFromTags(tags: Map<String, String>): String? {
        for (key in LYRICS_KEYS) {
            val value = tags[key]
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    // ------------------------------------------------------------- 内部工具

    /**
     * 解码 ID3v2 的 USLT（非同步歌词）帧。
     *
     * 结构：编码(1) + 语言(3) + 内容描述符(以空字符结尾) + 歌词正文
     */
    private fun decodeUslt(body: ByteArray, start: Int, length: Int): String {
        if (length <= 4) return ""
        val encoding = body[start].toInt() and 0xFF
        var pos = start + 4 // 跳过编码字节和 3 字节语言码
        val end = start + length
        if (pos >= end) return ""

        val wide = encoding == 1 || encoding == 2
        if (wide) {
            while (pos + 1 < end) {
                if (body[pos] == 0.toByte() && body[pos + 1] == 0.toByte()) {
                    pos += 2
                    break
                }
                pos += 2
            }
        } else {
            while (pos < end && body[pos] != 0.toByte()) pos++
            pos += 1
        }
        if (pos >= end) return ""

        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return String(body, pos, end - pos, charset).trimEnd('\u0000').trim()
    }

    private fun decodeTextFrame(body: ByteArray, start: Int, length: Int): String {
        if (length <= 1) return ""
        val encoding = body[start].toInt() and 0xFF
        val raw = body.copyOfRange(start + 1, start + length)
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return String(raw, charset).trimEnd('\u0000', ' ', '\n', '\r')
    }

    private fun readUInt32Le(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return -1
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readUInt24Be(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            (bytes[offset + 2].toInt() and 0xFF)
    }

    private fun readUInt32Be(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }
}

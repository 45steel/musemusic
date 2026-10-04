package com.localmusic.player.data.media

import android.media.MediaMetadataRetriever
import java.io.RandomAccessFile

/**
 * 读文件里的音频标签。
 *
 * 用于两类 MediaStore 不提供的信息：
 *  - **年份**：实测 MediaStore 的 `year` 列和 `MediaMetadataRetriever` 对 FLAC 都读不到
 *  - **内嵌歌词**：`MediaMetadataRetriever` 没有歌词字段
 *
 * 为了不拖慢扫描，只做定点读取（跳过封面等大块），不整文件读入。
 */
class TagReader {

    /** 读一首歌的发布年份，读不到返回 0。 */
    fun readYear(path: String): Int {
        if (path.isBlank()) return 0

        val fromTags = runCatching { readTags(path) }
            .onFailure { android.util.Log.w(TAG, "解析标签失败 $path: ${it.message}") }
            .getOrDefault(emptyMap())
        val year = TagParsers.yearFromTags(fromTags)
        if (year > 0) return year

        // 兜底：m4a / aac 等非 FLAC、非 ID3 的格式交给系统解析
        return runCatching { readYearViaRetriever(path) }.getOrDefault(0)
    }

    /** 读内嵌歌词，读不到返回 null。 */
    fun readEmbeddedLyrics(path: String): String? {
        if (path.isBlank()) return null
        val tags = runCatching { readTags(path) }.getOrDefault(emptyMap())
        return TagParsers.lyricsFromTags(tags)
    }

    // ------------------------------------------------------------ 内部实现

    /** 按文件格式读取标签键值对。 */
    private fun readTags(path: String): Map<String, String> {
        RandomAccessFile(path, "r").use { file ->
            val header = ByteArray(10)
            if (file.read(header) < 4) return emptyMap()
            return when {
                TagParsers.isFlac(header) -> readFlacTags(file)
                TagParsers.isId3v2(header) -> readId3Tags(file, header)
                else -> emptyMap()
            }
        }
    }

    /** 逐块跳过 FLAC 元数据，只读 VORBIS_COMMENT。 */
    private fun readFlacTags(file: RandomAccessFile): Map<String, String> {
        file.seek(4)
        val blockHeader = ByteArray(4)
        var guard = 0

        while (guard++ < MAX_FLAC_BLOCKS) {
            if (file.read(blockHeader) < 4) return emptyMap()
            val header = blockHeader[0].toInt() and 0xFF
            val isLast = header and 0x80 != 0
            val type = header and 0x7F
            val length = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                ((blockHeader[2].toInt() and 0xFF) shl 8) or
                (blockHeader[3].toInt() and 0xFF)

            if (type == BLOCK_VORBIS_COMMENT) {
                if (length <= 0 || length > TagParsers.MAX_COMMENT_BYTES) return emptyMap()
                val payload = ByteArray(length)
                if (file.read(payload) < length) return emptyMap()
                return TagParsers.parseVorbisComment(payload)
            }

            if (isLast) return emptyMap()
            file.seek(file.filePointer + length)
        }
        return emptyMap()
    }

    /** 读 ID3v2 标签体并解析帧。 */
    private fun readId3Tags(file: RandomAccessFile, header: ByteArray): Map<String, String> {
        val major = header[3].toInt() and 0xFF
        val size = TagParsers.syncSafeInt(header, 6)
        if (size <= 0 || size > TagParsers.MAX_COMMENT_BYTES) return emptyMap()

        file.seek(10)
        val body = ByteArray(size)
        var read = 0
        while (read < size) {
            val count = file.read(body, read, size - read)
            if (count <= 0) break
            read += count
        }
        if (read < size) return emptyMap()

        return TagParsers.parseId3v2Frames(body, major)
    }

    private fun readYearViaRetriever(path: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            parseYear(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR))
        } catch (error: Exception) {
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        const val TAG = MediaStoreScanner.TAG
        const val BLOCK_VORBIS_COMMENT = 4
        const val MAX_FLAC_BLOCKS = 64
    }
}

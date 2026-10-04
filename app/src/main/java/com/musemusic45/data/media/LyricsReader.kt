package com.musemusic45.data.media

import com.musemusic45.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 歌词的来源与状态。
 */
sealed interface LyricsState {
    /** 没有歌词。 */
    object None : LyricsState

    /** 带时间轴，可以逐行滚动。 */
    data class Synced(val document: LrcDocument) : LyricsState

    /** 只有纯文本，没有时间轴，整页静态显示。 */
    data class Plain(val text: String) : LyricsState
}

/**
 * 读取本地歌词。
 *
 * 优先级（对照 PRD）：
 *  1. 歌曲**同目录下的同名 `.lrc` 文件**
 *  2. 文件**内嵌歌词**（FLAC 的 LYRICS 注释 / MP3 的 USLT 帧）
 *
 * 全程不联网。
 */
class LyricsReader(private val tagReader: TagReader) {

    private val cache = HashMap<Long, LyricsState>()

    suspend fun load(song: Song?): LyricsState {
        if (song == null) return LyricsState.None
        cache[song.id]?.let { return it }

        val state = withContext(Dispatchers.IO) { loadFromSources(song) }
        cache[song.id] = state
        return state
    }

    fun invalidate() = cache.clear()

    private fun loadFromSources(song: Song): LyricsState {
        // 1. 同目录同名 .lrc
        readSiblingLrc(song.path)?.let { return it }

        // 2. 内嵌歌词
        val embedded = runCatching { tagReader.readEmbeddedLyrics(song.path) }.getOrNull()
        if (!embedded.isNullOrBlank()) {
            return toState(embedded)
        }

        return LyricsState.None
    }

    private fun readSiblingLrc(path: String): LyricsState? {
        if (path.isBlank()) return null
        val dot = path.lastIndexOf('.')
        if (dot <= 0) return null

        val lrcFile = File(path.substring(0, dot) + ".lrc")
        if (!lrcFile.isFile || !lrcFile.canRead()) return null

        val content = runCatching { lrcFile.readText() }.getOrNull()
        if (content.isNullOrBlank()) return null

        return toState(content)
    }

    /** 内容带时间戳就当同步歌词，否则当纯文本。 */
    private fun toState(content: String): LyricsState {
        val lines = parseLrc(content)
        return if (lines.isNotEmpty()) {
            LyricsState.Synced(LrcDocument(lines))
        } else {
            LyricsState.Plain(content.trim())
        }
    }
}

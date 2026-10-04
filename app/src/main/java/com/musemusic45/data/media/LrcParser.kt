package com.musemusic45.data.media

/**
 * 一行带时间轴的歌词。
 */
data class LrcLine(
    val timeMs: Long,
    val text: String,
)

/**
 * 解析后的歌词文档。
 */
data class LrcDocument(val lines: List<LrcLine>) {

    val isEmpty: Boolean get() = lines.isEmpty()

    /**
     * 找出某个播放位置应当高亮的行下标。
     *
     * 返回最后一个 `timeMs <= positionMs` 的行；位置在最前面时返回 0，
     * 没有歌词时返回 -1。用二分查找，播放时每帧调用也不会卡。
     */
    fun indexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (positionMs < lines[0].timeMs) return 0

        var low = 0
        var high = lines.size - 1
        var answer = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                answer = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return answer
    }

    companion object {
        val EMPTY = LrcDocument(emptyList())
    }
}

/** 形如 `[mm:ss]`、`[mm:ss.xx]`、`[mm:ss.xxx]` 的时间戳。 */
private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** 形如 `[ti:xxx]`、`[offset:+500]` 的元数据标签。 */
private val META_TAG = Regex("""^\[([a-zA-Z#]+):(.*)]$""")

/**
 * 解析 LRC 歌词。
 *
 * 支持：
 *  - 标准时间戳 `[00:12.34]`
 *  - 一行多个时间戳 `[00:01.00][00:05.00]同一句`（会展开成两条）
 *  - 元数据标签 `[ti:]` `[ar:]` `[al:]` `[by:]` 等（忽略，不当作歌词）
 *  - `[offset:+300]` 全局时间偏移（毫秒，正数表示歌词提前）
 *  - 只有纯文本没有时间轴（返回空列表，由调用方按纯文本处理）
 *
 * 纯函数，便于单元测试。
 */
fun parseLrc(content: String): List<LrcLine> {
    if (content.isBlank()) return emptyList()

    val rawLines = ArrayList<LrcLine>()
    var offsetMs = 0L

    for (rawLine in content.lineSequence()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        // 先看是不是元数据
        META_TAG.find(line)?.let { meta ->
            val key = meta.groupValues[1].lowercase()
            if (key == "offset") {
                offsetMs = meta.groupValues[2].trim().toLongOrNull() ?: 0L
            }
            // 元数据行里也可能同时带时间戳，继续往下走
            if (!TIME_TAG.containsMatchIn(line)) return@let
        }

        val stamps = TIME_TAG.findAll(line).toList()
        if (stamps.isEmpty()) continue

        // 时间戳之后的剩余文本就是歌词
        val lastStampEnd = stamps.last().range.last + 1
        val text = line.substring(lastStampEnd).trim()

        for (stamp in stamps) {
            val minutes = stamp.groupValues[1].toLongOrNull() ?: continue
            val seconds = stamp.groupValues[2].toLongOrNull() ?: continue
            val fractionText = stamp.groupValues[3]

            val fractionMs = when (fractionText.length) {
                0 -> 0L
                1 -> fractionText.toLong() * 100L          // .5 → 500ms
                2 -> fractionText.toLong() * 10L           // .50 → 500ms
                else -> fractionText.take(3).toLong()      // .500 → 500ms
            }

            val timeMs = minutes * 60_000L + seconds * 1_000L + fractionMs - offsetMs
            rawLines += LrcLine(timeMs.coerceAtLeast(0L), text)
        }
    }

    return rawLines.sortedBy { it.timeMs }
}

/**
 * 内容看起来是不是 LRC（含时间戳）。
 */
fun looksLikeLrc(content: String): Boolean = TIME_TAG.containsMatchIn(content)

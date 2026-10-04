package com.musemusic45.data.media

/**
 * 逐字歌词里的一个片段。
 *
 * 增强型 LRC 用行内标签标记每个字/词的起始时间：
 * `[00:12.00]<00:12.00>Hello <00:12.50>world`
 */
data class LrcWord(
    val timeMs: Long,
    val text: String,
)

/**
 * 一行带时间轴的歌词。
 *
 * @param translation 译文。双语歌词里译文行**不带时间戳**，紧跟在原文行后面，
 *   解析时挂到前一行上。
 * @param words 逐字时间轴。空表示这行没有逐字信息，按整行高亮。
 */
data class LrcLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LrcWord> = emptyList(),
) {
    /** 这行是否有逐字时间轴。 */
    val hasWords: Boolean get() = words.isNotEmpty()

    /** 是否有译文。 */
    val hasTranslation: Boolean get() = !translation.isNullOrEmpty()

    /**
     * 在 [positionMs] 这一刻，这行已经唱到第几个字。
     *
     * 用于逐字歌词的进度高亮：
     *  - 没有逐字信息时，返回整行长度（整行一起亮）
     *  - 有逐字信息时，把已开始的片段全部算上，并在**下一个片段内做线性插值**，
     *    这样高亮会平滑推进而不是跳字
     *
     * 纯函数，便于单元测试。
     */
    fun sungLength(positionMs: Long): Int {
        // 没有逐字信息：还没唱到这行时是 0，唱到了就整行一起亮
        if (words.isEmpty()) {
            return if (positionMs < timeMs) 0 else text.length
        }

        var sung = 0
        for (word in words) {
            if (positionMs >= word.timeMs) sung += word.text.length else break
        }

        val next = words.firstOrNull { it.timeMs > positionMs }
        val prev = words.lastOrNull { it.timeMs <= positionMs }
        if (next != null && prev != null) {
            val span = (next.timeMs - prev.timeMs).coerceAtLeast(1L)
            val progress = ((positionMs - prev.timeMs) * next.text.length / span).toInt()
            sung += progress.coerceIn(0, next.text.length)
        }

        return sung.coerceIn(0, text.length)
    }
}

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

/** 形如 `<mm:ss.xx>` 的逐字时间标签。 */
private val WORD_TAG = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")

/** 形如 `[ti:xxx]`、`[offset:+500]` 的元数据标签。 */
private val META_TAG = Regex("""^\[([a-zA-Z#]+):(.*)]$""")

/**
 * 解析 LRC 歌词。
 *
 * 支持：
 *  - 标准时间戳 `[00:12.34]`
 *  - 一行多个时间戳 `[00:01.00][00:05.00]同一句`（会展开成多条）
 *  - **双语歌词**：译文行不带时间戳，紧跟原文行，会挂到前一行上
 *  - **逐字歌词**：行内 `<00:12.50>` 标签
 *  - 元数据标签 `[ti:]` `[ar:]` `[al:]` `[by:]` 等（忽略，不当作歌词）
 *  - `[offset:+300]` 全局时间偏移（毫秒，正数表示歌词提前）
 *  - 只有纯文本没有时间轴（返回空列表，由调用方按纯文本处理）
 *
 * 纯函数，便于单元测试。
 */
fun parseLrc(content: String): List<LrcLine> {
    if (content.isBlank()) return emptyList()

    val entries = ArrayList<LrcLine>()
    var offsetMs = 0L

    // 上一行带时间戳的歌词共创建了几条，译文要挂到这几条上
    var lastTimedRange: IntRange? = null

    for (rawLine in content.lineSequence()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        // 元数据行：读 offset，其余忽略；且不影响译文的归属
        val meta = META_TAG.find(line)
        if (meta != null) {
            if (meta.groupValues[1].lowercase() == "offset") {
                offsetMs = meta.groupValues[2].trim().toLongOrNull() ?: 0L
            }
            if (!TIME_TAG.containsMatchIn(line)) continue
        }

        val stamps = TIME_TAG.findAll(line).toList()

        if (stamps.isEmpty()) {
            // 没有时间戳 → 当作上一行的译文
            val range = lastTimedRange ?: continue
            for (index in range) {
                if (entries[index].translation == null) {
                    entries[index] = entries[index].copy(translation = line)
                }
            }
            continue
        }

        val body = line.substring(stamps.last().range.last + 1).trim()
        val (display, words) = parseWords(body, offsetMs)

        val start = entries.size
        for (stamp in stamps) {
            val timeMs = stampTimeMs(stamp) ?: continue
            entries += LrcLine(
                timeMs = (timeMs - offsetMs).coerceAtLeast(0L),
                text = display,
                words = words,
            )
        }
        lastTimedRange = if (entries.size > start) start until entries.size else null
    }

    return entries.sortedBy { it.timeMs }
}

/**
 * 把行内 `<mm:ss.xx>` 标签拆出来。
 *
 * 返回「用于显示的纯文本」与「逐字时间轴」。没有标签时返回 (原文, 空列表)。
 * 第一个标签之前的文字会并到第一个片段里，保证拼起来等于显示文本。
 */
private fun parseWords(body: String, offsetMs: Long): Pair<String, List<LrcWord>> {
    val tags = WORD_TAG.findAll(body).toList()
    if (tags.isEmpty()) return body to emptyList()

    val words = ArrayList<LrcWord>(tags.size)
    val display = StringBuilder()

    // 第一个标签之前的残留文字
    val leading = body.substring(0, tags.first().range.first)

    tags.forEachIndexed { index, tag ->
        val timeMs = stampTimeMs(tag) ?: return@forEachIndexed
        val segmentStart = tag.range.last + 1
        val segmentEnd = if (index + 1 < tags.size) tags[index + 1].range.first else body.length
        val segment = body.substring(segmentStart, segmentEnd)

        val text = if (index == 0) leading + segment else segment
        if (text.isEmpty()) return@forEachIndexed

        display.append(text)
        words += LrcWord((timeMs - offsetMs).coerceAtLeast(0L), text)
    }

    if (words.isEmpty()) return body to emptyList()
    return display.toString() to words
}

/** 从时间标签里取毫秒数。 */
private fun stampTimeMs(stamp: MatchResult): Long? {
    val minutes = stamp.groupValues[1].toLongOrNull() ?: return null
    val seconds = stamp.groupValues[2].toLongOrNull() ?: return null
    val fractionText = stamp.groupValues[3]

    val fractionMs = when (fractionText.length) {
        0 -> 0L
        1 -> fractionText.toLong() * 100L          // .5 → 500ms
        2 -> fractionText.toLong() * 10L           // .50 → 500ms
        else -> fractionText.take(3).toLong()      // .500 → 500ms
    }
    return minutes * 60_000L + seconds * 1_000L + fractionMs
}

/**
 * 内容看起来是不是 LRC（含时间戳）。
 */
fun looksLikeLrc(content: String): Boolean = TIME_TAG.containsMatchIn(content)

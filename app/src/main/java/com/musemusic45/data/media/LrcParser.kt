package com.musemusic45.data.media

/**
 * 逐字歌词里的一个片段。
 */
data class LrcWord(
    val timeMs: Long,
    val text: String,
)

/**
 * 一行带时间轴的歌词。
 *
 * @param translation 译文。支持三种写法（见 [parseLrc]）。
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
     *  - 没有逐字信息时：还没唱到这行返回 0，唱到了整行一起亮
     *  - 有逐字信息时：把已开始的片段全部算上，并在**下一个片段内做线性插值**，
     *    这样高亮会平滑推进而不是跳字
     *
     * 纯函数，便于单元测试。
     */
    fun sungLength(positionMs: Long): Int {
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

/** 形如 `[mm:ss]`、`[mm:ss.xx]`、`[mm:ss.xxx]` 的时间标签。逐字和整行都用它。 */
private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** 形如 `<mm:ss.xx>` 的逐字标签（增强型 LRC 的另一种写法）。 */
private val ANGLE_TAG = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")

/** 形如 `[ti:xxx]`、`[offset:+500]` 的元数据标签。 */
private val META_TAG = Regex("""^\[([a-zA-Z#]+):(.*)]$""")

/** 细空格：真实歌词用它分隔原文与译文。 */
private const val THIN_SPACE = '\u2009'
private const val HAIR_SPACE = '\u200A'

/**
 * 解析 LRC 歌词。
 *
 * 支持：
 *  - 标准时间戳 `[00:12.34]`
 *  - 一行多个时间戳 `[00:01.00][00:05.00]同一句`（会展开成多条）
 *  - **逐字歌词**：`[00:12.37]予[00:12.35]想[00:12.53]出…`
 *    —— 时间标签夹在字与字之间，第一个是整行时间，其余是**后面那个字**的时间
 *    （增强型 LRC 的 `<00:12.37>` 写法也认）
 *  - **双语歌词**，三种写法都支持：
 *      1. 译文独立一行，时间戳与原文相同
 *      2. 译文独立一行，**不带**时间戳（紧跟在原文后面）
 *      3. 原文与译文在**同一行**，用**细空格 U+2009** 分隔
 *  - `[offset:+300]` 全局时间偏移（毫秒，正数表示歌词提前）
 *  - 只有纯文本没有时间轴（返回空列表，由调用方按纯文本处理）
 *
 * 纯函数，便于单元测试。
 */
fun parseLrc(content: String): List<LrcLine> {
    if (content.isBlank()) return emptyList()

    val entries = ArrayList<LrcLine>()
    var offsetMs = 0L

    // 上一行带时间戳的歌词创建了哪几条，无时间戳的译文要挂到这几条上
    var lastTimedRange: IntRange? = null

    for (rawLine in content.lineSequence()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        // 元数据行：读 offset，其余忽略，且不影响译文归属
        val meta = META_TAG.find(line)
        if (meta != null) {
            if (meta.groupValues[1].lowercase() == "offset") {
                offsetMs = meta.groupValues[2].trim().toLongOrNull() ?: 0L
            }
            if (!TIME_TAG.containsMatchIn(line)) continue
        }

        val stamps = TIME_TAG.findAll(line).toList()

        // ---- 写法 2：没有时间戳，当作上一行的译文 ----
        if (stamps.isEmpty()) {
            val range = lastTimedRange ?: continue
            val text = line.trim()
            for (index in range) {
                if (entries[index].translation == null) {
                    entries[index] = entries[index].copy(translation = text)
                }
            }
            continue
        }

        // 把每两个时间标签之间的文字切出来
        val segments = ArrayList<String>(stamps.size)
        stamps.forEachIndexed { index, stamp ->
            val start = stamp.range.last + 1
            val end = if (index + 1 < stamps.size) stamps[index + 1].range.first else line.length
            segments += line.substring(start, end)
        }

        // 除了最后一段，中间只要有任何一段有字 → 这是逐字行
        val isWordLevel = segments.dropLast(1).any { it.isNotEmpty() }
        val lineTimeMs = stampTimeMs(stamps[0])?.let { (it - offsetMs).coerceAtLeast(0L) }

        // ---- 写法 1：作为上一行的译文 ----
        //
        // 命中两种情形之一：
        //  a) 时间戳与上一行相同（`[t]原文` / `[t]译文` 这种写法）
        //  b) 上一行带逐字信息、且还没有译文
        //
        // (b) 是必需的：真实歌词里出现过译文行时间戳被写错的情况，
        // 例如 Holiday∞Holiday 里 `(Let's go)铃声响起…` 那行的标签是
        // `[00:39.776]`，而它对应的逐字行其实在 `[00:44.711]`。
        val last = entries.lastOrNull()
        val sameTime = lineTimeMs != null && last != null && last.timeMs == lineTimeMs
        val followsWordLine = last != null && last.hasWords && last.translation == null

        if (!isWordLevel && stamps.size == 1 && last != null && last.translation == null &&
            (sameTime || followsWordLine)
        ) {
            val (text, _) = splitThinSpace(segments.last())
            entries[entries.lastIndex] = last.copy(translation = text)
            lastTimedRange = null
            continue
        }

        val startIndex = entries.size

        if (isWordLevel) {
            val time = lineTimeMs ?: continue
            val words = ArrayList<LrcWord>(stamps.size)
            stamps.forEachIndexed { index, stamp ->
                val segment = segments[index]
                val wordTime = stampTimeMs(stamp)?.let { (it - offsetMs).coerceAtLeast(0L) }
                if (segment.isNotEmpty() && wordTime != null) {
                    words += LrcWord(wordTime, segment)
                }
            }
            entries += LrcLine(
                timeMs = time,
                text = segments.joinToString("").trim(),
                words = words,
            )
        } else {
            val raw = segments.last()
            // ---- 写法 3：同一行里用细空格分隔原文与译文 ----
            val (text, translation) = splitThinSpace(raw)
            val angleWords = parseAngleWords(text, offsetMs)
            val display = angleWords.first
            val words = angleWords.second

            for (stamp in stamps) {
                val time = stampTimeMs(stamp)?.let { (it - offsetMs).coerceAtLeast(0L) } ?: continue
                entries += LrcLine(
                    timeMs = time,
                    text = display,
                    translation = translation,
                    words = words,
                )
            }
        }

        lastTimedRange = if (entries.size > startIndex) startIndex until entries.size else null
    }

    return entries.sortedBy { it.timeMs }
}

/**
 * 把「原文＋细空格＋译文」拆开。
 *
 * 只用**细空格（U+2009）/ 发丝空格（U+200A）**当分隔符 —— 普通空格在歌词里
 * 太常见（英文词组、`(It's time to go)` 之类），拿它拆会误伤。
 */
private fun splitThinSpace(raw: String): Pair<String, String?> {
    val index = raw.indexOfFirst { it == THIN_SPACE || it == HAIR_SPACE }
    if (index < 0) return raw.trim() to null

    val original = raw.substring(0, index).trim()
    val translation = raw.substring(index + 1).trim()
    return original to translation.ifEmpty { null }
}

/**
 * 处理增强型 LRC 的 `<mm:ss.xx>` 写法，返回（纯文本, 逐字时间轴）。
 * 没有这种标签时返回 (原文, 空列表)。
 */
private fun parseAngleWords(text: String, offsetMs: Long): Pair<String, List<LrcWord>> {
    val tags = ANGLE_TAG.findAll(text).toList()
    if (tags.isEmpty()) return text to emptyList()

    val display = StringBuilder()
    val words = ArrayList<LrcWord>(tags.size)
    val leading = text.substring(0, tags.first().range.first)

    tags.forEachIndexed { index, tag ->
        val time = stampTimeMs(tag)?.let { (it - offsetMs).coerceAtLeast(0L) } ?: return@forEachIndexed
        val segmentStart = tag.range.last + 1
        val segmentEnd = if (index + 1 < tags.size) tags[index + 1].range.first else text.length
        val segment = text.substring(segmentStart, segmentEnd)
        val piece = if (index == 0) leading + segment else segment
        if (piece.isEmpty()) return@forEachIndexed

        display.append(piece)
        words += LrcWord(time, piece)
    }

    if (words.isEmpty()) return text to emptyList()
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

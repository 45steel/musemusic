package com.musemusic45.data.model

/**
 * 歌手名的归一化与拆分。
 *
 * 媒体库里的歌手字段经常是「周杰伦、费玉清」或「某某（xxx）」这种形式，
 * 直接拿去分组会得到一堆只此一家的伪歌手。这里把它规整成真正的歌手名。
 *
 * 纯函数，不依赖 Android，便于单元测试。
 */
object ArtistNames {

    /**
     * 分隔符：需求里指定的 `、` `；` `/`，外加它们的半角/全角等价形式。
     *
     * **刻意不含逗号** —— 逗号在乐队名里是合法字符（Earth, Wind & Fire），
     * 拿它当分隔符会误拆。
     */
    val SEPARATORS = listOf("、", "；", ";", "/", "／")

    private val FULL_WIDTH_PARENS = Regex("（[^）]*）")
    private val HALF_WIDTH_PARENS = Regex("\\([^)]*\\)")

    /**
     * 去掉括号及其内容：「某某（xxx）」→「某某」。
     *
     * 两条规则：
     *  1. 成对的括号连同内容一起去掉（全角、半角都处理）
     *  2. 万一括号没闭合，从左括号起截断，避免留下「某某（xxx」这种残缺名字
     *
     * 全部去掉后如果什么都不剩（原串本身就只有括号），退回原串 —— 宁可留着括号，
     * 也不能产生空名字。
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        var result = FULL_WIDTH_PARENS.replace(trimmed, "")
        result = HALF_WIDTH_PARENS.replace(result, "")

        val cut = result.indexOfFirst { it == '（' || it == '(' }
        if (cut >= 0) result = result.substring(0, cut)

        result = result.trim()
        return result.ifEmpty { trimmed }
    }

    /**
     * 拆成多个歌手名。
     *
     * 顺序：先整体去括号 → 再按分隔符拆 → 每一项各自归一化 → 去空白 → 去重。
     * 最后什么都不剩时返回 [Song.UNKNOWN_ARTIST]，绝不返回空列表。
     */
    fun split(raw: String): List<String> {
        val cleaned = normalize(raw)
        val parts = cleaned.split(*SEPARATORS.toTypedArray())
            .map { normalize(it).trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return parts.ifEmpty { listOf(Song.UNKNOWN_ARTIST) }
    }
}

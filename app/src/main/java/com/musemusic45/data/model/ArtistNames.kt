package com.musemusic45.data.model

/**
 * 歌手字段的解析配置（可作为设置项）。
 *
 * @param splitMultiArtist 是否把「周杰伦、费玉清」拆成两位歌手
 * @param ignoreParentheses 是否忽略歌手名里的括号内容（「某某（xxx）」→「某某」）
 * @param extraSeparators **额外添加**的分隔符，会与 [DEFAULT_SEPARATORS] 合并生效。
 *   需求是「可后期添加分隔符」，所以这里是追加而不是替换 —— 默认那三个永远不会丢。
 */
data class ArtistParsingConfig(
    val splitMultiArtist: Boolean = true,
    val ignoreParentheses: Boolean = true,
    val extraSeparators: String = "",
) {
    /** 实际生效的分隔符：默认 + 额外，去重且保持顺序。 */
    val separators: String
        get() = mergeSeparators(DEFAULT_SEPARATORS, extraSeparators)

    /** 把歌手字段拆成真正的歌手名列表。 */
    fun names(raw: String): List<String> = ArtistNames.parse(raw, this)

    companion object {
        /**
         * 默认分隔符：需求指定的 `、` `；` `/`，外加半角分号与全角斜杠。
         *
         * **刻意不含逗号** —— 逗号在乐队名里是合法字符（Earth, Wind & Fire）。
         */
        const val DEFAULT_SEPARATORS = "、；;/／"

        /** 合并默认与额外分隔符，去重保序。 */
        fun mergeSeparators(base: String, extra: String): String =
            (base + extra).toList().distinct().joinToString("")

        /** 额外分隔符里，已经被默认覆盖的那些字符（不必重复显示）。 */
        fun redundantSeparators(extra: String): String =
            extra.filter { it in DEFAULT_SEPARATORS }
    }
}

/**
 * 歌手名的归一化与拆分。
 *
 * 媒体库里的歌手字段经常是「周杰伦、费玉清」或「某某（xxx）」这种形式，
 * 直接拿去分组会得到一堆只此一家的伪歌手。这里按 [ArtistParsingConfig] 规整。
 *
 * 纯函数，不依赖 Android，便于单元测试。
 */
object ArtistNames {

    private val FULL_WIDTH_PARENS = Regex("（[^）]*）")
    private val HALF_WIDTH_PARENS = Regex("\\([^)]*\\)")

    /**
     * 去掉括号及其内容：「某某（xxx）」→「某某」。
     *
     * [ignoreParentheses] 为 false 时原样返回（用户在设置里关掉了这条规则）。
     *
     * 两条规则：
     *  1. 成对的括号连同内容一起去掉（全角、半角都处理）
     *  2. 万一括号没闭合，从左括号起截断，避免留下「某某（xxx」这种残缺名字
     *
     * 全部去掉后如果什么都不剩（原串本身就只有括号），退回原串 —— 宁可留着括号，
     * 也不能产生空名字。
     */
    fun normalize(raw: String, ignoreParentheses: Boolean = true): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || !ignoreParentheses) return trimmed

        var result = FULL_WIDTH_PARENS.replace(trimmed, "")
        result = HALF_WIDTH_PARENS.replace(result, "")

        val cut = result.indexOfFirst { it == '（' || it == '(' }
        if (cut >= 0) result = result.substring(0, cut)

        result = result.trim()
        return result.ifEmpty { trimmed }
    }

    /**
     * 按配置解析歌手字段。
     *
     * 顺序：先整体去括号 → 再按分隔符拆 → 每一项各自归一化 → 去空白 → 去重。
     * 最后什么都不剩时返回 [Song.UNKNOWN_ARTIST]，绝不返回空列表。
     */
    fun parse(raw: String, config: ArtistParsingConfig): List<String> {
        val cleaned = normalize(raw, config.ignoreParentheses)

        // 关掉拆分（或分隔符为空）时，整串当作一位歌手
        if (!config.splitMultiArtist || config.separators.isEmpty()) {
            val single = cleaned.trim()
            return if (single.isEmpty()) listOf(Song.UNKNOWN_ARTIST) else listOf(single)
        }

        val delimiters = config.separators.map { it.toString() }.toTypedArray()
        val parts = cleaned.split(*delimiters)
            .map { normalize(it, config.ignoreParentheses).trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return parts.ifEmpty { listOf(Song.UNKNOWN_ARTIST) }
    }

    /** 用默认配置解析，供测试与不需要配置的场合使用。 */
    fun split(raw: String): List<String> = parse(raw, ArtistParsingConfig())
}

package com.localmusic.player.data.search

/**
 * 从拼音串里提取首字母。
 *
 * ICU 的 Han-Latin 转写结果是带声调的、空格分隔的拼音，例如
 * `周杰伦` → `zhōu jié lún`，提取首字母得到 `zjl`。
 *
 * 纯函数，便于单元测试。
 */
fun initialsFromPinyin(pinyin: String): String {
    if (pinyin.isBlank()) return ""
    return pinyin
        .split(' ', '\t', '\n', '-', '\'', '·', '/', '，', ',')
        .mapNotNull { token -> token.firstOrNull { it.isLetter() } }
        .joinToString("")
        .lowercase()
}

/**
 * 搜索匹配。
 *
 * 命中条件（任一满足）：
 *  - 原文包含查询串（不分大小写）
 *  - 拼音首字母包含查询串
 *
 * 纯函数，便于单元测试。
 */
fun matchesQuery(rawText: String, initials: String, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return false
    if (rawText.lowercase().contains(q)) return true

    // 两边都做大小写归一化，调用方传什么样进来都能正确匹配
    val normalizedInitials = initials.lowercase()
    return normalizedInitials.isNotEmpty() && normalizedInitials.contains(q)
}

/**
 * 一条可搜索的记录：原始文本 + 它的拼音首字母。
 */
data class SearchKey(
    val raw: String,
    val initials: String,
) {
    companion object {
        val EMPTY = SearchKey("", "")
    }
}

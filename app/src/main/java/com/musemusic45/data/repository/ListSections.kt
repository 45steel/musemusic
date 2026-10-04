package com.musemusic45.data.repository

import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortSpec

/** 一个分段：标题 + 该段内容。 */
data class ListSection<T>(
    val key: String,
    val items: List<T>,
) {
    /** 空 key 表示这段不需要显示标题（比如按添加时间排序时只有一段）。 */
    val showHeader: Boolean get() = key.isNotEmpty()
}

/**
 * 列表分段（第二版新增）。
 *
 * 分段方式由当前排序字段决定：
 *
 * | 排序字段 | 分段 | 段标题 |
 * |---|---|---|
 * | 名称 | 首字母 | `A`…`Z`、`#` |
 * | 发布年份 | 年份 | `2020 年`、`未知年份` |
 * | 添加时间 | **不分段** | 空 |
 *
 * 拼音通过 [romanizeOf] 注入，这样这个文件不依赖 Android，可以直接单元测试。
 */
object ListSections {

    /** 首字符不是字母时的归属段。 */
    const val FALLBACK_KEY = "#"

    /** 年份未知时的归属段。 */
    const val UNKNOWN_YEAR_KEY = "未知年份"

    /**
     * 取分段键。
     *
     * 规则（刻意做得可预测）：
     *  - 首字符是 A–Z / a–z → 该字母大写
     *  - 首字符是汉字等非 ASCII 字符 → 用拼音的首字母
     *  - 首字符是数字、符号等 → 归入 `#`
     */
    fun initialKey(name: String, romanizeOf: (String) -> String): String {
        val trimmed = name.trim()
        val first = trimmed.firstOrNull() ?: return FALLBACK_KEY

        if (first in 'A'..'Z') return first.toString()
        if (first in 'a'..'z') return first.uppercaseChar().toString()
        if (first.code < 128) return FALLBACK_KEY

        val pinyin = romanizeOf(trimmed)
        val letter = pinyin.firstOrNull { it in 'a'..'z' || it in 'A'..'Z' }
            ?: return FALLBACK_KEY
        return letter.uppercaseChar().toString()
    }

    /**
     * 按 [keyOf] 把已排好序的列表切成若干段。
     *
     * 同一个键**不相邻也会并到一段** —— 排序用的是中文 Collator，
     * 而分段键是首字母，两者顺序不完全一致（`#` 段尤其明显），
     * 不合并就会出现好几个同名段。段与段之间保持首次出现的顺序。
     */
    fun <T> group(items: List<T>, keyOf: (T) -> String): List<ListSection<T>> {
        val buckets = LinkedHashMap<String, MutableList<T>>()
        for (item in items) {
            buckets.getOrPut(keyOf(item)) { mutableListOf() }.add(item)
        }
        return buckets.map { (key, group) -> ListSection(key, group) }
    }

    /**
     * 按当前排序设定切段。
     *
     * [yearOf] 只在按发布年份排序时会被调用。
     */
    fun <T> build(
        items: List<T>,
        spec: SortSpec,
        nameOf: (T) -> String,
        yearOf: (T) -> Int,
        romanizeOf: (String) -> String,
    ): List<ListSection<T>> {
        if (items.isEmpty()) return emptyList()

        return when (spec.field) {
            SortField.NAME -> group(items) { initialKey(nameOf(it), romanizeOf) }

            SortField.YEAR -> group(items) {
                val year = yearOf(it)
                if (year > 0) "$year 年" else UNKNOWN_YEAR_KEY
            }

            // 添加时间是连续量，切成"今天/本周"这类分段意义不大
            SortField.DATE_ADDED -> listOf(ListSection("", items))
        }
    }

    /**
     * 某个元素在**整个懒加载列表**里的下标 —— 也就是把标题占位也算进去。
     *
     * 「定位到正在播放」要滚动到正确位置就得用这个：直接拿元素在自己段里的下标会少算
     * 前面所有标题和段的行数。
     *
     * 找不到时返回 -1。
     */
    fun <T> flatIndexOf(sections: List<ListSection<T>>, predicate: (T) -> Boolean): Int {
        var index = 0
        for (section in sections) {
            if (section.showHeader) index++
            val offset = section.items.indexOfFirst(predicate)
            if (offset >= 0) return index + offset
            index += section.items.size
        }
        return -1
    }

    /**
     * 每个分段在扁平列表里的**起始下标**（有标题时那一位就是标题）。
     *
     * 滚动条要用它把"拖到某个位置"翻译成"这是哪一段"，
     * 好在气泡里显示对应的字母。
     */
    fun <T> flatStartIndices(sections: List<ListSection<T>>): List<Int> {
        val starts = ArrayList<Int>(sections.size)
        var index = 0
        for (section in sections) {
            starts += index
            index += (if (section.showHeader) 1 else 0) + section.items.size
        }
        return starts
    }

    /** 扁平列表的总项数（标题占位也算一项）。 */
    fun <T> flatCount(sections: List<ListSection<T>>): Int {
        var index = 0
        for (section in sections) {
            index += (if (section.showHeader) 1 else 0) + section.items.size
        }
        return index
    }

    /**
     * 只取**有标题**的分段：返回（段标题, 该段在扁平列表里的起始下标）。
     *
     * 滚动条气泡要用它。按添加时间排序时整列只有一段且没有标题，
     * 这时返回空列表 —— 气泡自然就不显示，不会冒出一个空白的圆角块。
     */
    fun <T> headerStarts(sections: List<ListSection<T>>): List<Pair<String, Int>> {
        val starts = flatStartIndices(sections)
        return sections.indices
            .filter { sections[it].showHeader }
            .map { sections[it].key to starts[it] }
    }
}

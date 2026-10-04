package com.musemusic45.data.model

import java.text.Normalizer

/**
 * 名称排序键（第三批新增）。
 *
 * **为什么不用 `Collator` 当主排序**：`java.text.Collator.getInstance(Locale.CHINA)`
 * 的结果**跨平台不一致** —— 同一个歌手列表，JVM 单元测试里 `A` 排在汉字前面，
 * Android 设备上却排到了最后。而且它按五十音图排列假名（あ/さ/ひ…），不是罗马音。
 * 所以主路径改成自己算一条确定的键。
 *
 * **规则**
 *  1. 罗马化：汉字 → 拼音，假名 → 罗马字（由外部注入，Android 上用 ICU）
 *  2. 去掉声调、变音符号（拼音的 zhōu → zhou）
 *  3. 转小写、去掉空白（zhōu jié lún → zhoujielun）
 *  4. 直接按码位比较字符串。ASCII 里 `0`–`9` 排在 `A`–`Z` 之前，
 *     所以「数字排在字母前面，且按 0-9 顺序」是天然满足的。
 *
 * **兜底**：Android 10（API 29）以下没有 ICU 转写器，
 * 这时用 [installCollator] 退回系统排序器 —— 拿不到确定的 A-Z，
 * 但中文仍是拼音序，比按 Unicode 码位排强。
 */
object NameSortKey {

    /** 罗马化实现。默认原样返回（降级）。 */
    private var romanize: (String) -> String = { it }

    /** true 表示退回系统 Collator 比较（老系统）。 */
    private var useCollator = false

    private val cache = HashMap<String, String>()

    /** 注入罗马化实现（Android 10 及以上）。 */
    fun install(fn: (String) -> String) = synchronized(cache) {
        romanize = fn
        useCollator = false
        cache.clear()
    }

    /** 老系统兜底：用系统 Collator 比较，中文仍是拼音序。 */
    fun installCollator() = synchronized(cache) {
        romanize = { it }
        useCollator = true
        cache.clear()
    }

    /** 恢复成"按码位比较"。单元测试之间用它清理全局状态。 */
    fun installIdentity() = install { it }

    fun key(name: String): String {
        val fn = romanize
        return synchronized(cache) { cache.getOrPut(name) { build(name, fn) } }
    }

    fun compare(a: String, b: String): Int {
        if (useCollator) return ChineseCollator.compare(a, b)
        return key(a).compareTo(key(b))
    }

    val comparator: Comparator<String> = Comparator { a, b -> compare(a, b) }

    /**
     * 纯函数，便于单元测试：给定名字和罗马化实现，算出排序键。
     *
     * 罗马化抛异常时退回原文，不让排序整个崩掉。
     */
    fun build(name: String, romanize: (String) -> String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""

        val latin = runCatching { romanize(trimmed) }.getOrDefault(trimmed)

        return latin
            .let(::stripDiacritics)
            .lowercase()
            .filterNot { it.isWhitespace() }
    }

    /**
     * 去掉组合用变音符号。
     *
     * 拼音的声调（ō é ǔ …）在 Unicode 里是"字母 + 组合符号"，
     * 用 NFD 拆开后再把组合符号滤掉即可。
     */
    fun stripDiacritics(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .filter { it.code !in COMBINING_MARKS }

    /** Unicode 组合用附加符号区间。 */
    private val COMBINING_MARKS = 0x0300..0x036F
}

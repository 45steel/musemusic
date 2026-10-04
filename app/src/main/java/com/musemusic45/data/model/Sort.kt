package com.musemusic45.data.model

import java.text.Collator
import java.util.Locale

/** 排序字段。 */
enum class SortField {
    NAME,
    DATE_ADDED,
    YEAR,
    ;

    val label: String
        get() = when (this) {
            NAME -> "名称"
            DATE_ADDED -> "添加时间"
            YEAR -> "发布年份"
        }
}

/** 排序方向。 */
enum class SortOrder { ASCENDING, DESCENDING }

/** 哪个列表在用这套排序设定。 */
enum class SortTarget { SONGS, ALBUMS, ARTISTS }

/**
 * 每个字段第一次被选中时的默认方向。
 *
 * 名称默认 A→Z；时间类默认「最新在前」，因为找新加的东西更常见。
 */
fun defaultOrderFor(field: SortField): SortOrder = when (field) {
    SortField.NAME -> SortOrder.ASCENDING
    SortField.DATE_ADDED -> SortOrder.DESCENDING
    SortField.YEAR -> SortOrder.DESCENDING
}

/** 一次完整的排序设定。 */
data class SortSpec(
    val field: SortField,
    val order: SortOrder = SortOrder.ASCENDING,
) {
    fun toggled(): SortSpec =
        copy(order = if (order == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING)

    /**
     * 顶栏上显示的当前排序文字，必须一眼看懂。
     *
     * 注意：属性访问器里的 `field` 是软关键字，指该属性自己的幕后字段，
     * 所以这里必须写 `this.field` 才能拿到排序字段。
     */
    val displayLabel: String
        get() = when (this.field) {
            SortField.NAME ->
                if (order == SortOrder.ASCENDING) "名称 A→Z" else "名称 Z→A"

            SortField.DATE_ADDED ->
                if (order == SortOrder.DESCENDING) "添加时间 最新" else "添加时间 最早"

            SortField.YEAR ->
                if (order == SortOrder.DESCENDING) "发布年份 最新" else "发布年份 最早"
        }

    companion object {
        val DEFAULT = SortSpec(SortField.NAME, SortOrder.ASCENDING)

        /** 歌手没有发布年份，所以只提供两个字段。 */
        val ARTIST_FIELDS = listOf(SortField.NAME, SortField.DATE_ADDED)

        /** 歌曲和专辑提供三个字段。 */
        val SONG_FIELDS = listOf(SortField.NAME, SortField.DATE_ADDED, SortField.YEAR)
    }
}

/**
 * 系统中文排序器。
 *
 * 第三批起**不再是主排序路径**（它跨平台不一致，而且假名按五十音排），
 * 只在 Android 10 以下没有 ICU 转写器时由 [NameSortKey.installCollator] 兜底使用。
 */
val ChineseCollator: Collator = Collator.getInstance(Locale.CHINA).apply {
    strength = Collator.PRIMARY
}

/**
 * 按名称比较两个字符串。
 *
 * 第三批起改用 [NameSortKey]（自己算罗马化排序键）：汉字按拼音、假名按罗马音、
 * 数字排在字母之前，且**跨平台结果一致**。
 */
fun compareByName(a: String, b: String): Int = NameSortKey.compare(a, b)

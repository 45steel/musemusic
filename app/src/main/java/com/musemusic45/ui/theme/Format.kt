package com.musemusic45.ui.theme

import java.util.Locale

/**
 * 把毫秒时长格式化成界面显示用的字符串。
 *
 * 规则：
 *  - 时长无效（<= 0）显示 `--:--`
 *  - 不足 1 小时显示 `m:ss`，例如 `3:45`
 *  - 1 小时及以上显示 `h:mm:ss`，例如 `1:02:03`
 *  - 秒数向下取整（59.9 秒显示为 0:59）
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"

    val totalSeconds = durationMs / 1000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L

    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * 把年份格式化成显示用字符串，无效年份返回空串。
 */
fun formatYear(year: Int): String = if (year > 0) year.toString() else ""

/**
 * 专辑卡片第三行：`年份 · N 首`，没有年份时只显示 `N 首`。
 */
fun formatAlbumSubtitle(year: Int, songCount: Int): String {
    val yearText = formatYear(year)
    return if (yearText.isEmpty()) "$songCount 首" else "$yearText · $songCount 首"
}

/**
 * 歌手条目的副标题：`N 张专辑 · M 首歌`。
 */
fun formatArtistSubtitle(albumCount: Int, songCount: Int): String =
    "$albumCount 张专辑 · $songCount 首歌"

/**
 * 播放页的轮次提示：`《专辑名》· 第 x / N 张`。
 */
fun formatRoundLabel(albumName: String, index: Int, total: Int): String =
    "《$albumName》· 第 $index / $total 张"

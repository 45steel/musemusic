package com.localmusic.player.ui.nav

/**
 * 全部路由常量。覆盖层（播放页 / 面板）不占导航栈，因此不在这里。
 */
object Routes {
    // 一级页
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"

    // 路由参数名
    const val ARG_ALBUM_ID = "albumId"
    const val ARG_ARTIST_NAME = "artistName"

    // 二级页（字面量直接写全，避免 const val 之间的前向引用）
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistName}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun albumDetail(albumId: Long) = "album/$albumId"

    fun artistDetail(artistName: String) = "artist/${percentEncode(artistName)}"
}

private const val HEX = "0123456789ABCDEF"

/**
 * 把字符串编码成可以安全放进 URL 路径段的形式（UTF-8 + 百分号编码）。
 *
 * 不依赖 Android 的 Uri，这样导航地址的生成逻辑可以在纯 JVM 单元测试里验证。
 * 未被编码的字符集合与 RFC 3986 的 unreserved 一致。
 */
fun percentEncode(raw: String): String {
    val out = StringBuilder()
    for (byte in raw.toByteArray(Charsets.UTF_8)) {
        val value = byte.toInt() and 0xFF
        val ch = value.toChar()
        val unreserved = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
            ch == '-' || ch == '_' || ch == '.' || ch == '~'
        if (unreserved) {
            out.append(ch)
        } else {
            out.append('%')
            out.append(HEX[value shr 4])
            out.append(HEX[value and 0x0F])
        }
    }
    return out.toString()
}

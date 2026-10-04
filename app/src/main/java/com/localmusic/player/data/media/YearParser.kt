package com.localmusic.player.data.media

/**
 * 从标签里解析发布年份。
 *
 * 实测结论：MediaStore 的 `year` 列对 FLAC **一律为 NULL**（MP3 能正常读到），
 * 所以年份必须自己从文件标签里读。标签里的日期格式五花八门：
 *
 *  - `2001`
 *  - `2001-05-01`
 *  - `2001/05/01`
 *  - `2001.05.01T00:00:00Z`
 *  - `20010501`（紧凑日期）
 *  - `(c)2001 Sony Music`
 *
 * 纯函数，便于单元测试。
 */
fun parseYear(raw: String?): Int {
    if (raw.isNullOrBlank()) return 0
    val text = raw.trim()

    // 紧凑的八位日期 yyyyMMdd
    if (text.length == 8 && text.all { it.isDigit() }) {
        return text.substring(0, 4).toIntOrNull().toValidYear()
    }

    // 其余情况找第一个独立的四位年份（1000-2999）。
    // 前后加数字边界，避免从 "123456789" 这种串里切出错误年份。
    val match = Regex("""(?<!\d)([12]\d{3})(?!\d)""").find(text) ?: return 0
    return match.value.toIntOrNull().toValidYear()
}

private fun Int?.toValidYear(): Int =
    if (this != null && this in 1000..2999) this else 0

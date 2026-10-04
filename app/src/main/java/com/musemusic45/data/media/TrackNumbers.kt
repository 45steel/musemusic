package com.musemusic45.data.media

/**
 * MediaStore 的音轨号归一化。
 *
 * AOSP 的 MediaProvider 对多碟专辑会把音轨号存成 `碟号 * 1000 + 音轨号`
 * （实测：碟 1 轨 1 得到 1001，碟 2 轨 1 得到 2001）。
 * 直接拿这个值排序会把顺序搞乱，所以必须反算回真正的音轨号。
 *
 * 纯函数，便于单元测试。
 */
fun normalizeTrackNumber(rawTrack: Int, discNumber: Int): Int {
    if (rawTrack <= 0) return 0
    if (discNumber > 0) {
        val base = discNumber * 1000
        val remainder = rawTrack - base
        if (rawTrack > base && remainder in 1..999) {
            return remainder
        }
    }
    return rawTrack
}

/**
 * 碟号归一化：MediaStore 没有碟号时为 null，统一当作第 1 碟。
 */
fun normalizeDiscNumber(rawDisc: Int): Int = if (rawDisc > 0) rawDisc else 1

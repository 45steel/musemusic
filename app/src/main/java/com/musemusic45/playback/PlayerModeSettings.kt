package com.musemusic45.playback

import androidx.media3.common.Player
import com.musemusic45.data.model.PlayMode

/**
 * 把播放方式翻译成播放器的循环 / 随机设置。
 *
 * 抽成纯函数的原因：这段映射以前散在 [PlaybackController.applyModeToPlayer] 的
 * `when` 里，而它恰好是最容易出错的地方 ——
 * 「按专辑播放」如果没关掉循环，单专辑队列会自己转圈，永远播不完，
 * 也就永远触发不了"换下一张专辑"。加单曲循环时又多了 REPEAT_MODE_ONE 这一支。
 *
 * 单测覆盖每种模式，免得再靠人眼盯 `when`。
 */
object PlayerModeSettings {

    /** 播放器循环模式 + 是否随机。 */
    data class Settings(val repeatMode: Int, val shuffle: Boolean)

    fun of(mode: PlayMode): Settings = when (mode) {
        // 顺序放完一遍再从头
        PlayMode.LIST_LOOP -> Settings(Player.REPEAT_MODE_ALL, shuffle = false)

        // 一直重复当前这首歌
        PlayMode.SINGLE_LOOP -> Settings(Player.REPEAT_MODE_ONE, shuffle = false)

        // 打乱后循环
        PlayMode.SHUFFLE -> Settings(Player.REPEAT_MODE_ALL, shuffle = true)

        // 按专辑播放**必须关循环**：单专辑队列如果自己转圈，
        // 就永远播不完，也就永远触发不了"换下一张专辑"。
        PlayMode.ALBUM_SHUFFLE -> Settings(Player.REPEAT_MODE_OFF, shuffle = false)
    }
}

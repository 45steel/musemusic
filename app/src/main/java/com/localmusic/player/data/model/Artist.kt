package com.localmusic.player.data.model

/**
 * 一位歌手。以歌手名为主键（合唱歌曲原样当作一个歌手，第一版不拆分）。
 */
data class Artist(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    /** 该歌手名下最早加入媒体库的时间，单位秒。用于"添加时间"排序。 */
    val dateAddedSec: Long,
)

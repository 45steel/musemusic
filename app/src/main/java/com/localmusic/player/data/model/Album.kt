package com.localmusic.player.data.model

/**
 * 一张专辑。以媒体库的专辑 ID 为主键。
 */
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val year: Int,
    val songCount: Int,
    /** 该专辑内最早加入媒体库的时间，单位秒。用于"添加时间"排序。 */
    val dateAddedSec: Long,
)

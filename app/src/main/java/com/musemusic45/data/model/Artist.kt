package com.musemusic45.data.model

/**
 * 一位歌手。以歌手名为主键。
 *
 * 第二版起：多歌手歌曲会拆分归属（见 [ArtistNames]），歌手名也做了括号归一化。
 */
data class Artist(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    /** 该歌手名下最早加入媒体库的时间，单位秒。用于"添加时间"排序。 */
    val dateAddedSec: Long,
    /**
     * 头像用的专辑封面（第二版新增）。
     *
     * 取该歌手**发布年份最早**的那张专辑；0 表示没有可用封面，界面退回首字色块。
     */
    val coverAlbumId: Long = 0L,
    /** 头像所取专辑的年份，0 表示该专辑没有年份信息。 */
    val coverYear: Int = 0,
)

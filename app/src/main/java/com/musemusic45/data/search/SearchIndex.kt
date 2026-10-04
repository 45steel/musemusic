package com.musemusic45.data.search

import android.icu.text.Transliterator
import android.os.Build
import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Artist
import com.musemusic45.data.model.Song

/**
 * 汉字转拼音。
 *
 * 用系统内置的 ICU 转写器（Android 10 / API 29 起可用）。
 * 更早的系统上没有这个能力，退化为只做原文匹配 —— 功能降级但不崩溃。
 */
class PinyinProvider {

    private val hanLatin = createTransliterator("Han-Latin")
    private val anyLatin = createTransliterator("Any-Latin")
    private val latinAscii = createTransliterator("Latin-ASCII")

    val available: Boolean get() = hanLatin != null

    /** 转成拼音；不支持时原样返回。 */
    fun toPinyin(text: String): String = applyTo(hanLatin, text)

    /** 直接取拼音首字母。 */
    fun initialsOf(text: String): String = initialsFromPinyin(toPinyin(text))

    /**
     * 统一罗马化（第三批新增）。
     *
     * 汉字 → 拼音，片假名/平假名 → 罗马字，最后去掉声调符号：
     * 「周杰伦」→ `zhou jie lun`、「カタカナ」→ `katakana`、「さくら」→ `sakura`。
     *
     * 先跑 Han-Latin 是为了让汉字拿到**汉语**读音（`Any-Latin` 对纯汉字串的
     * 处理不保证是拼音），再跑 `Any-Latin` 覆盖假名等其余文字，最后 `Latin-ASCII` 去声调。
     */
    fun toLatin(text: String): String {
        var result = applyTo(hanLatin, text)
        result = applyTo(anyLatin, result)
        return applyTo(latinAscii, result)
    }

    private fun applyTo(transliterator: Transliterator?, text: String): String =
        transliterator?.let { runCatching { it.transliterate(text) }.getOrDefault(text) } ?: text
}

/** ICU 转写器只在 Android 10（API 29）及以上可用，更低版本返回 null 并降级。 */
private fun createTransliterator(id: String): Transliterator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching { Transliterator.getInstance(id) }.getOrNull()
    } else {
        null
    }

/**
 * 搜索索引。
 *
 * 扫描完成后一次性把歌曲、专辑、歌手的拼音首字母算好并缓存，
 * 搜索时只做字符串匹配，不再做转写。
 */
class SearchIndex(
    val songs: List<IndexedSong>,
    val albums: List<IndexedAlbum>,
    val artists: List<IndexedArtist>,
) {
    data class IndexedSong(
        val song: Song,
        val titleKey: SearchKey,
        val artistKey: SearchKey,
        val albumKey: SearchKey,
    )

    data class IndexedAlbum(
        val album: Album,
        val nameKey: SearchKey,
        val artistKey: SearchKey,
    )

    data class IndexedArtist(
        val artist: Artist,
        val nameKey: SearchKey,
    )

    /** 按查询串过滤，返回分组结果。 */
    fun search(query: String): SearchResult {
        if (query.isBlank()) return SearchResult.EMPTY

        val matchedSongs = songs.asSequence()
            .filter { indexed ->
                matchesQuery(indexed.song.title, indexed.titleKey.initials, query) ||
                    matchesQuery(indexed.song.artist, indexed.artistKey.initials, query) ||
                    matchesQuery(indexed.song.album, indexed.albumKey.initials, query)
            }
            .map { it.song }
            .toList()

        val matchedAlbums = albums.asSequence()
            .filter { indexed ->
                matchesQuery(indexed.album.name, indexed.nameKey.initials, query) ||
                    matchesQuery(indexed.album.artist, indexed.artistKey.initials, query)
            }
            .map { it.album }
            .toList()

        val matchedArtists = artists.asSequence()
            .filter { indexed -> matchesQuery(indexed.artist.name, indexed.nameKey.initials, query) }
            .map { it.artist }
            .toList()

        return SearchResult(matchedSongs, matchedAlbums, matchedArtists)
    }

    companion object {
        val EMPTY = SearchIndex(emptyList(), emptyList(), emptyList())

        fun build(
            allSongs: List<Song>,
            allAlbums: List<Album>,
            allArtists: List<Artist>,
            provider: PinyinProvider,
        ): SearchIndex {
            // 同一段文字（尤其是歌手名、专辑名）在库里大量重复，这里做一次缓存
            val cache = HashMap<String, SearchKey>()
            fun keyOf(text: String): SearchKey =
                cache.getOrPut(text) { SearchKey(text, provider.initialsOf(text)) }

            return SearchIndex(
                songs = allSongs.map { song ->
                    IndexedSong(
                        song = song,
                        titleKey = keyOf(song.title),
                        artistKey = keyOf(song.artist),
                        albumKey = keyOf(song.album),
                    )
                },
                albums = allAlbums.map { album ->
                    IndexedAlbum(
                        album = album,
                        nameKey = keyOf(album.name),
                        artistKey = keyOf(album.artist),
                    )
                },
                artists = allArtists.map { artist ->
                    IndexedArtist(artist = artist, nameKey = keyOf(artist.name))
                },
            )
        }
    }
}

/** 搜索结果，分三段。 */
data class SearchResult(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
    val totalCount: Int get() = songs.size + albums.size + artists.size

    companion object {
        val EMPTY = SearchResult(emptyList(), emptyList(), emptyList())
    }
}

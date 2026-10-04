package com.musemusic45.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Artist
import com.musemusic45.data.model.Song
import com.musemusic45.data.search.SearchIndex
import com.musemusic45.data.search.SearchResult
import com.musemusic45.ui.components.ArtistAvatar
import com.musemusic45.ui.components.CoverImage
import com.musemusic45.ui.components.SectionHeader
import com.musemusic45.ui.theme.AppShapes
import com.musemusic45.ui.theme.formatArtistSubtitle
import com.musemusic45.ui.theme.formatDuration

/**
 * 搜索页。三个一级页顶部的搜索条都进到这一个页面，结果统一分成
 * 「歌曲 / 专辑 / 歌手」三段。
 */
@Composable
fun SearchScreen(
    index: SearchIndex?,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * 是否在进入时自动聚焦输入框。
     *
     * 预测式返回的**目的地预览**会把它设成 false —— 预览只是画一张图，
     * 要是那里也抢焦点，手指还没松开输入法就弹出来了。
     */
    autoFocus: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }

    val result = remember(query, index) {
        index?.search(query) ?: SearchResult.EMPTY
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("输入歌名、歌手或专辑") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清空")
                    }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            shape = RoundedCornerShape(AppShapes.searchBar),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
        )

        when {
            query.isBlank() -> HintText("输入关键词即可搜索歌名、歌手或专辑名")

            result.isEmpty -> HintText("没有找到「$query」\n可以试试歌手名或专辑名")

            else -> SearchResults(
                result = result,
                onSongClick = onSongClick,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
    )
}

@Composable
private fun SearchResults(
    result: SearchResult,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (result.songs.isNotEmpty()) {
            item(key = "sec-songs") { SectionHeader("歌曲 (${result.songs.size})") }
            items(
                count = result.songs.size,
                key = { "s-${result.songs[it].id}" },
            ) { i ->
                val song = result.songs[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverImage(
                        albumId = song.albumId,
                        modifier = Modifier.size(SEARCH_COVER_SIZE),
                        shape = RoundedCornerShape(AppShapes.forCoverSize(SEARCH_COVER_SIZE)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${song.artist} · ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        formatDuration(song.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 第八批：去掉逐行分隔线
            }
        }

        if (result.albums.isNotEmpty()) {
            item(key = "sec-albums") { SectionHeader("专辑 (${result.albums.size})") }
            items(
                count = result.albums.size,
                key = { "a-${result.albums[it].id}" },
            ) { i ->
                val album = result.albums[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverImage(
                        albumId = album.id,
                        modifier = Modifier.size(SEARCH_COVER_SIZE),
                        shape = RoundedCornerShape(AppShapes.forCoverSize(SEARCH_COVER_SIZE)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            album.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 第八批：去掉逐行分隔线
            }
        }

        if (result.artists.isNotEmpty()) {
            item(key = "sec-artists") { SectionHeader("歌手 (${result.artists.size})") }
            items(
                count = result.artists.size,
                key = { "ar-${result.artists[it].name}" },
            ) { i ->
                val artist = result.artists[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtistAvatar(name = artist.name, size = SEARCH_COVER_SIZE)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatArtistSubtitle(artist.albumCount, artist.songCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 第八批：去掉逐行分隔线
            }
        }

        item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

/**
 * 搜索页里小封面的尺寸。
 *
 * 第八批改成用共用的 [com.musemusic45.ui.components.SectionHeader] 与本文件里
 * 私有的那份重复实现 —— 私有版本被删掉了，否则改了共用版这里不会有反应。
 */
private val SEARCH_COVER_SIZE = 44.dp

package com.musemusic45.ui.songs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musemusic45.data.model.Song
import com.musemusic45.data.repository.ListSection
import com.musemusic45.ui.components.SectionHeader
import com.musemusic45.ui.components.SongRow

/**
 * 歌曲页内容区。顶栏与搜索条由 AppRoot 统一提供。
 *
 * 第二版：列表按分段显示，段标题吸顶。分几段、按什么分由排序字段决定
 * （见 `ListSections`）；按「添加时间」排序时只有一段且不显示标题。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsScreen(
    sections: List<ListSection<Song>>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onSongLongClick: ((Song) -> Unit)? = null,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        sections.forEach { section ->
            if (section.showHeader) {
                stickyHeader(key = "header-${section.key}") {
                    SectionHeader(section.key)
                }
            }
            items(items = section.items, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                )
                // 第八批：去掉逐行分隔线 —— M3 的列表靠间距和分段标题区分，
                // 每行都画线会显得杂乱，也和"不要多余装饰"相悖。
            }
        }
    }
}

package com.musemusic45.ui.songs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.musemusic45.data.model.Song
import com.musemusic45.data.repository.ListSection
import com.musemusic45.data.repository.ListSections
import com.musemusic45.ui.components.ListScrollbar
import com.musemusic45.ui.components.SectionHeader
import com.musemusic45.ui.components.SongRow
import kotlinx.coroutines.launch

/**
 * 歌曲页内容区。顶栏与搜索条由 AppRoot 统一提供。
 *
 * 第二版：列表按分段显示，段标题吸顶。分几段、按什么分由排序字段决定
 * （见 `ListSections`）；按「添加时间」排序时只有一段且不显示标题。
 *
 * 第十批：右侧加滚动条（第十批第 4 项）——
 * 滚动条本身不显示字母，拖动/滚动时才用气泡显示当前分段。
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
    val scope = rememberCoroutineScope()
    val headerStarts = remember(sections) { ListSections.headerStarts(sections) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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

        ListScrollbar(
            firstVisibleIndex = listState.firstVisibleItemIndex,
            totalItemCount = listState.layoutInfo.totalItemsCount,
            visibleItemCount = listState.layoutInfo.visibleItemsInfo.size,
            sectionKeys = headerStarts.map { it.first },
            sectionStarts = headerStarts.map { it.second },
            onScrollTo = { index -> scope.launch { listState.scrollToItem(index) } },
            isScrolling = listState.isScrollInProgress,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

package com.musemusic45.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.Album
import com.musemusic45.data.repository.ListSection
import com.musemusic45.data.repository.ListSections
import com.musemusic45.ui.components.CoverImage
import com.musemusic45.ui.components.ListScrollbar
import com.musemusic45.ui.components.SectionHeader
import com.musemusic45.ui.theme.AppShapes
import com.musemusic45.ui.theme.formatAlbumSubtitle
import kotlinx.coroutines.launch

/**
 * 专辑页：三列网格。顶栏与搜索条由 AppRoot 统一提供。
 *
 * 第二版：按分段显示。段标题用整行占位（`GridItemSpan(maxLineSpan)`）。
 * 网格布局不支持吸顶标题，所以专辑页的标题在滚动时不会固定 —— 这是平台的限制，
 * 歌曲页与歌手页用的是 LazyColumn，标题是真吸顶的。
 *
 * 第十批：右侧加滚动条。网格用的是 `LazyGridState`，
 * 与列表的状态类型不同，但滚动条只要求四项滚动信息，所以两边共用同一个组件。
 */
@Composable
fun AlbumsScreen(
    sections: List<ListSection<Album>>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val scope = rememberCoroutineScope()
    val headerStarts = remember(sections) { ListSections.headerStarts(sections) }

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            sections.forEach { section ->
                if (section.showHeader) {
                    item(
                        key = "header-${section.key}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SectionHeader(section.key)
                    }
                }
                items(items = section.items, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }

        ListScrollbar(
            firstVisibleIndex = gridState.firstVisibleItemIndex,
            totalItemCount = gridState.layoutInfo.totalItemsCount,
            visibleItemCount = gridState.layoutInfo.visibleItemsInfo.size,
            sectionKeys = headerStarts.map { it.first },
            sectionStarts = headerStarts.map { it.second },
            onScrollTo = { index -> scope.launch { gridState.scrollToItem(index) } },
            isScrolling = gridState.isScrollInProgress,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        CoverImage(
            albumId = album.id,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(AppShapes.forCoverSize(GRID_COVER_SIZE)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatAlbumSubtitle(album.year, album.songCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 网格封面的标称尺寸。
 *
 * 三列网格里实际宽度随屏幕变化，但圆角按这个档位取就行 ——
 * 尺寸分档只是为了让圆角与封面大小成比例（见 [AppShapes.forCoverSize]）。
 */
private val GRID_COVER_SIZE = 110.dp

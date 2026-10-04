package com.musemusic45.ui.artists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.Artist
import com.musemusic45.data.repository.ListSection
import com.musemusic45.data.repository.ListSections
import com.musemusic45.ui.components.ArtistAvatar
import com.musemusic45.ui.components.ListScrollbar
import com.musemusic45.ui.components.SectionHeader
import com.musemusic45.ui.theme.formatArtistSubtitle
import kotlinx.coroutines.launch

/**
 * 歌手页。顶栏与搜索条由 AppRoot 统一提供。
 *
 * 第二版：分段显示 + 段标题吸顶；头像改用该歌手最早年份专辑的封面。
 * 第十批：右侧加滚动条（拖动/滚动时用气泡显示当前分段）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsScreen(
    sections: List<ListSection<Artist>>,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
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
                items(items = section.items, key = { it.name }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artist) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtistAvatar(
                            name = artist.name,
                            albumId = artist.coverAlbumId,
                            size = ARTIST_AVATAR_SIZE,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatArtistSubtitle(artist.albumCount, artist.songCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    // 第八批：去掉逐行分隔线，靠间距与分段标题区分
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

/** 列表里的歌手头像尺寸。 */
private val ARTIST_AVATAR_SIZE = 48.dp

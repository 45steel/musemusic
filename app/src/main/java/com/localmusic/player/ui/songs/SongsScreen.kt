package com.localmusic.player.ui.songs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localmusic.player.data.model.Song
import com.localmusic.player.ui.components.SongRow

/**
 * 歌曲页内容区。顶栏与搜索条由 AppRoot 统一提供。
 */
@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id },
        ) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentSongId,
                onClick = { onSongClick(index) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

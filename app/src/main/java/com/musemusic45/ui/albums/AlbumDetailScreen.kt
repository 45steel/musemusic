package com.musemusic45.ui.albums

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.Album
import com.musemusic45.data.model.Song
import com.musemusic45.data.repository.LibraryAggregator
import com.musemusic45.ui.components.CoverImage
import com.musemusic45.ui.theme.formatAlbumSubtitle
import com.musemusic45.ui.theme.formatDuration

/**
 * 专辑详情。
 *
 * 曲目**按碟号分节**显示，节内按音轨号升序 —— 需求要求"专辑内按碟号和音轨号升序"，
 * 界面上就得让这个顺序看得见。
 */
@Composable
fun AlbumDetailScreen(
    album: Album,
    songs: List<Song>,
    currentSongId: Long?,
    onPlayAll: () -> Unit,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(songs) { buildAlbumRows(songs) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "album-header-${album.id}") {
            AlbumHeader(album = album, onPlayAll = onPlayAll)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        items(items = rows, key = { it.key }) { row ->
            when (row) {
                is AlbumRow.DiscHeader -> DiscHeaderText(row.disc)
                is AlbumRow.Track -> {
                    TrackRow(
                        song = row.song,
                        isCurrent = row.song.id == currentSongId,
                        onClick = { onSongClick(row.index) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(album: Album, onPlayAll: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverImage(
                albumId = album.id,
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(10.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatAlbumSubtitle(album.year, album.songCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPlayAll) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("播放整张")
                }
            }
        }
    }
}

@Composable
private fun DiscHeaderText(disc: Int) {
    Text(
        text = "碟 $disc",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun TrackRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (song.hasTrack) "${song.trackNumber}" else "–",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 第三批：专辑详情页也要显示歌手（合辑里每首歌的歌手都不同）
            Text(
                text = if (isCurrent) {
                    "正在播放 · ${song.artist}"
                } else {
                    song.artist
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

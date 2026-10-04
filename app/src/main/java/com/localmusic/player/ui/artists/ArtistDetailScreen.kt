package com.localmusic.player.ui.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.localmusic.player.data.model.Album
import com.localmusic.player.data.model.Artist
import com.localmusic.player.data.model.Song
import com.localmusic.player.data.repository.LibraryAggregator
import com.localmusic.player.ui.components.ArtistAvatar
import com.localmusic.player.ui.components.CoverImage
import com.localmusic.player.ui.theme.formatAlbumSubtitle
import com.localmusic.player.ui.theme.formatArtistSubtitle
import com.localmusic.player.ui.theme.formatDuration

/**
 * 歌手详情：上段是专辑横滑条，下段是该歌手的全部歌曲，两段一体滚动。
 */
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    albums: List<Album>,
    songs: List<Song>,
    currentSongId: Long?,
    onAlbumClick: (Album) -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedSongs = remember(songs) { LibraryAggregator.sortArtistSongs(songs) }
    val sortedAlbums = remember(albums) { albums.sortedBy { it.name } }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "artist-header") {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtistAvatar(name = artist.name, size = 72.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatArtistSubtitle(artist.albumCount, artist.songCount),
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
                            Text("播放全部")
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        if (sortedAlbums.isNotEmpty()) {
            item(key = "artist-albums") {
                Column {
                    SectionTitle("专辑 (${sortedAlbums.size})")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = sortedAlbums, key = { it.id }) { album ->
                            ArtistAlbumCard(album = album, onClick = { onAlbumClick(album) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        item(key = "artist-songs-title") {
            SectionTitle("全部歌曲 (${sortedSongs.size})")
        }

        itemsIndexed(
            items = sortedSongs,
            key = { _, song -> "artist-song-${song.id}" },
        ) { index, song ->
            ArtistSongRow(
                song = song,
                isCurrent = song.id == currentSongId,
                onClick = { onSongClick(index) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun ArtistAlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick),
    ) {
        CoverImage(
            albumId = album.id,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun ArtistSongRow(
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
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

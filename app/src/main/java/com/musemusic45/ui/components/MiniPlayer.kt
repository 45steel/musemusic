package com.musemusic45.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.Song
import com.musemusic45.ui.theme.AppShapes

/**
 * 全局迷你播放器。没有正在播放的歌时整条不显示。
 *
 * 第八批：从"通栏色块"改成**浮起的 M3 卡片** —— 大圆角 + tonal 表面 + 柔和阴影，
 * 和底部导航条拉开层次。播放/暂停用 [FilledIconButton]（M3 高强调控件），
 * 上一首/下一首是次级操作，保持普通图标按钮。
 */
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (song == null) return

    Surface(
        shape = RoundedCornerShape(AppShapes.miniPlayer),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = MINI_PLAYER_SHADOW,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(MINI_PLAYER_HEIGHT)
            .clickable(onClick = onExpand),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 4.dp),
        ) {
            CoverImage(
                albumId = song.albumId,
                modifier = Modifier.size(MINI_COVER_SIZE),
                shape = RoundedCornerShape(AppShapes.forCoverSize(MINI_COVER_SIZE)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首")
            }
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(MINI_PLAY_BUTTON_SIZE),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "下一首")
            }
        }
    }
}

private val MINI_PLAYER_HEIGHT = 64.dp
private val MINI_PLAYER_SHADOW = 6.dp
private val MINI_COVER_SIZE = 44.dp
private val MINI_PLAY_BUTTON_SIZE = 40.dp

package com.localmusic.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localmusic.player.data.media.LyricsState
import com.localmusic.player.data.model.Song
import com.localmusic.player.ui.components.CoverImage
import com.localmusic.player.ui.theme.formatDuration
import kotlin.math.roundToInt

/**
 * 全屏播放页（覆盖层）。
 *
 * M3 阶段已接入真实播放状态（进度、播放暂停、上下首、拖动跳转）。
 * 播放方式面板在 M4、播放列表面板在 M4、歌词在 M9 接入。
 */
@Composable
fun PlayerSheet(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    modeLabel: String,
    roundLabel: String?,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onModeClick: () -> Unit,
    onQueueClick: () -> Unit,
    lyricsState: LyricsState,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 点封面切到歌词态，再点切回封面态。
    // 没有歌词时**不允许切换** —— 否则会点进一个空页面。
    //
    // 刻意不绑定歌曲 ID：切歌时保持当前的封面/歌词态，
    // 这样切到没有歌词的歌才会显示「暂无歌词」，而不是莫名其妙跳回封面。
    val hasLyrics = lyricsState !is LyricsState.None
    var showLyrics by remember { mutableStateOf(false) }

    // 拖动进度条时先预览，松手才真正跳转
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(0L)
    val actualProgress = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownProgress = if (dragging) dragValue else actualProgress
    val shownPositionMs = (shownProgress * safeDuration).toLong()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, dragOffset.roundToInt())
                }
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                },
                onDragStopped = {
                    if (dragOffset > COLLAPSE_THRESHOLD_PX) onCollapse() else dragOffset = 0f
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "收起")
                }
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(48.dp))
            }

            if (showLyrics) {
                LyricsPane(
                    state = lyricsState,
                    positionMs = positionMs,
                    onSeek = onSeek,
                    onExitLyrics = { showLyrics = false },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = song?.let { "${it.title} — ${it.artist}" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "点歌词区空白处回到封面",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            } else {
                Spacer(Modifier.weight(1f))

                CoverImage(
                    albumId = song?.albumId ?: 0L,
                    modifier = Modifier
                        .size(260.dp)
                        .clickable(enabled = hasLyrics) { showLyrics = true },
                    shape = RoundedCornerShape(16.dp),
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = song?.title ?: "没有正在播放的歌曲",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = song?.let { "${it.artist} · ${it.album}" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (roundLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = roundLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Slider(
                value = shownProgress,
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    if (safeDuration > 0L) {
                        onSeek((dragValue * safeDuration).toLong())
                    }
                    dragging = false
                },
                enabled = safeDuration > 0L,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(shownPositionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(safeDuration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onModeClick) {
                    Icon(Icons.Filled.Repeat, contentDescription = "播放方式")
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(56.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onQueueClick) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放列表")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 下拉超过这个距离（像素）就收起播放页。 */
private const val COLLAPSE_THRESHOLD_PX = 220f

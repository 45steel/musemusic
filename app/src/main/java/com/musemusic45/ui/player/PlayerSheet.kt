package com.musemusic45.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
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
import com.musemusic45.data.media.LyricsState
import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song
import com.musemusic45.ui.components.CoverImage
import com.musemusic45.ui.theme.LyricsPalette
import com.musemusic45.ui.theme.formatDuration
import kotlin.math.roundToInt

/**
 * 全屏播放页（覆盖层）。
 *
 * 第七批（界面美化）调整：
 *  - 莫奈风格：颜色全部来自主题（跟随壁纸），不再写死
 *  - 封面放进**独立大圆角卡片**，卡片带柔和阴影
 *  - 进度条与播放控件换成 M3 标准大圆角控件，并收进**底部控制卡片**分层
 *  - 歌词区保持沉浸式：纯色底、无卡片、无边框（见 [LyricsPane]）
 *
 * 交互（保持不变）：
 *  - 点封面或空白区切到歌词态，歌词态点空白回封面
 *  - 歌名 / 歌手 / 专辑均可点击跳转
 *  - 下拉超过阈值收起播放页
 */
@Composable
fun PlayerSheet(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    mode: PlayMode,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onModeClick: () -> Unit,
    onQueueClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    onAlbumClick: (Long) -> Unit,
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTopBar(modeLabel = mode.playerTitle, onCollapse = onCollapse)

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
                LyricsFooter(song = song)
            } else {
                // 封面与歌名整块在剩余空间里垂直居中。
                // 整块空白区域都可以点进歌词态（歌名/歌手/专辑自己的点击优先）。
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            enabled = hasLyrics,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { showLyrics = true },
                    contentAlignment = Alignment.Center,
                ) {
                    CoverAndTitles(
                        song = song,
                        hasLyrics = hasLyrics,
                        onEnterLyrics = { showLyrics = true },
                        onTitleClick = onTitleClick,
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            PlayerControlsCard(
                progress = shownProgress,
                onProgressChange = {
                    dragging = true
                    dragValue = it
                },
                onProgressChangeFinished = {
                    if (safeDuration > 0L) {
                        onSeek((dragValue * safeDuration).toLong())
                    }
                    dragging = false
                },
                seekEnabled = safeDuration > 0L,
                positionLabel = formatDuration(shownPositionMs),
                durationLabel = formatDuration(safeDuration),
                isPlaying = isPlaying,
                mode = mode,
                onModeClick = onModeClick,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onQueueClick = onQueueClick,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayerTopBar(modeLabel: String, onCollapse: () -> Unit) {
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
}

/**
 * 封面 + 歌名 / 歌手 / 专辑。
 *
 * 封面放进一个**独立的大圆角卡片**（[Surface]，28dp 圆角 + 8dp 柔和阴影）。
 * 封面本身铺满卡片 —— 卡片不再加边框或内衬，避免多余装饰。
 */
@Composable
private fun CoverAndTitles(
    song: Song?,
    hasLyrics: Boolean,
    onEnterLyrics: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    onAlbumClick: (Long) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(COVER_CORNER),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = COVER_SHADOW,
            modifier = Modifier
                .size(COVER_SIZE)
                .clickable(enabled = hasLyrics) { onEnterLyrics() },
        ) {
            CoverImage(
                albumId = song?.albumId ?: 0L,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(COVER_CORNER),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = song?.title ?: "没有正在播放的歌曲",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = song != null) { onTitleClick() },
        )
        Spacer(Modifier.height(6.dp))

        if (song != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onArtistClick() },
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(2f, fill = false)
                        .clickable { onAlbumClick(song.albumId) },
                )
            }
        }
    }
}

/**
 * 底部控制卡片：进度条 + 时间 + 播放控件。
 *
 * 按 M3 卡片分层收在一个大圆角的 tonal 卡片里，和上面的歌词区明确分开。
 * 播放/暂停用 [FilledIconButton]（M3 标准的高强调圆形控件），
 * 切歌与播放方式/列表用 [FilledTonalIconButton]（次级强调）。
 */
@Composable
private fun PlayerControlsCard(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    seekEnabled: Boolean,
    positionLabel: String,
    durationLabel: String,
    isPlaying: Boolean,
    mode: PlayMode,
    onModeClick: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onQueueClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(CONTROL_CARD_CORNER),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Slider(
                value = progress,
                onValueChange = onProgressChange,
                onValueChangeFinished = onProgressChangeFinished,
                enabled = seekEnabled,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(positionLabel, style = MaterialTheme.typography.labelSmall)
                Text(durationLabel, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = onModeClick) {
                    // 图标跟随当前播放方式 —— 写死成 Repeat 的话，
                    // 切到单曲循环在播放页上看不出任何变化
                    Icon(mode.icon(), contentDescription = "播放方式: ${mode.label}")
                }
                FilledTonalIconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(SKIP_BUTTON_SIZE),
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(28.dp),
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(PLAY_BUTTON_SIZE),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(34.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(SKIP_BUTTON_SIZE),
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(28.dp),
                    )
                }
                FilledTonalIconButton(onClick = onQueueClick) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放列表")
                }
            }
        }
    }
}

/** 歌词态底部的一行歌名 + 操作提示，用歌词调色板保持同色系。 */
@Composable
private fun LyricsFooter(song: Song?) {
    val background = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = song?.let { "${it.title} — ${it.artist}" } ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = LyricsPalette.idle(primary, background),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "点歌词区空白处回到封面",
            style = MaterialTheme.typography.labelSmall,
            color = LyricsPalette.idleFaded(primary, background, mix = 0.85f),
        )
    }
}

/** 下拉超过这个距离（像素）就收起播放页。 */
private const val COLLAPSE_THRESHOLD_PX = 220f

/** 封面卡片：大圆角 + 柔和阴影。 */
private val COVER_CORNER = 28.dp
private val COVER_SHADOW = 8.dp
private val COVER_SIZE = 260.dp

/** 底部控制卡片的圆角。 */
private val CONTROL_CARD_CORNER = 28.dp

/** 播放键（高强调）与切歌键（次级）的尺寸。 */
private val PLAY_BUTTON_SIZE = 68.dp
private val SKIP_BUTTON_SIZE = 52.dp

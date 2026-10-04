package com.musemusic45.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.musemusic45.data.media.LrcLine
import com.musemusic45.data.media.LyricsState
import com.musemusic45.ui.theme.LyricsPalette

/**
 * 歌词区（沉浸式排版）。
 *
 * 设计要求：
 *  - **背景是纯色**，不跟随歌曲 —— 明确铺一层 `Surface`，杜绝渐变/模糊图片/专辑图当背景
 *  - **没有卡片容器、没有边框**，歌词直接叠在纯色背景上
 *  - 当前行放大高亮（高对比度主题色），其余行**同色系的低饱和色**加半透明弱化
 *  - 文字颜色全部来自莫奈主题色（[LyricsPalette]），背景固定不变
 *
 * 交互：
 *  - 点任意一行跳到那个时间点
 *  - 点空白处回到封面态（歌词行的点击优先）
 */
@Composable
fun LyricsPane(
    state: LyricsState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onExitLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onExitLyrics),
        ) {
            when (state) {
                is LyricsState.None -> EmptyLyrics()

                is LyricsState.Plain -> PlainLyrics(state.text)

                is LyricsState.Synced -> SyncedLyrics(
                    state = state,
                    positionMs = positionMs,
                    onSeek = onSeek,
                )
            }
        }
    }
}

@Composable
private fun EmptyLyrics() {
    val background = MaterialTheme.colorScheme.surface
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "暂无歌词",
            style = MaterialTheme.typography.titleMedium,
            color = LyricsPalette.idleFaded(MaterialTheme.colorScheme.primary, background),
        )
    }
}

@Composable
private fun PlainLyrics(text: String) {
    val background = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = LyricsPalette.idle(MaterialTheme.colorScheme.primary, background),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun SyncedLyrics(
    state: LyricsState.Synced,
    positionMs: Long,
    onSeek: (Long) -> Unit,
) {
    val lines = state.document.lines
    val currentIndex = remember(lines, positionMs) { state.document.indexAt(positionMs) }
    val listState = rememberLazyListState()

    // 当前行变化时滚到中间
    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex >= 0 && lines.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(currentIndex) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            count = lines.size,
            key = { index -> "$index-${lines[index].timeMs}" },
        ) { index ->
            val line = lines[index]
            LyricLineView(
                line = line,
                isCurrent = index == currentIndex,
                positionMs = positionMs,
                onClick = { onSeek(line.timeMs) },
            )
        }
    }
}

/**
 * 一行歌词：原文在上，译文在下。
 *
 * 当前行若带逐字时间轴，则按 [LrcLine.sungLength] 把原文分成"已唱/未唱"两段染色。
 * 所有颜色都走 [LyricsPalette]，保证是同色系而不是灰阶。
 */
@Composable
private fun LyricLineView(
    line: LrcLine,
    isCurrent: Boolean,
    positionMs: Long,
    onClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary

    val currentColor = LyricsPalette.current(primary)
    val idleColor = LyricsPalette.idleFaded(primary, background)
    val unsungColor = LyricsPalette.unsung(primary, background)
    val translationColor = LyricsPalette.currentTranslation(primary, background)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isCurrent && line.hasWords) {
            val sung = remember(line, positionMs) { line.sungLength(positionMs) }
            val styled = buildAnnotatedString {
                withStyle(SpanStyle(color = currentColor)) {
                    append(line.text.take(sung))
                }
                withStyle(SpanStyle(color = unsungColor)) {
                    append(line.text.drop(sung))
                }
            }
            Text(
                text = styled,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = line.text.ifEmpty { " " },
                style = if (isCurrent) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) currentColor else idleColor,
                textAlign = TextAlign.Center,
            )
        }

        // 译文单独占一行（双语歌词）
        if (line.hasTranslation) {
            Text(
                text = line.translation.orEmpty(),
                style = if (isCurrent) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = if (isCurrent) translationColor else idleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

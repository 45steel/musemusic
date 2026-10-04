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

/**
 * 歌词区。
 *
 * - 带时间轴：逐行滚动，当前行居中高亮，**点任意一行跳到那个时间点**
 * - 有译文时，译文显示在原文下面一行（双语歌词）
 * - 有逐字时间轴时，当前行按进度**逐字高亮**
 * - 纯文本：整页静态显示
 * - 没有歌词：显示「暂无歌词」
 *
 * 点空白处调用 [onExitLyrics] 回到封面态（歌词行的点击优先，用于跳转）。
 */
@Composable
fun LyricsPane(
    state: LyricsState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onExitLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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

@Composable
private fun EmptyLyrics() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "暂无歌词",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlainLyrics(text: String) {
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
 */
@Composable
private fun LyricLineView(
    line: LrcLine,
    isCurrent: Boolean,
    positionMs: Long,
    onClick: () -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baseStyle = if (isCurrent) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isCurrent && line.hasWords) {
            val sung = remember(line, positionMs) { line.sungLength(positionMs) }
            val styled = buildAnnotatedString {
                withStyle(SpanStyle(color = activeColor)) {
                    append(line.text.take(sung))
                }
                withStyle(SpanStyle(color = idleColor)) {
                    append(line.text.drop(sung))
                }
            }
            Text(
                text = styled,
                style = baseStyle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = line.text.ifEmpty { " " },
                style = baseStyle,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) activeColor else idleColor,
                textAlign = TextAlign.Center,
            )
        }

        // 译文单独占一行 —— 之前没有这一行，双语歌词会挤在原文里看不出换行
        if (line.hasTranslation) {
            Text(
                text = line.translation.orEmpty(),
                style = if (isCurrent) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = if (isCurrent) {
                    activeColor.copy(alpha = 0.8f)
                } else {
                    idleColor.copy(alpha = 0.75f)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

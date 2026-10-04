package com.musemusic45.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 列表右侧的滚动条。
 *
 * 设计要求：
 *  - **滚动条本身不显示任何字母** —— 只有一条轨道和一个细滑块
 *  - **拖动或列表滚动时**，滑块旁边浮出一个气泡，显示当前分段（A、B、#、2020 年…）
 *  - 拖动即可快速跳转到对应位置
 *
 * 位置换算全部交给 [ScrollbarMapping]，这里只负责画和收手势。
 *
 * 之所以用四个「扁平列表」参数而不是直接接 `LazyListState`：专辑页用的是
 * `LazyGridState`，两者类型不同但需要的滚动信息一样，这样两边都能复用。
 */
@Composable
fun ListScrollbar(
    firstVisibleIndex: Int,
    totalItemCount: Int,
    visibleItemCount: Int,
    sectionKeys: List<String>,
    sectionStarts: List<Int>,
    onScrollTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false,
) {
    if (totalItemCount <= 0) return

    var dragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val thumbRatio = ScrollbarMapping.thumbRatio(visibleItemCount, totalItemCount)
    val shownIndex = if (dragging) dragIndex else firstVisibleIndex
    val thumbPosition = ScrollbarMapping.ratioForIndex(shownIndex, totalItemCount)
    val bubbleLabel = sectionKeys
        .getOrNull(ScrollbarMapping.sectionIndexFor(shownIndex, sectionStarts))
        .orEmpty()

    // 气泡只在"手在动"的时候出现：拖着滚动条，或列表正在滚
    val showBubble = (dragging || isScrolling) && bubbleLabel.isNotEmpty()

    fun jumpTo(y: Float, height: Float) {
        val index = ScrollbarMapping.indexForRatio(y / height, totalItemCount)
        dragIndex = index
        onScrollTo(index)
    }

    Box(
        modifier = modifier
            .width(TOUCH_WIDTH)
            .fillMaxHeight()
            .pointerInput(totalItemCount) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        jumpTo(offset.y, size.height.toFloat())
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        jumpTo(change.position.y, size.height.toFloat())
                    },
                )
            }
            .pointerInput(totalItemCount) {
                detectTapGestures { offset ->
                    jumpTo(offset.y, size.height.toFloat())
                }
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        // 轨道：很淡，只用来提示"这里有滚动条"
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = TRACK_INSET, top = TRACK_PADDING, bottom = TRACK_PADDING)
                .width(SLIDER_WIDTH)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = TRACK_ALPHA),
                    shape = RoundedCornerShape(SLIDER_WIDTH / 2),
                ),
        )

        // 滑块位置需要像素，所以在盒子里量一次可用高度
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = TRACK_INSET, top = TRACK_PADDING, bottom = TRACK_PADDING)
                .width(SLIDER_WIDTH)
                .fillMaxHeight(),
        ) {
            val availablePx = with(density) { maxHeight.toPx() }
            val thumbPx = (availablePx * thumbRatio)
                .coerceAtLeast(with(density) { MIN_THUMB_HEIGHT.toPx() })
                .coerceAtMost(availablePx)
            val thumbDp = with(density) { thumbPx.toDp() }
            val maxOffsetPx = (availablePx - thumbPx).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, (maxOffsetPx * thumbPosition).toInt()) }
                    .width(SLIDER_WIDTH)
                    .height(thumbDp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = THUMB_ALPHA),
                        shape = RoundedCornerShape(SLIDER_WIDTH / 2),
                    ),
            )
        }

        // 气泡：拖动或滚动时出现，显示当前分段
        AnimatedVisibility(
            visible = showBubble,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = -(SLIDER_WIDTH + BUBBLE_GAP)),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp,
            ) {
                Text(
                    text = bubbleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** 触摸区宽度（比滑块宽，方便按住）。 */
private val TOUCH_WIDTH = 26.dp

/** 滑块本身的宽度。 */
private val SLIDER_WIDTH = 4.dp

/** 轨道距最右侧的距离。 */
private val TRACK_INSET = 3.dp

/** 轨道上下留白，视觉上不要顶到边界。 */
private val TRACK_PADDING = 8.dp

/** 滑块最短高度，保证还抓得住。 */
private val MIN_THUMB_HEIGHT = 28.dp

/** 气泡与滑块之间的距离。 */
private val BUBBLE_GAP = 4.dp

private const val TRACK_ALPHA = 0.12f
private const val THUMB_ALPHA = 0.7f

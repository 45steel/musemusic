package com.musemusic45.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 底部导航的一项。 */
data class FloatingNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 浮动胶囊式底部导航。
 *
 * 造型对齐常见的现代移动端做法：一整条**浮起的大圆角胶囊**，
 * 选中项用一层同色系半透明高亮托住图标。
 *
 * 这里是**纯图标**（选中项也只有高亮、不加文字）——
 * 文字标签交给 [Icon] 的 contentDescription，读屏仍然能念出名字。
 */
@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(BAR_CORNER),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = BAR_SHADOW,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BAR_MARGIN_H, vertical = BAR_MARGIN_V),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavBarEntry(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavBarEntry(
    item: FloatingNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlight by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_HIGHLIGHT_ALPHA)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
        },
        label = "navHighlight",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "navIconTint",
    )
    val highlightWidth by animateDpAsState(
        targetValue = if (selected) SELECTED_WIDTH else COLLAPSED_WIDTH,
        label = "navWidth",
    )

    Box(
        modifier = modifier.height(ENTRY_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(highlightWidth)
                .height(ENTRY_HEIGHT)
                .clip(RoundedCornerShape(ENTRY_HEIGHT / 2))
                .background(highlight)
                // 用无涟漪的 clickable：胶囊高亮本身就是反馈
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconTint,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
    }
}

/** 整条胶囊的圆角。 */
private val BAR_CORNER = 28.dp

/** 胶囊与屏幕边缘 / 底部导航条上方的留白。 */
private val BAR_MARGIN_H = 20.dp
private val BAR_MARGIN_V = 8.dp

/** 浮起程度。 */
private val BAR_SHADOW = 8.dp

private val ENTRY_HEIGHT = 48.dp
private val ICON_SIZE = 24.dp

/** 选中项高亮的宽度：比收起时宽，形成"胶囊撑开"的动效。 */
private val SELECTED_WIDTH = 64.dp
private val COLLAPSED_WIDTH = 48.dp

/** 选中项高亮的透明度。 */
private const val SELECTED_HIGHLIGHT_ALPHA = 0.18f

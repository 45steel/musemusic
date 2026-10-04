package com.musemusic45.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 可展开文本的状态。
 *
 * 抽成不依赖 Compose 运行时的数据类，是为了能单测 —— 这里的判断
 * （什么时候该显示"展开"、收起后还能不能再次展开）出错的话，
 * 表现是"名字被截断了却看不到展开按钮"，很容易被忽略。
 */
data class ExpandUiState(
    val expanded: Boolean = false,
    /** 折叠状态下文字是否真的超出了行数。 */
    val overflowed: Boolean = false,
) {
    /** 是否显示「展开 / 收起」。文字没被截断就不该出现这个入口。 */
    val showsToggle: Boolean get() = overflowed || expanded

    val label: String get() = if (expanded) "收起" else "展开"

    /** 每次排版后回调。[expanded] 为 true 时不更新 —— 展开后当然不溢出，不能因此把入口藏掉。 */
    fun onLayout(hasOverflow: Boolean): ExpandUiState =
        if (expanded) this else copy(overflowed = hasOverflow)

    fun toggle(): ExpandUiState = copy(expanded = !expanded)
}

/**
 * 名称过长时折叠显示，并提供「展开 / 收起」。
 *
 * [collapsedMaxLines] 是折叠时显示的行数；文字没超出就不显示切换入口。
 */
@Composable
fun ExpandableText(
    text: String,
    style: TextStyle,
    collapsedMaxLines: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    // 文字换了（换了专辑/歌手）就重置回折叠态
    var state by remember(text) { mutableStateOf(ExpandUiState()) }

    Column(modifier) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (state.expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> state = state.onLayout(result.hasVisualOverflow) },
        )

        if (state.showsToggle) {
            Text(
                text = state.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { state = state.toggle() },
            )
        }
    }
}

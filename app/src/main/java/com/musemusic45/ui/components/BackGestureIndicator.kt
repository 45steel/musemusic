package com.musemusic45.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 预测式返回时跟着手指的箭头指示。
 *
 * 这是照着 Google 的做法来的：手势拖动时，手指旁边浮出一个圆形小按钮，
 * 里面的箭头**指着手势前进的方向**（从左边滑出来是 `>`，从右边滑出来是 `<`）。
 *
 * 它不只是装饰 —— 窗口缩放时四周露出的背景基本对称，光看画面很难判断
 * "松手会往哪边去"，箭头把方向直接说清楚了。
 *
 * @param touchX 手指位置（窗口坐标，px）
 * @param touchY 同上
 * @param diameter 圆直径
 */
@Composable
fun BackGestureIndicator(
    touchX: Float,
    touchY: Float,
    pointsRight: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = INDICATOR_SIZE,
) {
    val half = diameter / 2
    Box(
        modifier = modifier
            // 以手指为中心：把圆挪到左上角再减掉半径
            .offset { IntOffset((touchX - half.toPx()).roundToInt(), (touchY - half.toPx()).roundToInt()) }
            .size(diameter)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (pointsRight) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(diameter * 0.6f),
        )
    }
}

/** 指示器直径，和 Google 那个圆差不多大。 */
private val INDICATOR_SIZE = 44.dp

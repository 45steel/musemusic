package com.musemusic45.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 头像配色板。颜色索引由歌手名决定，保证同一歌手每次颜色一致。 */
private val AvatarPalette = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF26A69A),
    Color(0xFFEF6C00),
    Color(0xFF8E24AA),
    Color(0xFF43A047),
    Color(0xFF00838F),
    Color(0xFFD81B60),
    Color(0xFF6D4C41),
)

/** 配色板大小。 */
const val AVATAR_PALETTE_SIZE: Int = 8

/**
 * 由歌手名算出配色板下标。
 *
 * 纯函数，不依赖 Android，方便单元测试；`String.hashCode()` 由 Java 规范固定，
 * 因此同一歌手在任何一次运行里都会得到同一个下标。
 */
fun avatarColorIndexFor(name: String): Int {
    if (name.isEmpty()) return 0
    return ((name.hashCode() % AVATAR_PALETTE_SIZE) + AVATAR_PALETTE_SIZE) % AVATAR_PALETTE_SIZE
}

/** 歌手首字圆形色块。没有头像图片时使用。 */
@Composable
fun ArtistAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val color = remember(name) { AvatarPalette[avatarColorIndexFor(name)] }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).ifEmpty { "?" },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

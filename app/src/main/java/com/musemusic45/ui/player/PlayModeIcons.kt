package com.musemusic45.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector
import com.musemusic45.data.model.PlayMode

/**
 * 播放方式对应的图标。
 *
 * 抽出来是因为**播放页底栏和选择面板必须一致** ——
 * 第十批加单曲循环时，播放页那个按钮的图标是写死的 `Repeat`，
 * 于是切到单曲循环在播放页上看不出任何变化，很容易让人以为"没生效"。
 */
fun PlayMode.icon(): ImageVector = when (this) {
    PlayMode.LIST_LOOP -> Icons.Filled.Repeat
    PlayMode.SINGLE_LOOP -> Icons.Filled.RepeatOne
    PlayMode.SHUFFLE -> Icons.Filled.Shuffle
    PlayMode.ALBUM_SHUFFLE -> Icons.Filled.Album
}

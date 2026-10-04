package com.musemusic45.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.musemusic45.data.media.MediaStoreScanner
import java.util.concurrent.ConcurrentHashMap

private const val TAG = MediaStoreScanner.TAG

/** 已经打过"加载成功"日志的专辑，避免日志刷屏。 */
private val loggedOnce = ConcurrentHashMap.newKeySet<Long>()

/**
 * 专辑封面。
 *
 * 走 MediaStore 的 albumart 通道（`content://media/external/audio/albumart/<albumId>`），
 * 由系统提供缩略图，不用自己解析内嵌封面。加载中和失败时都显示占位图。
 *
 * 尺寸由 [modifier] 决定。
 */
@Composable
fun CoverImage(
    albumId: Long,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
) {
    if (albumId <= 0L) {
        CoverPlaceholder(modifier = modifier, shape = shape)
        return
    }

    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(MediaStoreScanner.albumArtUri(albumId))
        .crossfade(true)
        .listener(
            onSuccess = { _, _ ->
                if (loggedOnce.add(albumId)) {
                    Log.i(TAG, "封面加载成功 albumId=$albumId")
                }
            },
            onError = { _, error ->
                Log.w(TAG, "封面加载失败 albumId=$albumId: ${error.throwable.message}")
            },
        )
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
        loading = { CoverPlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
        error = { CoverPlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
    )
}

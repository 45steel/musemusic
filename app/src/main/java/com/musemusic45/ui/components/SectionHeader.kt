package com.musemusic45.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 列表分段标题。
 *
 * 放在 `stickyHeader` 里时需要有**不透明**底色才能盖住滚动内容。
 * 第八批把它从 `surfaceVariant` 改成 `surface` —— 与页面底色同色，
 * 视觉上"融进"背景，更像 M3 的分段标签；靠主色小字做区分就够了。
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
    }
}

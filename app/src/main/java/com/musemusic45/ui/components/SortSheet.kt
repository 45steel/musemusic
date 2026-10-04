package com.musemusic45.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.SortField
import com.musemusic45.data.model.SortOrder
import com.musemusic45.data.model.SortSpec

/**
 * 排序面板。
 *
 * 交互（对照设计稿）：
 *  - 点某一行 → 选中该字段
 *  - 点选中行右侧的 ⇅ → 切换升序/降序
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    current: SortSpec,
    fields: List<SortField>,
    onFieldSelected: (SortField) -> Unit,
    onToggleOrder: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            fields.forEach { field ->
                val selected = field == current.field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFieldSelected(field) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { onFieldSelected(field) })
                    Text(
                        text = field.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Text(
                            text = orderHint(current.order),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(onClick = onToggleOrder) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "切换升降序",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

private fun orderHint(order: SortOrder): String =
    if (order == SortOrder.ASCENDING) "升序" else "降序"

/** 面板高度占位，避免最后一项贴着屏幕底边。 */
@Composable
private fun SheetBottomSpacer() {
    Spacer(Modifier.height(24.dp))
}

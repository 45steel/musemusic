package com.localmusic.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 设置页：重新扫描、文件夹管理、统计信息、关于。
 */
@Composable
fun SettingsScreen(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    folders: List<String>,
    onlyFolders: Boolean,
    isScanning: Boolean,
    lastScanMillis: Long,
    versionName: String,
    onRescan: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onToggleOnlyFolders: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionTitle("音乐库")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "共 $songCount 首歌 · $albumCount 张专辑 · $artistCount 位歌手",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (lastScanMillis > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "上次扫描用时 ${lastScanMillis}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRescan,
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isScanning) "正在扫描…" else "重新扫描")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("文件夹")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "添加目录后可以只看这些目录里的音乐。App 只读取，不会改动任何文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            if (folders.isEmpty()) {
                Text(
                    text = "还没有添加任何文件夹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                folders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = folder,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveFolder(folder) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "移除 $folder",
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加文件夹")
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "仅显示我添加的文件夹",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = onlyFolders,
                    onCheckedChange = onToggleOnlyFolders,
                    enabled = folders.isNotEmpty(),
                )
            }
            if (folders.isEmpty()) {
                Text(
                    text = "先添加一个文件夹才能打开这个开关",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("关于")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("本地音乐 $versionName", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "纯本地播放器，完全不联网。App 只读取音频文件，不修改、不移动、不删除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

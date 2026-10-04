package com.musemusic45.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musemusic45.data.model.ArtistParsingConfig

/**
 * 设置页：重新扫描、文件夹管理、歌手归类、后台行为、手动移除、关于。
 */
@OptIn(ExperimentalLayoutApi::class)
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
    splitArtists: Boolean,
    ignoreArtistParens: Boolean,
    extraArtistSeparators: String,
    stopOnTaskRemoved: Boolean,
    hiddenCount: Int,
    predictiveBack: Boolean,
    onRescan: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onToggleOnlyFolders: (Boolean) -> Unit,
    onToggleSplitArtists: (Boolean) -> Unit,
    onToggleIgnoreArtistParens: (Boolean) -> Unit,
    onAddArtistSeparators: (String) -> Unit,
    onRemoveArtistSeparator: (Char) -> Unit,
    onToggleStopOnTaskRemoved: (Boolean) -> Unit,
    onUnhideAllSongs: () -> Unit,
    onTogglePredictiveBack: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 输入框只用于"添加"，添加完就清空，所以不需要跟 DataStore 同步
    var separatorDraft by remember { mutableStateOf("") }

    // 实时预览：让这几条规则一眼看懂
    val previewNames = remember(extraArtistSeparators, splitArtists, ignoreArtistParens) {
        ArtistParsingConfig(splitArtists, ignoreArtistParens, extraArtistSeparators)
            .names(SEPARATOR_PREVIEW_INPUT)
    }

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

        SectionTitle("歌手归类")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "这些规则决定「歌手」页怎么把歌归到歌手名下。改动后立即生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))

            SwitchRow(
                title = "拆分多位歌手",
                subtitle = "「周杰伦、费玉清」分别归到两位歌手名下；关掉后整串算一位",
                checked = splitArtists,
                onCheckedChange = onToggleSplitArtists,
            )
            SwitchRow(
                title = "忽略歌手名里的括号",
                subtitle = "「某某（xxx）」当作「某某」",
                checked = ignoreArtistParens,
                onCheckedChange = onToggleIgnoreArtistParens,
            )

            Spacer(Modifier.height(12.dp))
            Text("额外分隔符", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "默认已支持 ${ArtistParsingConfig.DEFAULT_SEPARATORS}（顿号、分号、斜杠）。" +
                    "可以在这里再添加，不会覆盖默认的。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))

            if (extraArtistSeparators.isEmpty()) {
                Text(
                    text = "还没添加任何额外分隔符",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    extraArtistSeparators.forEach { separator ->
                        InputChip(
                            selected = false,
                            onClick = { onRemoveArtistSeparator(separator) },
                            enabled = splitArtists,
                            label = { Text(separator.toString()) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "移除分隔符 $separator",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = separatorDraft,
                    onValueChange = { separatorDraft = it },
                    enabled = splitArtists,
                    singleLine = true,
                    label = { Text("添加分隔符") },
                    placeholder = { Text("例如 &") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onAddArtistSeparators(separatorDraft)
                        separatorDraft = ""
                    },
                    enabled = splitArtists && separatorDraft.isNotEmpty(),
                ) {
                    Text("添加")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "例：「$SEPARATOR_PREVIEW_INPUT」\n→ " + previewNames.joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("后台播放")
        Column(Modifier.padding(horizontal = 16.dp)) {
            SwitchRow(
                title = "划掉最近任务后停止播放",
                subtitle = "默认关闭：从最近任务列表划掉 App 后音乐继续播放。" +
                    "打开后划掉就会停止播放并结束后台服务。",
                checked = stopOnTaskRemoved,
                onCheckedChange = onToggleStopOnTaskRemoved,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("操作手感")
        Column(Modifier.padding(horizontal = 16.dp)) {
            SwitchRow(
                title = "预测式返回动画",
                subtitle = "Google 那套：返回手势拖动时，当前页跟着手指缩小让开、" +
                    "露出上一页，从哪边滑就往哪边退，松手才真正返回。" +
                    "关掉则返回直接跳转，没有预览也没有过渡。需要 Android 13 及以上。",
                checked = predictiveBack,
                onCheckedChange = onTogglePredictiveBack,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("手动移除的歌曲")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "长按任意一首歌可以把它从 App 里移除，用来挡掉扫描到的不需要的音频。" +
                    "移除只影响显示和播放列表，不会删除或修改文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            if (hiddenCount > 0) {
                Text(
                    text = "已移除 $hiddenCount 首歌",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUnhideAllSongs,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("全部恢复")
                }
            } else {
                Text(
                    text = "还没有移除任何歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        SectionTitle("关于")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("缪斯音乐 $versionName", style = MaterialTheme.typography.bodyMedium)
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

/** 一行「标题 + 说明 + 开关」。 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 分隔符预览用的样例：故意用一个默认分隔符之外的字符，方便看出"添加"的效果。 */
private const val SEPARATOR_PREVIEW_INPUT = "周杰伦&费玉清（合唱）"

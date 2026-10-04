package com.localmusic.player.data.media

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * 把 SAF 的目录树 URI 还原成系统绝对路径。
 *
 * 用「文件」App 选出来的目录是 `content://com.android.externalstorage.documents/tree/...`，
 * 它的 documentId 形如 `primary:Music/我的歌`，可以还原成 `/storage/emulated/0/Music/我的歌`。
 *
 * 纯字符串处理 + Environment，便于单元测试核心的解析逻辑（见 [documentIdToPath]）。
 */
object FolderPaths {

    /** 解析失败返回 null（例如第三方文档提供者没有对应的文件系统路径）。 */
    fun treeUriToPath(context: Context, treeUri: Uri): String? {
        val documentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return null

        val externalRoot = Environment.getExternalStorageDirectory().absolutePath
        return documentIdToPath(documentId, externalRoot)
    }

    /**
     * documentId → 绝对路径。
     *
     * - `primary:Music/foo` → `<externalRoot>/Music/foo`
     * - `1A2B-3C4D:Music/foo` → `/storage/1A2B-3C4D/Music/foo`
     * - `primary:` → `<externalRoot>`
     * - 没有冒号的非法 id → null
     */
    fun documentIdToPath(documentId: String, externalRoot: String): String? {
        if (documentId.isBlank()) return null
        val separator = documentId.indexOf(':')
        if (separator <= 0) return null

        val volume = documentId.substring(0, separator)
        val relative = documentId.substring(separator + 1).trim('/')

        val base = if (volume.equals("primary", ignoreCase = true)) {
            externalRoot
        } else {
            "/storage/$volume"
        }
        return if (relative.isEmpty()) base else "$base/$relative"
    }

    /** 判断歌曲路径是否落在某个已添加的文件夹里（含子目录）。 */
    fun isInside(songPath: String, folderPath: String): Boolean {
        if (songPath.isBlank() || folderPath.isBlank()) return false
        val normalizedFolder = folderPath.trimEnd('/')
        if (songPath == normalizedFolder) return true
        return songPath.startsWith("$normalizedFolder/")
    }

    /** 歌曲是否落在任意一个已添加的文件夹里。 */
    fun isInsideAny(songPath: String, folderPaths: Collection<String>): Boolean =
        folderPaths.any { isInside(songPath, it) }
}

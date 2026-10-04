package com.localmusic.player.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderPathsTest {

    private val root = "/storage/emulated/0"

    @Test
    fun `主存储的 documentId 还原成绝对路径`() {
        assertEquals("$root/Music", FolderPaths.documentIdToPath("primary:Music", root))
        assertEquals("$root/Music/我的歌", FolderPaths.documentIdToPath("primary:Music/我的歌", root))
    }

    @Test
    fun `主存储根目录`() {
        assertEquals(root, FolderPaths.documentIdToPath("primary:", root))
    }

    @Test
    fun `SD 卡等其它卷`() {
        assertEquals("/storage/1A2B-3C4D/Music", FolderPaths.documentIdToPath("1A2B-3C4D:Music", root))
    }

    @Test
    fun `前后斜杠会被规整`() {
        assertEquals("$root/Music", FolderPaths.documentIdToPath("primary:/Music/", root))
    }

    @Test
    fun `非法 id 返回 null`() {
        assertNull(FolderPaths.documentIdToPath("", root))
        assertNull(FolderPaths.documentIdToPath("没有冒号", root))
        assertNull(FolderPaths.documentIdToPath(":前面的卷名是空的", root))
    }

    // ---------------------------------------------------------- 归属判断

    @Test
    fun `歌曲在文件夹内`() {
        assertTrue(FolderPaths.isInside("$root/Music/a.mp3", "$root/Music"))
        assertTrue(FolderPaths.isInside("$root/Music/子目录/a.mp3", "$root/Music"))
    }

    @Test
    fun `歌曲不在文件夹内`() {
        assertFalse(FolderPaths.isInside("$root/Music2/a.mp3", "$root/Music"))
        assertFalse(FolderPaths.isInside("$root/Other/a.mp3", "$root/Music"))
    }

    @Test
    fun `前缀相同但目录不同不算在内`() {
        // 这是关键：/Music 不应当把 /MusicBackup 也算进来
        assertFalse(FolderPaths.isInside("$root/MusicBackup/a.mp3", "$root/Music"))
    }

    @Test
    fun `文件夹路径结尾有斜杠也能判断`() {
        assertTrue(FolderPaths.isInside("$root/Music/a.mp3", "$root/Music/"))
    }

    @Test
    fun `空路径一律不算在内`() {
        assertFalse(FolderPaths.isInside("", "$root/Music"))
        assertFalse(FolderPaths.isInside("$root/Music/a.mp3", ""))
    }

    @Test
    fun `任意一个文件夹命中即可`() {
        val folders = listOf("$root/Music", "$root/Download/歌")
        assertTrue(FolderPaths.isInsideAny("$root/Music/a.mp3", folders))
        assertTrue(FolderPaths.isInsideAny("$root/Download/歌/b.mp3", folders))
        assertFalse(FolderPaths.isInsideAny("$root/Other/c.mp3", folders))
        assertFalse(FolderPaths.isInsideAny("$root/Music/a.mp3", emptyList()))
    }
}

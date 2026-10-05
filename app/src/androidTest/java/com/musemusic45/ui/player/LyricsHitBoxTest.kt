package com.musemusic45.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.musemusic45.data.media.LrcDocument
import com.musemusic45.data.media.LrcLine
import com.musemusic45.data.media.LyricsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 歌词行的点击热区。
 *
 * 这里测的是一个**真实缺陷**：歌词行的热区原本写成
 * `.fillMaxWidth().clickable().padding()` —— 于是每一行的可点区域是
 * **整屏宽 × 112px**，而一行纯日文原文只有约 559×35。
 * 结果"点空白处返回封面"极难点中，几乎总会误触到某一行歌词触发跳转。
 *
 * 修法是把顺序改成 `.padding().clickable()` 并去掉 `fillMaxWidth`，
 * 热区于是严格等于文字本身的布局范围。
 *
 * 这种命中框属于 Compose 的布局行为，纯 JVM 单测测不到 ——
 * 只能在设备上跑：
 *
 *     ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class LyricsHitBoxTest {

    @get:Rule
    val rule = createComposeRule()

    /** 窄行（短歌词）：横向留白最明显，是复现原缺陷的最佳样本。 */
    private val shortLine = "「ここだよ」って笑顔はずるい"

    /** 宽行：占满可用宽度，用来确认长歌词没有被意外截断。 */
    private val longLine = "予想出来ない一日と君はとても似ている"

    private val lines = listOf(
        LrcLine(timeMs = 0L, text = longLine),
        LrcLine(timeMs = 10_000L, text = shortLine),
        LrcLine(timeMs = 20_000L, text = "全速力だよね毎回 なんて楽しんでいる"),
    )

    private fun setContent(onSeek: (Long) -> Unit, onExit: () -> Unit) {
        rule.setContent {
            MaterialTheme {
                LyricsPane(
                    state = LyricsState.Synced(LrcDocument(lines)),
                    positionMs = 12_000L,
                    onSeek = onSeek,
                    onExitLyrics = onExit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /** 可点节点的边界（clickable 会把子节点的语义合并上来，所以这里拿到的就是热区）。 */
    private fun hitBox(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    /** 文字本身的边界 —— 必须用未合并树，否则拿到的还是外层热区。 */
    private fun textBox(text: String) =
        rule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    // ---------------------------------------------------------------- 热区几何

    @Test
    fun 热区宽度等于文字宽度而不是整屏宽() {
        setContent({}, {})
        val hit = hitBox(shortLine)
        val text = textBox(shortLine)
        assertEquals(
            "热区左边界必须贴住文字左边界",
            text.left.toDouble(), hit.left.toDouble(), 1.0,
        )
        assertEquals(
            "热区右边界必须贴住文字右边界",
            text.right.toDouble(), hit.right.toDouble(), 1.0,
        )
    }

    @Test
    fun `窄行的热区明显窄于屏幕`() {
        setContent({}, {})
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val hit = hitBox(shortLine)
        // 原来这里是整屏宽，只比屏幕窄 70px。现在应当窄得多。
        assertTrue(
            "热区宽 ${hit.width} 太接近屏幕宽 ${root.width}，说明又写回了 fillMaxWidth",
            hit.width < root.width - 200f,
        )
    }

    @Test
    fun `长行的热区同样被限制在文字范围内`() {
        setContent({}, {})
        val hit = hitBox(longLine)
        val text = textBox(longLine)
        assertEquals("长行热区宽度必须等于文字宽度", text.width.toDouble(), hit.width.toDouble(), 1.0)
    }

    @Test
    fun `热区不含行间距`() {
        setContent({}, {})
        val hit = hitBox(shortLine)
        val text = textBox(shortLine)
        // 原来 clickable 在 padding 之前，上下各多出 12dp（本机约 31px）
        assertTrue(
            "热区高 ${hit.height} 比文字高 ${text.height} 多出太多，说明行间距被算进热区了",
            hit.height - text.height < 20f,
        )
    }

    // ---------------------------------------------------------------- 行为

    @Test
    fun `点歌词文字会跳转且不返回封面`() {
        var seeked = -1L
        var exited = false
        setContent({ seeked = it }, { exited = true })

        val hit = hitBox(shortLine)
        rule.onRoot().performTouchInput { click(hit.center) }
        rule.waitForIdle()

        assertEquals("点文字应当跳到这一行的时间点", 10_000L, seeked)
        assertEquals("点文字不应该返回封面", false, exited)
    }

    @Test
    fun `点歌词左侧的空白会返回封面而不是跳转`() {
        var seeked = -1L
        var exited = false
        setContent({ seeked = it }, { exited = true })

        val hit = hitBox(shortLine)
        // 与歌词同一高度、但在文字左边界之外 —— 这正是原来会误触的位置
        val blank = Offset(hit.left - 30f, hit.center.y)
        rule.onRoot().performTouchInput { click(blank) }
        rule.waitForIdle()

        assertEquals("点左侧空白不应该跳转", -1L, seeked)
        assertEquals("点左侧空白应当返回封面", true, exited)
    }

    @Test
    fun `点歌词右侧的空白会返回封面而不是跳转`() {
        var seeked = -1L
        var exited = false
        setContent({ seeked = it }, { exited = true })

        val hit = hitBox(shortLine)
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val blank = Offset(hit.right + 30f, hit.center.y)
        assertTrue("这个测试点必须还在屏幕内", blank.x < root.right)

        rule.onRoot().performTouchInput { click(blank) }
        rule.waitForIdle()

        assertEquals("点右侧空白不应该跳转", -1L, seeked)
        assertEquals("点右侧空白应当返回封面", true, exited)
    }
}

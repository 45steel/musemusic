package com.musemusic45.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「展开 / 收起」的状态机。
 *
 * 出错的表现是"名称被截断了却看不到展开按钮"，很容易被忽略，所以钉住。
 */
class ExpandUiStateTest {

    @Test
    fun `初始是折叠且不显示切换入口`() {
        val state = ExpandUiState()
        assertFalse(state.expanded)
        assertFalse(state.showsToggle)
        assertEquals("展开", state.label)
    }

    @Test
    fun `文字没超出时不显示切换入口`() {
        val state = ExpandUiState().onLayout(hasOverflow = false)
        assertFalse(state.showsToggle)
    }

    @Test
    fun `文字超出时显示展开`() {
        val state = ExpandUiState().onLayout(hasOverflow = true)
        assertTrue(state.showsToggle)
        assertEquals("展开", state.label)
    }

    @Test
    fun `点开后变成收起且文字完全展开`() {
        val state = ExpandUiState().onLayout(hasOverflow = true).toggle()
        assertTrue(state.expanded)
        assertEquals("收起", state.label)
    }

    @Test
    fun `展开后再点一次回到折叠`() {
        val state = ExpandUiState().onLayout(hasOverflow = true).toggle().toggle()
        assertFalse(state.expanded)
        assertEquals("展开", state.label)
    }

    @Test
    fun `展开后的排版回调不会把切换入口藏掉`() {
        // 展开后 maxLines 无上限，hasVisualOverflow 会变 false；
        // 如果这时把 overflowed 覆盖掉，"收起"按钮就消失了，用户再也收不回去。
        val expanded = ExpandUiState().onLayout(hasOverflow = true).toggle()
        val afterLayout = expanded.onLayout(hasOverflow = false)
        assertTrue("展开后切换入口不能消失", afterLayout.showsToggle)
        assertEquals("收起", afterLayout.label)
    }

    @Test
    fun `折叠状态下排版回调会更新溢出标记`() {
        val state = ExpandUiState().onLayout(hasOverflow = true).onLayout(hasOverflow = false)
        assertFalse(state.showsToggle)
    }
}

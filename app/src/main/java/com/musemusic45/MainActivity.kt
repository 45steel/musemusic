package com.musemusic45

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musemusic45.data.model.NameSortKey
import com.musemusic45.data.search.PinyinProvider
import com.musemusic45.ui.AppRoot
import com.musemusic45.ui.theme.MuseMusicTheme

/**
 * 单 Activity 架构：所有界面都是 Compose，没有 Fragment、没有 XML 布局。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 排序键必须在**任何排序发生之前**装好，否则第一次排出来的顺序是错的
        // （首帧的 remember 块会立刻用到它）。
        //
        // ICU 转写器（汉字→拼音、假名→罗马字）要 Android 10 起才有；
        // 更早的系统退回系统 Collator —— 拿不到确定的 A-Z，但中文仍是拼音序。
        val pinyin = PinyinProvider()
        if (pinyin.available) {
            NameSortKey.install { text -> pinyin.toLatin(text) }
        } else {
            NameSortKey.installCollator()
        }

        enableEdgeToEdge()
        setContent {
            MuseMusicTheme {
                AppRoot()
            }
        }
    }
}

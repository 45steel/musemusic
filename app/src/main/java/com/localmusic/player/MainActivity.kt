package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localmusic.player.ui.AppRoot
import com.localmusic.player.ui.theme.LocalMusicTheme

/**
 * 单 Activity 架构：所有界面都是 Compose，没有 Fragment、没有 XML 布局。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalMusicTheme {
                AppRoot()
            }
        }
    }
}

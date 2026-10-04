package com.musemusic45

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musemusic45.ui.AppRoot
import com.musemusic45.ui.theme.MuseMusicTheme

/**
 * 单 Activity 架构：所有界面都是 Compose，没有 Fragment、没有 XML 布局。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseMusicTheme {
                AppRoot()
            }
        }
    }
}

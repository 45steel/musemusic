package com.musemusic45.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musemusic45.MainActivity
import com.musemusic45.R
import com.musemusic45.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 后台播放服务。
 *
 * Android 14（API 34）起前台服务必须声明具体类型，媒体播放用 `mediaPlayback`，
 * 并且要在清单里申请 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限，否则会被系统直接杀掉。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 划掉最近任务后是否停止播放。
     *
     * `onTaskRemoved` 不是挂起函数，读不了 DataStore，所以这里开一个收集协程
     * 把值缓存下来 —— 在回调里同步读一个 volatile 字段，不会阻塞主线程。
     */
    @Volatile
    private var stopOnTaskRemoved: Boolean = false

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // 耳机拔出时自动暂停
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()

        // 通知栏小图标：必须换成单色剪影，用应用图标会显示成一坨白方块
        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_notification_music)
        setMediaNotificationProvider(notificationProvider)

        val settings = SettingsStore(this)
        scope.launch {
            settings.stopOnTaskRemoved.collect { enabled ->
                stopOnTaskRemoved = enabled
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * 用户从最近任务里划掉 App。
     *
     * 默认**继续播放** —— 划掉界面通常只是想关掉窗口，音乐不该跟着断。
     * 在设置里打开「划掉后停止播放」才会停。
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (stopOnTaskRemoved) {
            Log.i(TAG, "从最近任务移除：按设置停止播放")
            mediaSession?.player?.stop()
            stopSelf()
        } else {
            Log.i(TAG, "从最近任务移除：按设置继续播放")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val TAG = "MuseMusic"
    }
}

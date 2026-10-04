package com.musemusic45.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musemusic45.data.media.MediaStoreScanner
import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song
import com.musemusic45.data.repository.LibraryAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 界面侧对播放服务的代理。
 *
 * 除了包装 [MediaController]，这里还实现了三种播放方式的推进规则，
 * 其中「按专辑播放」的轮次由 [AlbumRoundPlanner] 负责。
 */
class PlaybackController(private val context: Context) {

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 当前队列里的歌曲，用来把播放项映射回 Song。 */
    private var queueSongs: List<Song> = emptyList()
    private var currentMode: PlayMode = PlayMode.LIST_LOOP

    /** 专辑 ID → 该专辑的曲目（已按碟号、音轨号排好序）。 */
    private var albumTracks: Map<Long, List<Song>> = emptyMap()

    /** 完整音乐库。从「按专辑播放」切回列表/随机时用它恢复队列。 */
    private var allSongs: List<Song> = emptyList()

    private val planner = AlbumRoundPlanner()

    /** 防止 STATE_ENDED 重入导致连跳两张专辑。 */
    private var advancingAlbum = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && currentMode == PlayMode.ALBUM_SHUFFLE) {
                advanceToNextAlbum()
            }
        }
    }

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val connected = future.awaitController()
        connected.addListener(listener)
        controller = connected
        syncFromPlayer()
        startTicker()
        Log.i(TAG, "已连接播放服务")
    }

    fun release() {
        tickerJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }

    /**
     * 把整个音乐库交给控制器，用于「按专辑播放」时按专辑取曲目。
     * 每次扫描完成后调用。
     */
    fun configureLibrary(songs: List<Song>) {
        allSongs = songs
        albumTracks = songs
            .groupBy { it.albumId }
            .mapValues { (_, group) -> LibraryAggregator.sortAlbumTracks(group) }
        planner.reset(albumTracks.keys.toList())
        Log.i(TAG, "按专辑播放已就绪: ${albumTracks.size} 张专辑")
        syncFromPlayer()
    }

    // ------------------------------------------------------------ 界面操作

    /**
     * 用给定列表作为队列，从 [startIndex] 开始播放。
     *
     * 注意「按专辑播放」下的特殊规则：手动点歌时，那首歌**所在专辑**成为当前专辑，
     * 从这首歌开始放，放完这张专辑后继续轮次 —— 队列不是点击时所在的列表。
     */
    fun playSongs(songs: List<Song>, startIndex: Int) {
        val player = controller ?: return
        if (songs.isEmpty()) return

        val safeIndex = clampIndex(startIndex, songs.size)
        val startSong = songs.getOrNull(safeIndex)

        if (currentMode == PlayMode.ALBUM_SHUFFLE && startSong != null) {
            Log.i(TAG, "按专辑播放下手动点歌: ${startSong.title} → 专辑 ${startSong.album}")
            startAlbum(startSong.albumId, startSong.id)
            return
        }

        queueSongs = songs
        player.setMediaItems(songs.map { it.toMediaItem() }, safeIndex, 0L)
        applyModeToPlayer()
        player.prepare()
        player.play()
        syncFromPlayer()
        Log.i(TAG, "开始播放: 队列=${songs.size} 首, 起始下标=$safeIndex, 模式=$currentMode")
    }

    /**
     * 切换播放方式。**不打断当前播放** —— 当前歌与播放位置都会保留。
     *
     * 第二版修复：第一版切到「按专辑播放」时调用 `startAlbum`，而它的起始位置写死为 0，
     * 导致当前歌从头重播；从专辑模式切回列表/随机时队列也没换回完整音乐库，
     * 会变成"单张专辑无限循环"。
     */
    fun setMode(mode: PlayMode) {
        if (mode == currentMode) return
        val player = controller ?: return

        val previousMode = currentMode
        val current = _state.value.currentSong
        val position = ModeSwitch.resumePosition(player.currentPosition)
        val wasPlaying = player.isPlaying

        currentMode = mode
        Log.i(TAG, "播放方式: $previousMode → $mode（保留位置 ${position}ms, 是否在播=$wasPlaying）")

        when {
            // 切到按专辑播放：当前歌所在专辑成为当前专辑，从原位置继续
            ModeSwitch.needsAlbumQueue(mode) -> {
                if (planner.roundTotal == 0) {
                    planner.reset(albumTracks.keys.toList())
                }
                if (current != null) {
                    startAlbum(
                        albumId = current.albumId,
                        startSongId = current.id,
                        startPositionMs = position,
                        keepPlaying = wasPlaying,
                    )
                } else {
                    val first = planner.nextAlbum()
                    if (first != null) {
                        startAlbum(first, keepPlaying = wasPlaying)
                    } else {
                        syncFromPlayer()
                    }
                }
            }

            // 离开按专辑播放：队列换回完整音乐库，否则会围着那一张专辑转
            ModeSwitch.needsFullQueueRestore(previousMode, mode) &&
                allSongs.isNotEmpty() && current != null -> {
                val index = ModeSwitch.indexInFullQueue(allSongs, current.id)
                queueSongs = allSongs
                player.setMediaItems(allSongs.map { it.toMediaItem() }, index, position)
                applyModeToPlayer()
                player.prepare()
                if (wasPlaying) player.play()
                syncFromPlayer()
                Log.i(TAG, "退出按专辑播放: 队列恢复为全部 ${allSongs.size} 首, 下标 $index")
            }

            else -> {
                applyModeToPlayer()
                syncFromPlayer()
            }
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
        syncFromPlayer()
    }

    fun next() {
        val player = controller ?: return
        if (currentMode == PlayMode.ALBUM_SHUFFLE) {
            // 按专辑播放时「下一首」仍在本专辑内推进；
            // 只有整张放完（STATE_ENDED）才换专辑。
            if (player.currentMediaItemIndex >= player.mediaItemCount - 1) {
                advanceToNextAlbum()
                return
            }
        }
        player.seekToNextMediaItem()
        syncFromPlayer()
    }

    /**
     * 上一首。
     *
     * 按专辑播放时，当前专辑的第一首按「上一首」**停在当前歌开头**，
     * 不切专辑、也不回上一张已播专辑。
     */
    fun previous() {
        val player = controller ?: return
        if (currentMode == PlayMode.ALBUM_SHUFFLE && player.currentMediaItemIndex == 0) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
        syncFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val safe = positionMs.coerceAtLeast(0L)
        player.seekTo(safe)
        _state.update { it.copy(positionMs = safe) }
    }

    fun skipToQueueIndex(index: Int) {
        val player = controller ?: return
        val safe = clampIndex(index, player.mediaItemCount)
        if (safe < 0) return
        player.seekTo(safe, 0L)
        player.play()
        syncFromPlayer()
    }

    fun clearQueue() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
        queueSongs = emptyList()
        syncFromPlayer()
    }

    // ------------------------------------------------------------ 内部实现

    /**
     * 把某张专辑装进队列并从指定曲目开始播放。
     *
     * [startPositionMs] 用于切换播放方式时**保留当前播放位置**，不从头重播。
     * [keepPlaying] 为 false 时只装载不自动播放（切模式时本来就处于暂停状态）。
     */
    private fun startAlbum(
        albumId: Long,
        startSongId: Long? = null,
        fromBeginning: Boolean = false,
        startPositionMs: Long = 0L,
        keepPlaying: Boolean = true,
    ) {
        val player = controller ?: return
        val tracks = albumTracks[albumId].orEmpty()
        if (tracks.isEmpty()) {
            Log.w(TAG, "专辑 $albumId 没有曲目，跳过")
            return
        }

        planner.setCurrentAlbum(albumId)
        queueSongs = tracks

        val startIndex = if (fromBeginning || startSongId == null) {
            0
        } else {
            tracks.indexOfFirst { it.id == startSongId }.takeIf { it >= 0 } ?: 0
        }

        player.setMediaItems(
            tracks.map { it.toMediaItem() },
            startIndex,
            ModeSwitch.resumePosition(startPositionMs),
        )
        applyModeToPlayer()
        player.prepare()
        if (keepPlaying) player.play()
        syncFromPlayer()

        Log.i(
            TAG,
            "按专辑播放: 专辑=$albumId 曲目=${tracks.size} 起始=$startIndex " +
                "位置=${startPositionMs}ms 本轮第 ${planner.played}/${planner.roundTotal} 张, " +
                "剩余 ${planner.remaining} 张",
        )
    }

    /** 当前专辑播完，抽取下一张。 */
    private fun advanceToNextAlbum() {
        if (advancingAlbum) return
        advancingAlbum = true
        try {
            val next = planner.nextAlbum()
            if (next == null) {
                Log.w(TAG, "按专辑播放：没有可播放的专辑")
                return
            }
            startAlbum(next, fromBeginning = true)
        } finally {
            advancingAlbum = false
        }
    }

    private fun applyModeToPlayer() {
        val player = controller ?: return
        when (currentMode) {
            PlayMode.LIST_LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }

            PlayMode.SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }

            // 按专辑播放必须关掉循环：单专辑队列如果自己转圈，
            // 就永远播不完，也就永远触发不了"换下一张专辑"。
            PlayMode.ALBUM_SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
        }
    }

    private fun syncFromPlayer() {
        val player = controller ?: return
        val index = player.currentMediaItemIndex
        val song = queueSongs.getOrNull(index)
        val duration = if (player.duration > 0L) player.duration else (song?.durationMs ?: 0L)
        val inAlbumMode = currentMode == PlayMode.ALBUM_SHUFFLE

        _state.update {
            it.copy(
                isConnected = true,
                currentSong = song,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                queue = queueSongs,
                queueIndex = index,
                mode = currentMode,
                albumRoundIndex = if (inAlbumMode) planner.played else 0,
                albumRoundTotal = if (inAlbumMode) planner.roundTotal else 0,
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = if (player.duration > 0L) player.duration else it.durationMs,
                            isPlaying = true,
                        )
                    }
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private suspend fun ListenableFuture<MediaController>.awaitController(): MediaController =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

    private companion object {
        const val TAG = MediaStoreScanner.TAG
        const val POSITION_TICK_MS = 500L
    }
}

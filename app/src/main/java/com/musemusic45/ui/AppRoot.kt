package com.musemusic45.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.BackEventCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musemusic45.BuildConfig
import com.musemusic45.R
import com.musemusic45.data.media.FolderPaths
import com.musemusic45.data.model.PlayMode
import com.musemusic45.data.model.Song
import com.musemusic45.data.model.SortSpec
import com.musemusic45.data.model.SortTarget
import com.musemusic45.data.repository.LibraryAggregator
import com.musemusic45.data.repository.LibrarySorter
import com.musemusic45.data.repository.ListSections
import com.musemusic45.data.search.PinyinProvider
import com.musemusic45.permission.AudioPermissions
import com.musemusic45.ui.albums.AlbumDetailScreen
import com.musemusic45.ui.albums.AlbumsScreen
import com.musemusic45.ui.artists.ArtistDetailScreen
import com.musemusic45.ui.artists.ArtistsScreen
import com.musemusic45.ui.components.EmptyLibraryScreen
import com.musemusic45.ui.components.FloatingNavBar
import com.musemusic45.ui.components.FloatingNavItem
import com.musemusic45.ui.components.MiniPlayer
import com.musemusic45.ui.components.PermissionScreen
import com.musemusic45.ui.components.SortSheet
import com.musemusic45.ui.nav.Routes
import com.musemusic45.ui.player.PlayerSheet
import com.musemusic45.ui.player.PlayModeSheet
import com.musemusic45.ui.player.QueueSheet
import com.musemusic45.ui.search.SearchScreen
import com.musemusic45.ui.settings.SettingsScreen
import com.musemusic45.ui.songs.SongsScreen
import com.musemusic45.ui.theme.AppShapes
import com.musemusic45.ui.theme.formatRoundLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** 底部导航的三个一级页。 */
enum class MainTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Songs(Routes.SONGS, R.string.tab_songs, Icons.Filled.MusicNote),
    Albums(Routes.ALBUMS, R.string.tab_albums, Icons.Filled.Album),
    Artists(Routes.ARTISTS, R.string.tab_artists, Icons.Filled.Person),
}

/**
 * 应用根界面：底部导航 + 全局迷你播放器 + 播放页覆盖层 + 排序面板。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val library by libraryViewModel.state.collectAsStateWithLifecycle()
    val sorts by libraryViewModel.sorts.collectAsStateWithLifecycle()
    val searchIndex by libraryViewModel.searchIndex.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()
    val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
    val folders by libraryViewModel.folders.collectAsStateWithLifecycle()
    val onlyFolders by libraryViewModel.onlyFolders.collectAsStateWithLifecycle()
    val artistConfig by libraryViewModel.artistConfig.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by libraryViewModel.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val predictiveBackEnabled by libraryViewModel.predictiveBack.collectAsStateWithLifecycle()
    val hiddenCount by libraryViewModel.hiddenCount.collectAsStateWithLifecycle()

    var permissionGranted by remember { mutableStateOf(AudioPermissions.hasPermission(context)) }
    var playerExpanded by remember { mutableStateOf(false) }
    var backPressedOnce by remember { mutableStateOf(false) }
    var sortSheetTarget by remember { mutableStateOf<SortTarget?>(null) }
    var playModeSheetVisible by remember { mutableStateOf(false) }
    var queueSheetVisible by remember { mutableStateOf(false) }

    /** 长按要移除的歌曲，非空时弹确认框。 */
    var pendingRemove by remember { mutableStateOf<Song?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val grantedNow = AudioPermissions.isGranted(granted)
        permissionGranted = grantedNow
        if (grantedNow) libraryViewModel.refresh()
    }

    // 通知权限：被拒绝也不影响播放，只是通知栏不显示
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (permissionGranted) libraryViewModel.refresh()
    }

    // 扫描完成后把音乐库交给播放引擎，「按专辑播放」需要按专辑取曲目
    LaunchedEffect(library.songs) {
        if (library.songs.isNotEmpty()) playerViewModel.configureLibrary(library.songs)
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // 文件夹选择器（SAF）。选完把目录树 URI 还原成绝对路径存起来。
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val path = FolderPaths.treeUriToPath(context, uri)
            if (path != null) {
                libraryViewModel.addFolder(path)
                Log.i("MuseMusic", "已添加文件夹: $path")
            } else {
                toast(context, "无法解析这个目录的路径，请换一个目录")
            }
        }
    }

    if (!permissionGranted) {
        PermissionScreen(onRequest = { permissionLauncher.launch(AudioPermissions.required) })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.SONGS

    /**
     * 导航过渡进行中。
     *
     * 过渡期间旧页面仍然在组合里、仍然能收到点击 —— 从专辑/歌手详情返回时
     * 快速点一下就会误触播放。第七批的做法是把动画压到 180ms 躲避，
     * 这一批改成**保留动画 + 过渡期间盖一层吃掉所有点击的遮罩**。
     */
    var navTransitioning by remember { mutableStateOf(false) }

    /**
     * 预测式返回：手势进度（0..1）与**从哪一侧滑的**。
     *
     * 方向必须跟手：从左边缘往右滑，当前页就该往右让开；
     * 从右边缘往左滑则镜像。只认一个方向的话，从另一边滑会有"往回缩"的错位感。
     */
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    /**
     * 手势刚完成、动画已经被手势做完了。
     *
     * 这时要**跳过一次普通的返回过渡** —— 否则手势把页面送到终点之后，
     * 普通过渡又从起点重放一遍，看起来会抖一下。
     * 有它之后，「开 / 关预测式返回」的观感才真正不同：
     * 开着是手势驱动的缩放，关掉是标准的 300ms 过渡。
     */
    var predictiveCompleting by remember { mutableStateOf(false) }

    val canPopBack = navController.previousBackStackEntry != null

    // 每次切换目的地都把过渡标记立起来，动画放完再放下
    LaunchedEffect(backStackEntry?.id) {
        navTransitioning = true
        delay(NAV_ANIMATION_MS.toLong())
        navTransitioning = false
        predictiveCompleting = false
    }

    // 预测式返回：跟手缩放，手势完成才真正返回、取消则弹回
    if (predictiveBackEnabled && canPopBack) {
        PredictiveBackHandler { progress ->
            try {
                progress.collect { event ->
                    predictiveBackProgress = event.progress
                    predictiveBackEdge = event.swipeEdge
                }
                predictiveCompleting = true
                predictiveBackProgress = 0f
                navController.popBackStack()
            } catch (cancel: CancellationException) {
                predictiveBackProgress = 0f
                throw cancel
            }
        }
    }

    BackHandler(enabled = playerExpanded) { playerExpanded = false }
    BackHandler(enabled = sortSheetTarget != null) { sortSheetTarget = null }
    BackHandler(enabled = playModeSheetVisible) { playModeSheetVisible = false }
    BackHandler(enabled = queueSheetVisible) { queueSheetVisible = false }

    val atNavigationRoot = navController.previousBackStackEntry == null
    BackHandler(enabled = !playerExpanded && atNavigationRoot) {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, R.string.exit_hint, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(EXIT_WINDOW_MS)
            backPressedOnce = false
        }
    }

    val sortTarget = sortTargetFor(currentRoute)
    val currentSort = sortTarget?.let { sorts[it] } ?: SortSpec.DEFAULT

    // 文件夹筛选：打开「仅显示我添加的文件夹」时只保留这些目录下的音乐
    val effectiveSongs = remember(library.songs, folders, onlyFolders) {
        if (!onlyFolders || folders.isEmpty()) {
            library.songs
        } else {
            library.songs.filter { FolderPaths.isInsideAny(it.path, folders) }
        }
    }
    val effectiveAlbums = remember(effectiveSongs, artistConfig) {
        LibraryAggregator.albums(effectiveSongs, artistConfig)
    }
    val effectiveArtists = remember(effectiveSongs, artistConfig) {
        LibraryAggregator.artists(effectiveSongs, artistConfig)
    }

    val sortedSongs = remember(effectiveSongs, sorts[SortTarget.SONGS]) {
        LibrarySorter.songs(effectiveSongs, sorts[SortTarget.SONGS] ?: SortSpec.DEFAULT)
    }
    val sortedAlbums = remember(effectiveAlbums, sorts[SortTarget.ALBUMS]) {
        LibrarySorter.albums(effectiveAlbums, sorts[SortTarget.ALBUMS] ?: SortSpec.DEFAULT)
    }
    val sortedArtists = remember(effectiveArtists, sorts[SortTarget.ARTISTS]) {
        LibrarySorter.artists(effectiveArtists, sorts[SortTarget.ARTISTS] ?: SortSpec.DEFAULT)
    }

    // ---------------------------------------------------- 第二版：列表分段

    val songSort = sorts[SortTarget.SONGS] ?: SortSpec.DEFAULT
    val albumSort = sorts[SortTarget.ALBUMS] ?: SortSpec.DEFAULT
    val artistSort = sorts[SortTarget.ARTISTS] ?: SortSpec.DEFAULT

    // 罗马化（汉字→拼音、假名→罗马字）比较贵，同一个名字只算一次。
    // 分段键和排序键用的是同一套规则，保证「A 段里的名字都以 a 开头」。
    val pinyinProvider = remember { PinyinProvider() }
    val romanizeCache = remember { HashMap<String, String>() }
    val romanizeOf: (String) -> String = remember(pinyinProvider) {
        { text -> romanizeCache.getOrPut(text) { pinyinProvider.toLatin(text) } }
    }

    val songSections = remember(sortedSongs, songSort) {
        ListSections.build(
            items = sortedSongs,
            spec = songSort,
            nameOf = { it.title },
            yearOf = { it.year },
            romanizeOf = romanizeOf,
        )
    }
    val albumSections = remember(sortedAlbums, albumSort) {
        ListSections.build(
            items = sortedAlbums,
            spec = albumSort,
            nameOf = { it.name },
            yearOf = { it.year },
            romanizeOf = romanizeOf,
        )
    }
    val artistSections = remember(sortedArtists, artistSort) {
        ListSections.build(
            items = sortedArtists,
            spec = artistSort,
            nameOf = { it.name },
            yearOf = { 0 },
            romanizeOf = romanizeOf,
        )
    }

    // 三个列表各自的滚动状态，「定位到正在播放」要用
    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    /** 把正在播放的内容滚进视野。 */
    val onLocateNowPlaying: () -> Unit = {
        val song = playback.currentSong
        val target = sortTarget

        val itemIndex = when {
            song == null || target == null -> -1
            target == SortTarget.SONGS ->
                ListSections.flatIndexOf(songSections) { it.id == song.id }

            target == SortTarget.ALBUMS ->
                ListSections.flatIndexOf(albumSections) { it.id == song.albumId }

            else -> {
                val name = artistConfig.names(song.artist).firstOrNull()
                ListSections.flatIndexOf(artistSections) { it.name == name }
            }
        }

        if (itemIndex < 0) {
            toast(
                context,
                if (song == null) "还没有正在播放的歌曲" else "正在播放的内容不在当前列表里",
            )
        } else {
            scope.launch {
                when (target) {
                    SortTarget.SONGS -> songsListState.animateScrollToItem(itemIndex)
                    SortTarget.ALBUMS -> albumsGridState.animateScrollToItem(itemIndex)
                    SortTarget.ARTISTS -> artistsListState.animateScrollToItem(itemIndex)
                    null -> Unit
                }
            }
        }
    }

    /** 播放页点歌名：切到歌曲页并定位到这首歌。 */
    val onJumpToCurrentSong: () -> Unit = {
        val song = playback.currentSong
        playerExpanded = false
        navController.navigate(Routes.SONGS) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        if (song != null) {
            val index = ListSections.flatIndexOf(songSections) { it.id == song.id }
            if (index >= 0) scope.launch { songsListState.animateScrollToItem(index) }
        }
    }

    val libraryEmpty = library.hasScanned && effectiveSongs.isEmpty()
    val showSearchBar = effectiveSongs.isNotEmpty() && sortTarget != null

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    if (sortTarget != null) {
                        AppTopBar(
                            title = stringResource(tabTitleRes(currentRoute)),
                            sortLabel = currentSort.displayLabel,
                            onSortClick = { sortSheetTarget = sortTarget },
                            onLocateClick = onLocateNowPlaying,
                            onRescanClick = { libraryViewModel.refresh() },
                            onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                        )
                        if (showSearchBar) {
                            SearchBarPlaceholder(onClick = { navController.navigate(Routes.SEARCH) })
                        }
                    } else {
                        DetailTopBar(
                            title = detailTitle(currentRoute, backStackEntry, library.albums),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    if (library.isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    MiniPlayer(
                        song = playback.currentSong,
                        isPlaying = playback.isPlaying,
                        onExpand = { playerExpanded = true },
                        onTogglePlay = { playerViewModel.togglePlayPause() },
                        onPrevious = { playerViewModel.previous() },
                        onNext = { playerViewModel.next() },
                    )
                    FloatingNavBar(
                        items = MainTab.entries.map { tab ->
                            FloatingNavItem(
                                route = tab.route,
                                label = stringResource(tab.labelRes),
                                icon = tab.icon,
                            )
                        },
                        selectedRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            /**
             * 各个目的地的渲染内容 —— **单一出处**。
             *
             * NavHost 的 composable 块和「预测式返回的目的地预览」都调它。
             * 不抽出来的话，预览里画出来的东西迟早会和真实页面长得不一样。
             *
             * @param asPreview 这是手势预览用的副本：不自动聚焦输入框、不产生副作用。
             *   搜索页在预览里抢焦点的话，手指还没松开输入法就弹出来了。
             */
            val renderDestination: @Composable (androidx.navigation.NavBackStackEntry, Boolean) -> Unit =
                { entry, asPreview ->
                    when (entry.destination.route) {
                        Routes.SONGS -> {
                            if (libraryEmpty) {
                                EmptyLibraryScreen(onRescan = { libraryViewModel.refresh() })
                            } else {
                                SongsScreen(
                                    sections = songSections,
                                    currentSongId = playback.currentSong?.id,
                                    onSongClick = { song ->
                                        val index = sortedSongs.indexOfFirst { it.id == song.id }
                                        playerViewModel.playSongs(sortedSongs, if (index >= 0) index else 0)
                                    },
                                    listState = songsListState,
                                    onSongLongClick = { pendingRemove = it },
                                )
                            }
                        }

                        Routes.ALBUMS -> {
                            AlbumsScreen(
                                sections = albumSections,
                                onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                                gridState = albumsGridState,
                            )
                        }

                        Routes.ALBUM_DETAIL -> {
                            val albumId = entry.arguments?.getLong(Routes.ARG_ALBUM_ID) ?: -1L
                            val album = library.albums.firstOrNull { it.id == albumId }
                            if (album == null) {
                                // 专辑在重新扫描后消失了，直接退回上一级
                                if (!asPreview) LaunchedEffect(albumId) { navController.popBackStack() }
                            } else {
                                val albumSongs = remember(albumId, library.songs) {
                                    LibraryAggregator.sortAlbumTracks(
                                        library.songs.filter { it.albumId == albumId },
                                    )
                                }
                                AlbumDetailScreen(
                                    album = album,
                                    songs = albumSongs,
                                    currentSongId = playback.currentSong?.id,
                                    onPlayAll = { playerViewModel.playSongs(albumSongs, 0) },
                                    onSongClick = { index -> playerViewModel.playSongs(albumSongs, index) },
                                    onSongLongClick = { index -> pendingRemove = albumSongs.getOrNull(index) },
                                )
                            }
                        }

                        Routes.SEARCH -> {
                            SearchScreen(
                                index = searchIndex,
                                onSongClick = { song ->
                                    val index = sortedSongs.indexOfFirst { it.id == song.id }
                                    playerViewModel.playSongs(sortedSongs, if (index >= 0) index else 0)
                                },
                                onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                                onArtistClick = { navController.navigate(Routes.artistDetail(it.name)) },
                                autoFocus = !asPreview,
                            )
                        }

                        Routes.SETTINGS -> {
                            SettingsScreen(
                                songCount = effectiveSongs.size,
                                albumCount = effectiveAlbums.size,
                                artistCount = effectiveArtists.size,
                                folders = folders,
                                onlyFolders = onlyFolders,
                                isScanning = library.isScanning,
                                lastScanMillis = library.lastScanMillis,
                                versionName = BuildConfig.VERSION_NAME,
                                splitArtists = artistConfig.splitMultiArtist,
                                ignoreArtistParens = artistConfig.ignoreParentheses,
                                extraArtistSeparators = artistConfig.extraSeparators,
                                stopOnTaskRemoved = stopOnTaskRemoved,
                                hiddenCount = hiddenCount,
                                predictiveBack = predictiveBackEnabled,
                                onRescan = { libraryViewModel.refresh() },
                                onAddFolder = { folderPicker.launch(null) },
                                onRemoveFolder = { libraryViewModel.removeFolder(it) },
                                onToggleOnlyFolders = { libraryViewModel.setOnlyFolders(it) },
                                onToggleSplitArtists = { libraryViewModel.setSplitArtists(it) },
                                onToggleIgnoreArtistParens = { libraryViewModel.setIgnoreArtistParens(it) },
                                onAddArtistSeparators = { libraryViewModel.setArtistSeparators(it) },
                                onRemoveArtistSeparator = { libraryViewModel.removeArtistSeparator(it) },
                                onToggleStopOnTaskRemoved = { libraryViewModel.setStopOnTaskRemoved(it) },
                                onUnhideAllSongs = { libraryViewModel.unhideAllSongs() },
                                onTogglePredictiveBack = { libraryViewModel.setPredictiveBack(it) },
                            )
                        }

                        Routes.ARTISTS -> {
                            ArtistsScreen(
                                sections = artistSections,
                                onArtistClick = { navController.navigate(Routes.artistDetail(it.name)) },
                                listState = artistsListState,
                            )
                        }

                        Routes.ARTIST_DETAIL -> {
                            val artistName = entry.arguments?.getString(Routes.ARG_ARTIST_NAME).orEmpty()
                            val artist = effectiveArtists.firstOrNull { it.name == artistName }
                            if (artist == null) {
                                if (!asPreview) LaunchedEffect(artistName) { navController.popBackStack() }
                            } else {
                                val artistSongs = remember(artistName, effectiveSongs, artistConfig) {
                                    LibraryAggregator.sortArtistSongs(
                                        // 多歌手歌曲会同时归属到每一位歌手名下（配置里关掉拆分则整串算一位）
                                        effectiveSongs.filter { artistName in artistConfig.names(it.artist) },
                                    )
                                }
                                val artistAlbums = remember(artistSongs, effectiveAlbums) {
                                    val albumIds = artistSongs.map { it.albumId }.toSet()
                                    effectiveAlbums.filter { it.id in albumIds }
                                }
                                ArtistDetailScreen(
                                    artist = artist,
                                    albums = artistAlbums,
                                    songs = artistSongs,
                                    currentSongId = playback.currentSong?.id,
                                    onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                                    onPlayAll = { playerViewModel.playSongs(artistSongs, 0) },
                                    onSongClick = { index -> playerViewModel.playSongs(artistSongs, index) },
                                    onSongLongClick = { index -> pendingRemove = artistSongs.getOrNull(index) },
                                )
                            }
                        }
                    }
                }

            Box(Modifier.padding(innerPadding)) {
            /**
             * 预测式返回：手势期间把「要去的那一页」画在当前页后面。
             *
             * 这一层**完全静止** —— 用户要的是「把当前那个窗口缩小来预览」，
             * 底下的目的地不动，靠当前页缩小让开自然露出来。
             * 给目的地也加缩放/透明反而会让两层都在动，看着飘。
             *
             * Navigation Compose 不做这件事（2.8、2.9 里一个 Predictive 类都没有），
             * 它不会在手势期间组合上一个目的地 —— 不自己画的话，当前页缩小后
             * 露出来的是 Scaffold 底色，一块空白。
             */
            val previousEntry = navController.previousBackStackEntry
            if (predictiveBackProgress > 0f && previousEntry != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    renderDestination(previousEntry, true)
                }
            }

            NavHost(
                navController = navController,
                startDestination = Routes.SONGS,
                modifier = Modifier
                    .fillMaxSize()
                    // 预测式返回：当前页跟着手势**缩小并让开**，不透明化。
                    // 方向按滑的是哪一侧镜像。
                    .graphicsLayer {
                        val p = predictiveBackProgress
                        val dir = if (predictiveBackEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                        translationX = size.width * PREDICTIVE_EXIT_TRANSLATION * p * dir
                        val s = 1f - PREDICTIVE_EXIT_SCALE * p
                        scaleX = s
                        scaleY = s
                    },
                // 第十批：把返回动画做回来（第七批为了躲误触压到了 180ms，
                // 但正确做法是保留动画 + 屏蔽过渡期间的输入，见下面的遮罩层）。
                enterTransition = {
                    fadeIn(tween(NAV_ANIMATION_MS)) +
                        slideInHorizontally(tween(NAV_ANIMATION_MS)) { it / 10 }
                },
                exitTransition = {
                    fadeOut(tween(NAV_ANIMATION_MS)) +
                        slideOutHorizontally(tween(NAV_ANIMATION_MS)) { -it / 10 }
                },
                popEnterTransition = {
                    // 手势已经把这趟动画做完了，就别再重放一遍
                    if (predictiveCompleting) {
                        EnterTransition.None
                    } else {
                        fadeIn(tween(NAV_ANIMATION_MS)) +
                            slideInHorizontally(tween(NAV_ANIMATION_MS)) { -it / 10 }
                    }
                },
                popExitTransition = {
                    if (predictiveCompleting) {
                        ExitTransition.None
                    } else {
                        fadeOut(tween(NAV_ANIMATION_MS)) +
                            slideOutHorizontally(tween(NAV_ANIMATION_MS)) { it / 10 }
                    }
                },
            ) {
                composable(Routes.SONGS) { entry -> renderDestination(entry, false) }
                composable(Routes.ALBUMS) { entry -> renderDestination(entry, false) }
                composable(
                    route = Routes.ALBUM_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_ALBUM_ID) { type = NavType.LongType }),
                ) { entry -> renderDestination(entry, false) }
                composable(Routes.SEARCH) { entry -> renderDestination(entry, false) }
                composable(Routes.SETTINGS) { entry -> renderDestination(entry, false) }
                composable(Routes.ARTISTS) { entry -> renderDestination(entry, false) }
                composable(
                    route = Routes.ARTIST_DETAIL,
                    arguments = listOf(
                        navArgument(Routes.ARG_ARTIST_NAME) { type = NavType.StringType },
                    ),
                ) { entry -> renderDestination(entry, false) }
            }

                // 过渡期间盖一层透明遮罩，把落向旧页面的点击全部吃掉。
                // 第七批只是把动画压短来躲避误触；这里是真正的屏蔽 ——
                // 动画可以做得好看，同时保证过渡中点什么都没反应。
                if (navTransitioning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = playerExpanded,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            PlayerSheet(
                song = playback.currentSong,
                isPlaying = playback.isPlaying,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                mode = playback.mode,
                onCollapse = { playerExpanded = false },
                onTogglePlay = { playerViewModel.togglePlayPause() },
                onPrevious = { playerViewModel.previous() },
                onNext = { playerViewModel.next() },
                onSeek = { playerViewModel.seekTo(it) },
                onModeClick = { playModeSheetVisible = true },
                onQueueClick = { queueSheetVisible = true },
                onTitleClick = onJumpToCurrentSong,
                onArtistClick = {
                    // 用归一化后的第一位歌手名，否则「周杰伦、费玉清」这种会在歌手页找不到
                    val name = playback.currentSong?.let { artistConfig.names(it.artist).firstOrNull() }
                    if (name != null) {
                        playerExpanded = false
                        navController.navigate(Routes.artistDetail(name))
                    }
                },
                onAlbumClick = { albumId ->
                    playerExpanded = false
                    navController.navigate(Routes.albumDetail(albumId))
                },
                lyricsState = lyrics,
            )
        }

        if (playModeSheetVisible) {
            PlayModeSheet(
                current = playback.mode,
                onSelect = {
                    playerViewModel.setMode(it)
                    playModeSheetVisible = false
                },
                onDismiss = { playModeSheetVisible = false },
            )
        }

        if (queueSheetVisible) {
            QueueSheet(
                queue = playback.queue,
                currentIndex = playback.queueIndex,
                mode = playback.mode,
                onSelectIndex = {
                    playerViewModel.skipToQueueIndex(it)
                    queueSheetVisible = false
                },
                onClear = {
                    playerViewModel.clearQueue()
                    queueSheetVisible = false
                },
                onDismiss = { queueSheetVisible = false },
                roundLabel = roundLabelFor(
                    playback.mode,
                    playback.currentSong?.album,
                    playback.albumRoundIndex,
                    playback.albumRoundTotal,
                ),
            )
        }

        val sheetTarget = sortSheetTarget
        if (sheetTarget != null) {
            SortSheet(
                current = sorts[sheetTarget] ?: SortSpec.DEFAULT,
                fields = fieldsFor(sheetTarget),
                onFieldSelected = { libraryViewModel.selectSortField(sheetTarget, it) },
                onToggleOrder = { libraryViewModel.toggleSortOrder(sheetTarget) },
                onDismiss = { sortSheetTarget = null },
            )
        }

        // 长按歌曲 → 确认后从 App 里移除（不动文件）
        pendingRemove?.let { song ->
            AlertDialog(
                onDismissRequest = { pendingRemove = null },
                title = { Text("从音乐库移除？") },
                text = {
                    Text(
                        "《${song.title}》将不再显示，也不会进入播放列表、专辑和歌手统计。\n\n" +
                            "不会删除或修改你的文件，之后可以在「设置」里恢复。",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            libraryViewModel.hideSong(song.id)
                            pendingRemove = null
                            toast(context, "已从音乐库移除")
                        },
                    ) { Text("移除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemove = null }) { Text("取消") }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
    )
}

/** 二级页顶栏标题。专辑详情用专辑名，其余用固定文案。 */
private fun detailTitle(
    route: String,
    entry: androidx.navigation.NavBackStackEntry?,
    albums: List<com.musemusic45.data.model.Album>,
): String = when (route) {
    Routes.ALBUM_DETAIL -> {
        val id = entry?.arguments?.getLong(Routes.ARG_ALBUM_ID) ?: -1L
        albums.firstOrNull { it.id == id }?.name ?: "专辑"
    }

    Routes.ARTIST_DETAIL -> entry?.arguments?.getString(Routes.ARG_ARTIST_NAME) ?: "歌手"
    Routes.SEARCH -> "搜索"
    Routes.SETTINGS -> "设置"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    sortLabel: String?,
    onSortClick: () -> Unit,
    onLocateClick: () -> Unit,
    onRescanClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                if (sortLabel != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = sortLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            if (sortLabel != null) {
                IconButton(onClick = onLocateClick) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "定位到正在播放",
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = stringResource(R.string.action_sort),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("重新扫描") },
                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRescanClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("设置") },
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSettingsClick()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchBarPlaceholder(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(AppShapes.searchBar),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(40.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun tabTitleRes(route: String): Int = when (route) {
    Routes.ALBUMS -> R.string.tab_albums
    Routes.ARTISTS -> R.string.tab_artists
    else -> R.string.tab_songs
}

private fun sortTargetFor(route: String): SortTarget? = when (route) {
    Routes.SONGS -> SortTarget.SONGS
    Routes.ALBUMS -> SortTarget.ALBUMS
    Routes.ARTISTS -> SortTarget.ARTISTS
    else -> null
}

/** 歌手没有发布年份，所以只给两个字段。 */
private fun fieldsFor(target: SortTarget) = when (target) {
    SortTarget.ARTISTS -> SortSpec.ARTIST_FIELDS
    else -> SortSpec.SONG_FIELDS
}

/**
 * 播放页的轮次提示，**只在按专辑播放模式下出现**。
 * 其他模式返回 null，界面上不会显示这一行。
 */
private fun roundLabelFor(
    mode: PlayMode,
    albumName: String?,
    index: Int,
    total: Int,
): String? {
    if (mode != PlayMode.ALBUM_SHUFFLE) return null
    if (total <= 0 || index <= 0) return null
    return formatRoundLabel(albumName.orEmpty(), index, total)
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/** 连按两次退出的时间窗口。 */
private const val EXIT_WINDOW_MS = 2000L

/**
 * 导航过渡时长。
 *
 * 第七批为了躲避"退出动画期间误触播放"把它压到 180ms（默认是 700ms）。
 * 第十批改回有存在感的 300ms —— 误触改由过渡遮罩解决，不再靠牺牲动画。
 */
private const val NAV_ANIMATION_MS = 300

/**
 * 预测式返回的动效参数。
 *
 * 手势进度 p 从 0 到 1，**当前页缩小并往一边让开**，底下的目的地静止不动、
 * 被自然露出来 —— 也就是「把当前这个窗口缩小来预览」，**不做透明化**。
 * 透明化会让两层都变淡、看不清预览的是什么。
 *
 * 让开的方向由 `BackEventCompat.swipeEdge` 决定，从哪边滑就往哪边退。
 */
private const val PREDICTIVE_EXIT_TRANSLATION = 0.25f
private const val PREDICTIVE_EXIT_SCALE = 0.08f

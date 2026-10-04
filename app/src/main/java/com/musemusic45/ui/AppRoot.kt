package com.musemusic45.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.musemusic45.ui.theme.formatRoundLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var permissionGranted by remember { mutableStateOf(AudioPermissions.hasPermission(context)) }
    var playerExpanded by remember { mutableStateOf(false) }
    var backPressedOnce by remember { mutableStateOf(false) }
    var sortSheetTarget by remember { mutableStateOf<SortTarget?>(null) }
    var playModeSheetVisible by remember { mutableStateOf(false) }
    var queueSheetVisible by remember { mutableStateOf(false) }

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
    val effectiveAlbums = remember(effectiveSongs) { LibraryAggregator.albums(effectiveSongs) }
    val effectiveArtists = remember(effectiveSongs) { LibraryAggregator.artists(effectiveSongs) }

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
                val name = song.artistNames.firstOrNull()
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
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SONGS,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.SONGS) {
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
                        )
                    }
                }
                composable(Routes.ALBUMS) {
                    AlbumsScreen(
                        sections = albumSections,
                        onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                        gridState = albumsGridState,
                    )
                }
                composable(
                    route = Routes.ALBUM_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_ALBUM_ID) { type = NavType.LongType }),
                ) { entry ->
                    val albumId = entry.arguments?.getLong(Routes.ARG_ALBUM_ID) ?: -1L
                    val album = library.albums.firstOrNull { it.id == albumId }
                    if (album == null) {
                        // 专辑在重新扫描后消失了，直接退回上一级
                        LaunchedEffect(albumId) { navController.popBackStack() }
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
                        )
                    }
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        index = searchIndex,
                        onSongClick = { song ->
                            val index = sortedSongs.indexOfFirst { it.id == song.id }
                            playerViewModel.playSongs(sortedSongs, if (index >= 0) index else 0)
                        },
                        onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                        onArtistClick = { navController.navigate(Routes.artistDetail(it.name)) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        songCount = effectiveSongs.size,
                        albumCount = effectiveAlbums.size,
                        artistCount = effectiveArtists.size,
                        folders = folders,
                        onlyFolders = onlyFolders,
                        isScanning = library.isScanning,
                        lastScanMillis = library.lastScanMillis,
                        versionName = BuildConfig.VERSION_NAME,
                        onRescan = { libraryViewModel.refresh() },
                        onAddFolder = { folderPicker.launch(null) },
                        onRemoveFolder = { libraryViewModel.removeFolder(it) },
                        onToggleOnlyFolders = { libraryViewModel.setOnlyFolders(it) },
                    )
                }
                composable(Routes.ARTISTS) {
                    ArtistsScreen(
                        sections = artistSections,
                        onArtistClick = { navController.navigate(Routes.artistDetail(it.name)) },
                        listState = artistsListState,
                    )
                }
                composable(
                    route = Routes.ARTIST_DETAIL,
                    arguments = listOf(
                        navArgument(Routes.ARG_ARTIST_NAME) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val artistName = entry.arguments?.getString(Routes.ARG_ARTIST_NAME).orEmpty()
                    val artist = library.artists.firstOrNull { it.name == artistName }
                    if (artist == null) {
                        LaunchedEffect(artistName) { navController.popBackStack() }
                    } else {
                        val artistSongs = remember(artistName, library.songs) {
                            LibraryAggregator.sortArtistSongs(
                                // 多歌手歌曲会同时归属到每一位歌手名下
                                library.songs.filter { artistName in it.artistNames },
                            )
                        }
                        val artistAlbums = remember(artistSongs, library.albums) {
                            val albumIds = artistSongs.map { it.albumId }.toSet()
                            library.albums.filter { it.id in albumIds }
                        }
                        ArtistDetailScreen(
                            artist = artist,
                            albums = artistAlbums,
                            songs = artistSongs,
                            currentSongId = playback.currentSong?.id,
                            onAlbumClick = { navController.navigate(Routes.albumDetail(it.id)) },
                            onPlayAll = { playerViewModel.playSongs(artistSongs, 0) },
                            onSongClick = { index -> playerViewModel.playSongs(artistSongs, index) },
                        )
                    }
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
                modeLabel = playback.mode.playerTitle,
                onCollapse = { playerExpanded = false },
                onTogglePlay = { playerViewModel.togglePlayPause() },
                onPrevious = { playerViewModel.previous() },
                onNext = { playerViewModel.next() },
                onSeek = { playerViewModel.seekTo(it) },
                onModeClick = { playModeSheetVisible = true },
                onQueueClick = { queueSheetVisible = true },
                onTitleClick = onJumpToCurrentSong,
                onArtistClick = { name ->
                    playerExpanded = false
                    navController.navigate(Routes.artistDetail(name))
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
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

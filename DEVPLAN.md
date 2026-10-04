# 本地音乐 · 开发计划

> 依据：`DESIGN.md`（页面结构与交互设计定稿）
> 目标产物：一个可安装到 Android 手机上的纯本地音乐播放器
> 验证环境：MuMu Player 15 模拟器（Android 15 / API 35），adb 地址 `127.0.0.1:16384`

---

## 一、计划总览

### 1.1 里程碑与依赖顺序

采用**增量可演示**的顺序：每个里程碑结束时，App 都能装进模拟器、能看到新东西。

```
M0  工程改造与导航骨架
 │    └─ 交付：Compose 化的空架子，三个 Tab 能切
 ▼
M1  音乐库数据层                      ← 风险最集中，先做
 │    └─ 交付：能扫描出真实歌曲数量
 ▼
M2  歌曲页 + 排序
 │    └─ 交付：看到全部歌曲，能排序
 ▼
M3  播放引擎（Media3）                 ← 第二个风险点
 │    └─ 交付：能出声，通知栏/锁屏/线控可用
 ▼
M4  三种播放方式                       ← 需求核心
 │    └─ 交付：列表循环／随机／按专辑播放全部正确
 ▼
M5  全屏播放页
 │    └─ 交付：封面态、进度拖动、封面⇄歌词切换
 ▼
M6  专辑页 + 专辑详情
 ▼
M7  歌手页 + 歌手详情
 ▼
M8  搜索
 ▼
M9  歌词
 ▼
M10 设置 / 文件夹管理 / 持久化 / 状态页
 ▼
M11 打磨与整体验收
```

### 1.2 里程碑一览

| 编号 | 名称 | 规模 | 主要风险 | 完成后可演示 |
|---|---|---|---|---|
| M0 | 工程改造与导航骨架 | 中 | Compose 版本匹配 | 三个 Tab 能切、播放页能滑出 |
| M1 | 音乐库数据层 | 大 | **碟号/年份/封面能否拿到** | 显示扫描到的歌曲总数 |
| M2 | 歌曲页 + 排序 | 中 | 大列表滚动性能 | 真实歌曲列表 |
| M3 | 播放引擎 | 大 | **Android 14+ 前台服务类型** | 能听歌 |
| M4 | 三种播放方式 | 大 | **按专辑播放的轮次逻辑** | 需求核心功能 |
| M5 | 全屏播放页 | 中 | 进度条拖动状态同步 | 完整播放体验 |
| M6 | 专辑页 + 专辑详情 | 中 | 碟号分节展示 | 专辑浏览 |
| M7 | 歌手页 + 歌手详情 | 小 | 无 | 歌手浏览 |
| M8 | 搜索 | 中 | 拼音首字母匹配 | 全局搜索 |
| M9 | 歌词 | 中 | 滚动与高亮同步 | 歌词跟唱 |
| M10 | 设置 / 文件夹 / 持久化 / 状态页 | 中 | SAF 目录授权 | 完整闭环 |
| M11 | 打磨与验收 | 中 | 无 | 可交付 |

---

## 二、通用约定（每个里程碑都适用）

### 2.1 构建环境变量

每次执行 Gradle 前必须先设这 4 个变量：

```powershell
$env:JAVA_HOME        = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = 'D:\Android\gradle-home'
$env:ANDROID_HOME     = 'D:\Android\sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\sdk'
```

### 2.2 常用命令

```powershell
# 工程根目录
$proj = 'C:\Users\No45_\Documents\deepseek-harness\default-workspace\LocalMusic'
$adb  = 'D:\Android\sdk\platform-tools\adb.exe'
$dev  = '127.0.0.1:16384'

# 编译 debug 包
& 'D:\Android\gradle-8.9\bin\gradle.bat' -p $proj assembleDebug --console=plain

# 跑单元测试
& 'D:\Android\gradle-8.9\bin\gradle.bat' -p $proj testDebugUnitTest --console=plain

# 安装并启动
& $adb -s $dev install -r "$proj\app\build\outputs\apk\debug\app-debug.apk"
& $adb -s $dev shell am start -n com.localmusic.player/.MainActivity

# 看日志（只看本 App 和崩溃）
& $adb -s $dev logcat -d -s LocalMusic:V AndroidRuntime:E

# 确认模拟器在线
& $adb devices
```

**MuMu 没启动时**（`adb devices` 里看不到 `127.0.0.1:16384`）：

```powershell
& 'D:\Program Files\Netease\MuMu\nx_main\MuMuManager.exe' control -v 0 launch
& $adb connect 127.0.0.1:16384
```

### 2.3 代码包结构（目标形态）

```
app/src/main/java/com/localmusic/player/
├── LocalMusicApp.kt                 Application，初始化仓库与偏好
├── MainActivity.kt                  单 Activity，setContent 挂根 UI
│
├── ui/
│   ├── theme/                       Color / Theme / Type
│   ├── AppRoot.kt                   Scaffold + 底部导航 + 迷你播放器 + 播放覆盖层
│   ├── nav/Routes.kt                路由常量与参数
│   ├── components/                  跨页面复用组件
│   │   ├── CoverImage.kt            封面（含灰底音符占位）
│   │   ├── ArtistAvatar.kt          歌手首字圆形色块
│   │   ├── SongRow.kt               歌曲列表行
│   │   ├── MiniPlayer.kt
│   │   ├── SortSheet.kt             排序面板
│   │   └── EmptyState.kt            空态通用组件
│   ├── songs/SongsScreen.kt
│   ├── albums/AlbumsScreen.kt
│   ├── albums/AlbumDetailScreen.kt
│   ├── artists/ArtistsScreen.kt
│   ├── artists/ArtistDetailScreen.kt
│   ├── search/SearchScreen.kt
│   ├── settings/SettingsScreen.kt
│   └── player/
│       ├── PlayerSheet.kt           全屏播放页（封面态 / 歌词态）
│       ├── QueueSheet.kt            播放列表面板
│       ├── PlayModeSheet.kt         播放方式面板
│       └── LyricsPane.kt            歌词滚动区
│
├── data/
│   ├── model/                       Song / Album / Artist / PlayMode / SortField / SortOrder
│   ├── media/MediaStoreScanner.kt   查询系统媒体库
│   ├── media/TagReader.kt           碟号/年份/内嵌封面兜底读取
│   ├── media/FolderScanner.kt       SAF 目录遍历
│   ├── media/LrcParser.kt           .lrc 解析
│   ├── media/AlbumArtLoader.kt      封面加载
│   ├── repository/MusicRepository.kt
│   └── prefs/SettingsStore.kt       DataStore 持久化
│
├── playback/
│   ├── PlaybackService.kt           MediaSessionService
│   ├── PlaybackCoordinator.kt       对 UI 暴露播放状态与操作
│   ├── QueueContext.kt              队列来源描述
│   └── AlbumRoundPlanner.kt         ★ 按专辑播放的轮次算法
│
└── permission/AudioPermission.kt
```

### 2.4 代码规范

- 全部界面文字用中文，硬编码在 `strings.xml`，不写死在 Kotlin 里
- 数据模型用 `data class` + `ImmutableList`（避免 Compose 无谓重组）
- 时间长度统一用毫秒 `Long` 存储，格式化函数集中在 `ui/theme/Format.kt`
- 所有列表的 key 用稳定的 `id`（MediaStore 的 `_ID`），不用 index
- 每个里程碑结束前必须：编译通过 + 单元测试通过 + 装模拟器跑一遍验收清单

---

## 三、依赖与版本矩阵

### 3.1 构建插件

| 组件 | 版本 | 说明 |
|---|---|---|
| Gradle | 8.9 | 已装好在 `D:\Android\gradle-8.9` |
| Android Gradle Plugin | 8.5.2 | 沿用现有工程，不改 |
| Kotlin | **2.0.21** | 从 1.9.24 升上来 |
| Compose 编译器插件 | **2.0.21** | Kotlin 2.0 起随 Kotlin 版本走，必须与 Kotlin 完全一致 |
| compileSdk / targetSdk | 34 | 已安装 |
| minSdk | 26 | Android 8.0 |

### 3.2 应用依赖

```
Compose BOM                       2024.09.03
androidx.activity:activity-compose 1.9.2
androidx.lifecycle:lifecycle-runtime-compose   2.8.6
androidx.lifecycle:lifecycle-viewmodel-compose 2.8.6
androidx.navigation:navigation-compose         2.8.1
androidx.compose.material3:material3           (由 BOM 管)
androidx.compose.material:material-icons-extended (由 BOM 管)
androidx.media3:media3-exoplayer    1.4.1
androidx.media3:media3-session      1.4.1
androidx.media3:media3-ui           1.4.1
io.coil-kt:coil-compose             2.7.0
androidx.datastore:datastore-preferences 1.1.1
androidx.core:core-ktx              1.13.1
```

测试依赖：

```
junit:junit                         4.13.2
org.jetbrains.kotlinx:kotlinx-coroutines-test 1.8.1
app.cash.turbine:turbine            1.1.0
```

### 3.3 已知的版本风险与应对

| 风险 | 现象 | 应对 |
|---|---|---|
| 某依赖要求更高的 compileSdk | 报 `Dependency requires compileSdk 35` | 装 `platforms;android-35`，把 compileSdk 提到 35（targetSdk 保持 34） |
| Compose 编译器插件与 Kotlin 不匹配 | 报 `Compose Compiler version mismatch` | 两个版本号必须完全相同，一起升降 |
| `material-icons-extended` 让 debug 包变大 | 包体积增加约 10 MB | 可接受；release 构建由 R8 自动剔除未用图标 |

**磁盘空间**：C 盘只剩约 21 GB，D 盘 71 GB。Gradle 缓存已导向 D 盘（`GRADLE_USER_HOME`），不要在 C 盘新增缓存目录。

---

## 四、各里程碑详细任务

---

### M0 · 工程改造与导航骨架

**目标**：把现有的经典 Views 空壳改造成 Compose 工程，并搭出全局骨架。全部使用假数据。

**前置**：无

**任务**

| # | 任务 | 涉及文件 |
|---|---|---|
| 0-1 | 根构建脚本加 Compose 编译器插件（Kotlin 升到 2.0.21） | `build.gradle.kts` |
| 0-2 | app 模块开启 `buildFeatures.compose = true`，关掉 `viewBinding` | `app/build.gradle.kts` |
| 0-3 | 加入第三章列出的全部依赖 | `app/build.gradle.kts` |
| 0-4 | Manifest 补齐权限与服务声明（此时先加权限，服务留给 M3） | `AndroidManifest.xml` |
| 0-5 | 生成 Gradle Wrapper，并把 `distributionUrl` 指向腾讯云镜像 | `gradle/wrapper/*` |
| 0-6 | MainActivity 改为 `setContent {}`，删掉 `activity_main.xml` | `MainActivity.kt` |
| 0-7 | 建立主题（Material 3 配色 + 中文字体回退） | `ui/theme/*` |
| 0-8 | 搭 `AppRoot`：Scaffold + 底部三 Tab + 迷你播放器占位 + 播放覆盖层占位 | `ui/AppRoot.kt` |
| 0-9 | 三个一级页放假数据列表，验证滚动与切换 | `ui/songs/SongsScreen.kt` 等 |
| 0-10 | 实现返回键规则：一级页提示"再按一次退出" | `MainActivity.kt` |

**验收标准**

1. `assembleDebug` 成功
2. 装进 MuMu 后能启动，**不闪退**
3. 底部三个 Tab 能互相切换，切换后标题正确
4. 迷你播放器占位可见；点它能上滑展开播放覆盖层；下滑能收起
5. 在一级页按返回键 → 弹"再按一次退出"提示；连按两次才退出
6. `gradle testDebugUnitTest` 通过（此时可以没有测试）

**注意**

- Kotlin 从 1.9.24 升到 2.0.21 后，`kotlinOptions { jvmTarget }` 会报废弃警告，可以接受，也可以改成 `compilerOptions`
- 生成 Wrapper 后，后续构建可以改用 `.\gradlew.bat`，但本机仍建议直接用 `D:\Android\gradle-8.9\bin\gradle.bat`（省掉一次发行版下载）

---

### M1 · 音乐库数据层

**目标**：能扫描出手机里的真实音乐，建立数据模型与仓库。

**前置**：M0

**这是风险最集中的里程碑**，先做三个验证性任务（Spike），结论决定后面怎么写。

#### Spike 任务（必须先做，结论要记录）

| # | 验证什么 | 方法 | 结论影响 |
|---|---|---|---|
| S-1 | 读取音频权限在 API 35 上怎么要 | 用 `READ_MEDIA_AUDIO` 试查询 | 决定权限代码分支 |
| S-2 | **碟号能否拿到** | 先查 `MediaStore.Audio.Media` 有没有碟号列；没有则试 `MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER` | 决定是否需要自己解析标签 |
| S-3 | **发布年份是否可靠** | 查 `MediaStore.Audio.Media.YEAR` 与文件实际年份对比 | 决定年份排序的数据源 |
| S-4 | **专辑封面怎么取** | 试 API 29+ 的 `loadThumbnail(albumUri)`；再试内嵌封面 | 决定封面方案 |

**S-2 的兜底策略（重要）**：如果两个途径都拿不到碟号，采用**按需读取**——默认所有歌碟号记为 1；只有当同一张专辑内出现**重复的音轨号**时，才判定它大概率是多碟专辑，此时才对这张专辑的歌逐个读标签。这样避免对上千首歌全量解析标签导致扫描变慢。

#### 常规任务

| # | 任务 | 涉及文件 |
|---|---|---|
| 1-1 | 数据模型：`Song` / `Album` / `Artist` | `data/model/*.kt` |
| 1-2 | 权限封装：按 API 等级选择 `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | `permission/AudioPermission.kt` |
| 1-3 | `MediaStoreScanner`：一次查询取回全部字段，映射成 `Song` | `data/media/MediaStoreScanner.kt` |
| 1-4 | 由 `Song` 聚合出 `Album` 与 `Artist`（处理"未知专辑""未知歌手"） | `data/repository/MusicRepository.kt` |
| 1-5 | `TagReader`：碟号/年份/内嵌封面的兜底读取 | `data/media/TagReader.kt` |
| 1-6 | `MusicRepository`：对外暴露 `songs` / `albums` / `artists` 的 Flow | `data/repository/MusicRepository.kt` |
| 1-7 | 准备测试素材并推入模拟器 | 见下 |

#### 测试素材准备

**来源**：优先用电脑上现成的音乐。`D:\CloudMusic` 是网易云音乐的下载目录，很可能有可用文件，先去看一眼。

**必须覆盖的样本类型**（缺哪一种，相应功能就没法验证）：

| 样本 | 用来验证 |
|---|---|
| 单碟专辑、标签齐全 | 基本路径 |
| **多碟专辑**（碟1/碟2，音轨号重复） | 碟号分节、专辑内排序 |
| 无专辑名的歌 | "未知专辑"归并 |
| 无音轨号的歌 | 排在专辑末尾 |
| 无年份的专辑 | 年份排序排最后 |
| 同目录带 `.lrc` 的歌 | 歌词读取 |
| 有内嵌歌词的歌 | 内嵌歌词读取 |
| 有内嵌封面的歌 | 封面显示 |
| 没有封面的歌 | 占位图 |
| 歌手名含中文的歌 | 拼音首字母搜索 |

**推入模拟器**：

```powershell
& $adb -s $dev push '<本地文件>' /sdcard/Music/
```

**触发媒体扫描**：高版本 Android 上 `MEDIA_SCANNER_SCAN_FILE` 广播已不可靠，最稳的办法是用 App 内 P7 设置页的"重新扫描"按钮（走 `MediaScannerConnection.scanFile`）。这个按钮本来就在设计里，M10 才正式做，M1 可以先做一个临时入口。

**验收标准**

1. 首次启动弹出权限申请；拒绝后显示权限引导页；同意后开始扫描
2. logcat 打印出的歌曲数、专辑数、歌手数与推进模拟器的文件数**吻合**
3. 四个 Spike 全部有明确结论，记录在案
4. 扫描 1000 首歌耗时在可接受范围（目标 < 3 秒，用 logcat 打时间戳测）

---

### M2 · 歌曲页 + 排序

**目标**：把真实歌曲列表画出来，并实现歌曲的三种排序。

**前置**：M1

| # | 任务 | 涉及文件 |
|---|---|---|
| 2-1 | `SortField` / `SortOrder` 模型与比较器 | `data/model/Sort.kt` |
| 2-2 | 中文按拼音排序（用 `java.text.Collator` 的 `Locale.CHINA`） | `data/model/Sort.kt` |
| 2-3 | `SongRow` 组件：小封面 + 歌名 + `歌手 · 专辑` + 时长 | `ui/components/SongRow.kt` |
| 2-4 | `CoverImage` 组件：Coil 加载 + 灰底音符占位 | `ui/components/CoverImage.kt` |
| 2-5 | 歌曲页顶栏：显示当前排序文字 + ⇅ + ⋮ | `ui/songs/SongsScreen.kt` |
| 2-6 | `SortSheet` 排序面板（字段 + 升降序） | `ui/components/SortSheet.kt` |
| 2-7 | 正在播放的歌曲整行高亮（此时可先用假状态） | `ui/songs/SongsScreen.kt` |

**验收标准**

1. 歌曲页列出全部真实歌曲，歌名/歌手/专辑/时长正确
2. 封面小图显示正常，没有封面的显示灰底音符占位
3. 点 ⇅ 弹出排序面板；选"名称"→ 中文按拼音 A→Z 排列
4. 切换到"添加时间"，最新的排最上面；再点箭头切换成升序
5. 切换到"发布年份"，无年份的排在最后
6. 1000 首歌的列表滚动流畅，不卡顿、不闪白

---

### M3 · 播放引擎

**目标**：能出声，并且后台、通知栏、锁屏、耳机线控全部可用。

**前置**：M2

**这是第二个风险集中点**：Android 14（API 34）起前台服务必须声明类型，媒体播放用 `mediaPlayback`。

| # | 任务 | 涉及文件 |
|---|---|---|
| 3-1 | Manifest 声明 `PlaybackService`，带 `foregroundServiceType="mediaPlayback"` | `AndroidManifest.xml` |
| 3-2 | `PlaybackService : MediaSessionService` | `playback/PlaybackService.kt` |
| 3-3 | `PlaybackCoordinator`：包装 ExoPlayer，暴露播放状态 | `playback/PlaybackCoordinator.kt` |
| 3-4 | 请求 `POST_NOTIFICATIONS` 运行时权限（API 33+） | `permission/*` |
| 3-5 | 迷你播放器接真实状态：封面 + 歌名 + 歌手 + 三键 | `ui/components/MiniPlayer.kt` |
| 3-6 | 点击歌曲行 → 播放，并把队列设为当前列表 | `ui/songs/SongsScreen.kt` |
| 3-7 | 耳机线控（Media3 自带，验证单击/双击/三击） | — |

**验收标准**

1. 点任意一首歌能出声，音质正常、不卡顿
2. 通知栏出现播放控制：封面、歌名、歌手、上一首/播放暂停/下一首
3. 按 Home 键退到后台，音乐**继续播放**
4. 锁屏上能控制播放
5. 耳机线控：单击暂停/播放，双击下一首，三击上一首
6. 通知栏进度条能拖动
7. 从最近任务里划掉 App，音乐停止且通知消失（不掉后台幽灵进程）

---

### M4 · 三种播放方式

**目标**：实现需求的核心。这是整个项目最需要仔细做对的部分。

**前置**：M3

#### 任务

| # | 任务 | 涉及文件 |
|---|---|---|
| 4-1 | `PlayMode` 枚举 + `QueueContext`（描述队列来源） | `data/model/*.kt`、`playback/QueueContext.kt` |
| 4-2 | **`AlbumRoundPlanner`**：按专辑播放的轮次算法 | `playback/AlbumRoundPlanner.kt` |
| 4-3 | `AlbumRoundPlanner` 的**单元测试**（见下） | `app/src/test/...` |
| 4-4 | `PlayModeSheet` 播放方式面板（三项带说明文字） | `ui/player/PlayModeSheet.kt` |
| 4-5 | `QueueSheet` 播放列表面板（三种模式显示不同内容） | `ui/player/QueueSheet.kt` |
| 4-6 | 三种模式接入 ExoPlayer | `playback/PlaybackCoordinator.kt` |
| 4-7 | 播放页顶栏文字随模式变化 | `ui/player/PlayerSheet.kt` |
| 4-8 | 播放页显示 `《专辑》· 第 x / N 张`（仅按专辑模式） | `ui/player/PlayerSheet.kt` |

#### 三种模式的实现方式

| 模式 | ExoPlayer 配置 | 额外逻辑 |
|---|---|---|
| 列表循环 | `repeatMode = REPEAT_MODE_ALL`，`shuffleModeEnabled = false` | 无 |
| 随机播放 | `shuffleModeEnabled = true`，`repeatMode = REPEAT_MODE_ALL` | 无 |
| 按专辑播放 | `repeatMode = REPEAT_MODE_OFF`，队列**只装当前专辑** | 监听 `STATE_ENDED` → 问 Planner 要下一张专辑 → 换队列 |

**为什么按专辑播放要用 `REPEAT_MODE_OFF`**：如果用 `ALL`，单专辑队列会自己循环，永远播不完，也就永远触发不了"换下一张专辑"。用 `OFF` 才能在整张放完时收到结束事件。

**已知取舍**：换专辑时会有极短的间隙。如果实测听感明显，改成"最后一首开始播放时就预先把下一张专辑追加进队列"的策略。先按简单做法做，实测再优化。

#### `AlbumRoundPlanner` 的完整规则（对照 DESIGN.md 第七章）

```
状态：
  roundPool     本轮还没播过的专辑集合
  currentAlbum  当前专辑

规则：
  startRound()            重置 roundPool = 全部专辑
  setCurrent(album)       手动点歌时：currentAlbum = album
                          如果 album 在本轮已播过，不动 roundPool
                          否则把它从 roundPool 移除（算它已播）
  nextAlbum()             当前专辑播完时：
                          若 roundPool 为空 → startRound() 后重新抽
                          从 roundPool 随机抽一张 → 移出 → 返回
  remaining()             roundPool 的大小
```

#### 必须写的单元测试

| 测试 | 断言 |
|---|---|
| 一轮内不重复 | 连续抽 N 次，抽到的专辑两两不同 |
| 抽完自动重开 | 抽满一轮后再抽，又得到全部专辑各一次 |
| 手动点已播专辑不消耗池 | 播完 3 张后手动点回第 1 张，剩余数不变 |
| 手动点未播专辑会消耗池 | 手动点一张没播过的，剩余数减 1 |
| 只有一张专辑时不死循环 | 反复 `nextAlbum()` 始终返回那一张，不抛异常 |
| 零张专辑时不崩溃 | 返回 null，调用方不崩 |

**验收标准**

1. 三种模式都能通过播放方式面板切换，切换时**当前歌不中断**
2. 列表循环：播到最后一首自动回到第一首
3. 随机播放：顺序确实被打乱，且播完一轮后继续
4. **按专辑播放（重点走查）**：
   - 从某首歌切进来，当前专辑 = 那首歌所在专辑，从该首继续
   - 整张专辑按**碟号 → 音轨号升序**放完，中途不跳到别的专辑
   - 专辑放完后随机换到另一张没播过的专辑
   - 走完一整轮，专辑不重复
   - 一轮结束后自动开始新一轮
   - 在专辑第 1 首按"上一首"，停在当前歌开头
   - 手动点别的专辑的歌 → 那张专辑成为当前专辑，从该首开始
   - 播放列表面板里**只有当前这张专辑的歌**
5. 单元测试全绿

---

### M5 · 全屏播放页

**前置**：M4

| # | 任务 | 涉及文件 |
|---|---|---|
| 5-1 | 封面态布局：大封面 + 歌名 + 歌手·专辑 + 轮次行 + 进度 + 五键 | `ui/player/PlayerSheet.kt` |
| 5-2 | 进度条拖动（拖动中显示预览时间，松手才跳转） | `ui/player/PlayerSheet.kt` |
| 5-3 | 上滑展开 / 下滑收起的手势 | `ui/AppRoot.kt` |
| 5-4 | 点封面区 ⇄ 歌词态切换（歌词区先放占位） | `ui/player/PlayerSheet.kt` |

**验收标准**

1. 点迷你播放器上滑展开，动画流畅
2. 下滑收起，播放不中断
3. 拖进度条时时间实时预览，松手才真正跳转，松手后进度不回弹
4. 点封面切到歌词占位区，再点切回封面
5. 五个按钮全部有效：🔁 弹面板、⏮ 上一首、⏯ 播放暂停、⏭ 下一首、☰ 弹队列

---

### M6 · 专辑页 + 专辑详情

**前置**：M2

| # | 任务 | 涉及文件 |
|---|---|---|
| 6-1 | 专辑三列网格 + 排序 | `ui/albums/AlbumsScreen.kt` |
| 6-2 | 专辑详情：头部信息 + `[▶ 播放整张]` | `ui/albums/AlbumDetailScreen.kt` |
| 6-3 | **按碟号分节**的曲目列表（单碟不显示"碟 1"标题） | `ui/albums/AlbumDetailScreen.kt` |
| 6-4 | 曲目行显示音轨号 | `ui/albums/AlbumDetailScreen.kt` |

**验收标准**

1. 三列网格布局正确，无封面的显示占位图
2. 第三行显示"年份 · 曲目数"，无年份时只显示曲目数
3. 进专辑详情，曲目**按碟号、再按音轨号升序**排列
4. 多碟专辑显示"碟 1""碟 2"分节；单碟专辑不显示分节标题
5. 点 `[▶ 播放整张]` 从碟1音轨1开始播

---

### M7 · 歌手页 + 歌手详情

**前置**：M2

| # | 任务 | 涉及文件 |
|---|---|---|
| 7-1 | 歌手列表 + 排序（只有名称、添加时间两项） | `ui/artists/ArtistsScreen.kt` |
| 7-2 | `ArtistAvatar` 首字圆形色块（颜色由名字哈希决定） | `ui/components/ArtistAvatar.kt` |
| 7-3 | 歌手详情：头部 + 专辑横滑条 + 全部歌曲 | `ui/artists/ArtistDetailScreen.kt` |

**验收标准**

1. 同一歌手的色块颜色**每次进入都一致**
2. 歌手条目显示"N 张专辑 · M 首歌"
3. 详情页上下两段一体滚动
4. 点专辑横滑条里的专辑能进专辑详情
5. 排序面板里**不出现"发布年份"**这一项

---

### M8 · 搜索

**前置**：M2

| # | 任务 | 涉及文件 |
|---|---|---|
| 8-1 | 搜索页：输入框 + 三段分组结果 | `ui/search/SearchScreen.kt` |
| 8-2 | 匹配逻辑：歌名/歌手/专辑名，大小写不敏感 | `data/repository/MusicRepository.kt` |
| 8-3 | **拼音首字母匹配**（`zjl` → 周杰伦） | `data/media/PinyinMatcher.kt` |
| 8-4 | 无结果时的提示文案 | `ui/search/SearchScreen.kt` |

**拼音首字母的实现取舍**：完整拼音库会显著增加体积和扫描耗时。建议做法是**扫描入库时为每首歌预计算一次**"名称/歌手/专辑的拼音首字母串"并缓存，搜索时只做字符串匹配。汉字转拼音可以用一个小型码表（常用字约 2 万字）或 Android 自带的 `HanziToPinyin`（系统内部类，不能直接用）。如果实测体积或耗时不可接受，退回"仅汉字匹配"，但要告知。

**验收标准**

1. 输入汉字能搜到对应的歌曲、专辑、歌手，结果分三段
2. 输入 `zjl` 能搜到周杰伦相关的歌
3. 大小写不敏感
4. 无结果时显示 `没有找到"xxx"`
5. 清空输入框回到初始状态

---

### M9 · 歌词

**前置**：M5

| # | 任务 | 涉及文件 |
|---|---|---|
| 9-1 | `LrcParser`：解析标准 LRC（含 `[mm:ss.xx]` 时间轴、多时间戳一行） | `data/media/LrcParser.kt` |
| 9-2 | `LrcParser` 单元测试 | `app/src/test/...` |
| 9-3 | 歌词来源查找：同目录同名 `.lrc` 优先，其次内嵌歌词 | `data/media/*` |
| 9-4 | `LyricsPane`：逐行滚动 + 当前行居中高亮 | `ui/player/LyricsPane.kt` |
| 9-5 | 点歌词行跳到该时间点 | `ui/player/LyricsPane.kt` |
| 9-6 | 无歌词时显示"暂无歌词"，且禁止切到歌词态 | `ui/player/PlayerSheet.kt` |

**必须写的单元测试**

| 测试 | 断言 |
|---|---|
| 标准单行时间戳 | 时间与文本解析正确 |
| 一行多时间戳 `[00:01.00][00:05.00]歌词` | 展开成两条 |
| 无时间戳的元数据行 `[ar:xxx]` | 被忽略，不污染歌词 |
| 毫秒位数 `[00:01.5]` / `[00:01.50]` / `[00:01.500]` | 都解析正确 |
| 空文件 / 乱码 | 不崩溃，返回空列表 |
| 二分查找当前行 | 各时间点定位正确 |

**验收标准**

1. 同目录有 `.lrc` 的歌，歌词能滚动、当前行高亮
2. 内嵌歌词的歌也能显示
3. 都没有时显示"暂无歌词"，此时点封面**不切换**
4. 点歌词行能跳到对应时间点
5. 拖动进度条后，歌词位置立即跟上（不是滞后一首歌）

---

### M10 · 设置 / 文件夹管理 / 持久化 / 状态页

**前置**：M1～M9

| # | 任务 | 涉及文件 |
|---|---|---|
| 10-1 | `SettingsStore`（DataStore）：排序偏好、播放方式、上次播放的歌与位置、已添加文件夹、只看文件夹开关 | `data/prefs/SettingsStore.kt` |
| 10-2 | 启动时恢复上次的播放方式与排序 | `LocalMusicApp.kt` |
| 10-3 | 设置页：重新扫描、文件夹管理、统计信息、关于 | `ui/settings/SettingsScreen.kt` |
| 10-4 | SAF 目录授权 + `FolderScanner` 遍历 | `data/media/FolderScanner.kt` |
| 10-5 | 权限引导页 | `ui/components/EmptyState.kt` |
| 10-6 | 空库引导页（没有找到音乐） | `ui/components/EmptyState.kt` |
| 10-7 | 扫描中的细进度条（不遮挡列表） | `ui/AppRoot.kt` |

#### "添加文件夹"的语义定义（本计划确定，如与预期不符请指出）

- **默认**：音乐库 = 系统媒体库里所有标记为音乐的音频
- **添加文件夹**：用 SAF 授权一个目录，App 遍历该目录（含子目录）找出音频文件，**合并**进库（与媒体库已有的去重）
- **另有一个开关**"仅显示我添加的文件夹"，打开后库只包含这些目录下的音频

这样同时满足"补充媒体库没索引到的文件"和"只看某几个目录"两种用法。

**验收标准**

1. 改排序后杀掉 App 重开，排序**保持**
2. 切到随机播放后重开，播放方式**保持**
3. 设置页能触发重新扫描，扫描中有细进度条
4. 能添加一个目录、能看到它出现在列表里、能移除
5. 打开"仅显示我添加的文件夹"，歌曲数相应变化
6. 拒绝权限时显示引导页；清空音乐库时显示空库引导页

---

### M11 · 打磨与整体验收

| # | 任务 |
|---|---|
| 11-1 | 逐条走查 DESIGN.md 第十二章"交互规则总表"，每条都实测 |
| 11-2 | 逐条走查 DESIGN.md 第十三章"状态与异常"，每个状态都构造出来看 |
| 11-3 | 性能：1000+ 首歌下列表滚动、切页、搜索的流畅度 |
| 11-4 | 内存：连续播放 1 小时、反复进出各页面，观察是否泄漏 |
| 11-5 | 边界：单曲专辑、零专辑、空歌手名、超长歌名、超长专辑名 |
| 11-6 | 打包 release APK（`assembleRelease`），确认能装能跑 |

**整体验收 = DESIGN.md 第四章"要做"清单逐条打勾。**

---

## 五、风险清单

| 风险 | 等级 | 影响 | 应对 |
|---|---|---|---|
| **碟号拿不到** | 高 | 多碟专辑顺序错 | M1 的 S-2 先验证；兜底是"同专辑内音轨号重复时才逐个读标签" |
| **Android 14+ 前台服务限制** | 高 | 后台播放被系统杀掉 | Manifest 正确声明 `mediaPlayback` 类型 + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限 |
| **Compose 版本不匹配** | 中 | 编译直接失败 | Kotlin 与 Compose 编译器插件版本严格一致；M0 第一件事就是验证能编译 |
| **依赖要求更高 compileSdk** | 中 | 依赖解析失败 | 装 `platforms;android-35`，compileSdk 提到 35 |
| **模拟器里没有音乐** | 中 | 全流程无法验证 | M1 专门准备测试素材并推入 |
| **媒体扫描触发不了** | 中 | 推入的文件查不到 | 用 App 内的 `MediaScannerConnection.scanFile`，不依赖广播 |
| **下载慢** | 中 | 构建卡住 | Gradle 缓存已在 D 盘；Gradle 发行版走腾讯云镜像（已实测 28 秒 vs 官方 75 分钟） |
| **拼音首字母实现成本超预期** | 低 | M8 延期 | 退回"仅汉字匹配"并告知 |
| **换专辑时有听感间隙** | 低 | 体验小瑕疵 | 改成预追加下一张专辑的策略 |

---

## 六、测试策略

### 单元测试（必须在对应里程碑完成）

| 里程碑 | 测试对象 | 覆盖点 |
|---|---|---|
| M1 | `MediaStoreScanner` 的映射逻辑 | 字段缺失时的默认值 |
| M2 | 排序比较器 | 中文拼音序、无年份排最后 |
| M4 | `AlbumRoundPlanner` | 6 个用例，见 M4 |
| M9 | `LrcParser` | 6 个用例，见 M9 |

### 手动验收

每个里程碑的"验收标准"逐条在 MuMu 模拟器上实测。**不靠"看起来没问题"，靠动手点。**

### 日志约定

关键路径统一用 `android.util.Log`，tag 用 `LocalMusic`：

- 扫描：开始/结束时间、扫到的歌曲数、专辑数、歌手数
- 播放：模式切换、专辑轮次变化（当前第几张、剩余几张）
- 错误：任何 catch 都要打日志，不静默吞掉

---

## 七、执行须知

1. **每完成一个里程碑，先跑完整验收再进下一个**，不要攒着一起验
2. **M1 的四个 Spike 结论要写下来**，它们决定后面代码怎么写
3. **M4 是整个项目的核心**，`AlbumRoundPlanner` 必须先写测试再写实现
4. 遇到 DESIGN.md 没覆盖到的情况，**先停下来问**，不要自己发明规则
5. 改动 DESIGN.md 已确定的交互（比如返回键行为、面板内容）之前，先说明理由

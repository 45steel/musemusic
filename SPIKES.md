# M1 Spike 验证结论

> 全部结论来自 Android 15（API 35）模拟器上的**实测**，测试素材见 `D:\Android\testmusic`。
> 这些结论直接决定了数据层的实现方式。

---

## S-1 · 读取音频权限

| 项 | 结论 |
|---|---|
| Android 13（API 33）及以上 | `READ_MEDIA_AUDIO` |
| Android 12（API 32）及以下 | `READ_EXTERNAL_STORAGE`（`maxSdkVersion=32`） |
| 实测表现 | 首次启动显示权限引导页；点「去授权」弹出系统弹窗，文案为「要允许"缪斯音乐"访问此设备上的音乐和音频吗？」；授权后立即开始扫描 |

**决定**：`AudioPermissions` 按 API 等级选择权限数组，UI 用 `RequestMultiplePermissions` 申请。

---

## S-2 · 碟号（最重要的一条）

| 项 | 结论 |
|---|---|
| MediaStore 是否有碟号列 | ✅ **有**。列名 `disc_number`，但它**不在** `MediaStore.Audio.Media` 的公开常量里 |
| 实测值 | 合成的双碟专辑读到 `disc_number` = 1,1,1,2,2,2，完全正确 |
| **陷阱** | 多碟专辑的 `track` 列被 MediaProvider 存成 **`碟号 × 1000 + 音轨号`**：碟 1 轨 1 → `1001`，碟 2 轨 1 → `2001` |

**决定**：
1. 列名用字面量 `"disc_number"`，并在查询失败时降级到不含碟号的查询
2. 必须用 `normalizeTrackNumber()` 反算真实音轨号，否则多碟专辑的曲目顺序会全乱
3. 单碟专辑（`disc_number` 为 NULL）的 `track` 就是普通音轨号，原样使用

---

## S-3 · 发布年份

| 途径 | 结果 |
|---|---|
| MediaStore 的 `year` 列 | ❌ **FLAC 一律为 NULL**；MP3 正常（Metallica 读到 2008） |
| `MediaMetadataRetriever.METADATA_KEY_YEAR` | ❌ FLAC 同样读不到，返回 null |
| 自己解析标签字节 | ✅ **有效**。FLAC 读 Vorbis 注释的 `DATE`，MP3 读 ID3v2 的 `TDRC`/`TYER` |

**决定**：实现 `TagParsers`（纯字节解析）+ `TagReader`（定点文件读取，跳过封面块不整文件读入）。
扫描分两步：**先发布列表保证首屏**，再后台补齐年份并二次更新。

**实测效果**：36 首歌中 32 首需要补年份，成功补齐 31 首（唯一失败的是故意不带年份标签的测试文件），耗时 **249ms**。

---

## S-4 · 专辑封面

| 项 | 结论 |
|---|---|
| 测试素材 | 真实 FLAC 内嵌 JPEG 封面（1.2MB）；合成 FLAC 内嵌 300×300 PNG |
| 取用方式 | 走 `content://media/external/audio/albumart/<albumId>`，由 MediaStore 统一提供缩略图 |
| 验证阶段 | 在 M2（歌曲页小封面）实测 |

---

## 其它发现

### 缺失专辑名不会为空
MediaStore 在专辑标签缺失时会**回退成文件夹名**。
例如把歌放在 `边界用例/` 目录下且不写 ALBUM 标签，`album` 列读出来就是 `"边界用例"`。
所以「未知专辑」的归并规则只在标签和文件夹名都拿不到时才会触发。

### 缺失音轨号读出来是 NULL
归一化为 `0`，按设计排在所属碟的**最后**。

### MediaStore 不提供歌词
`MediaMetadataRetriever` 没有 `METADATA_KEY_LYRICS` 常量，FLAC 的 `LYRICS` 注释和
MP3 的 `USLT` 帧都取不到。**内嵌歌词必须自己解析标签块**（M9 实现）。

### 专辑按 (专辑名, 专辑歌手) 建键
同一个专辑名配不同歌手会拆成**两张专辑**。已读取 `album_artist` 列用于专辑归并，
但 MediaProvider 本身就会给不同的 `album_artist` 分配不同的 `album_id`，
所以合辑要正确归为一张专辑，前提是文件里有正确的 `ALBUMARTIST` 标签。

---

## 扫描性能实测

| 歌曲数 | MediaStore 扫描 | 年份补齐 | 合计 |
|---|---|---|---|
| 36 首 | **59ms** | 249ms / 32 首 | ~310ms |
| **1010 首** | **68ms** | **854ms / 1005 首** | **~920ms** |

验收目标是「1000 首扫描 < 3 秒」，**实测远优于目标**。

年份补齐之所以能在 854ms 内处理 1000 首，是因为 `TagReader` 只做定点读取
（逐块跳过封面等大块，只读 VORBIS_COMMENT / ID3v2 标签体），不把整个文件读进内存。

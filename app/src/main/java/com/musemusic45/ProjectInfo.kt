package com.musemusic45

/**
 * 项目对外信息。
 *
 * 单独放一个文件是为了**只有一个出处** —— 设置页的版本号、GitHub 入口、
 * README 里写的地址，都从这里来，改的时候不会漏掉某一处。
 */
object ProjectInfo {

    /** GitHub 仓库地址。 */
    const val REPO_URL = "https://github.com/45steel/musemusic"

    /** 设置页「关于」里显示的仓库名。 */
    const val REPO_LABEL = "45steel/musemusic"

    /** 项目名。 */
    const val NAME = "缪斯音乐"

    /** 一句话介绍。 */
    const val TAGLINE = "纯本地 Android 音乐播放器，完全不联网"
}

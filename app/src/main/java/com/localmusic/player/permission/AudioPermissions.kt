package com.localmusic.player.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 读取音频的权限。
 *
 * - Android 13（API 33）起用 READ_MEDIA_AUDIO
 * - 更早版本用 READ_EXTERNAL_STORAGE
 * - Android 6（API 23）起必须运行时申请；minSdk 26 所以总是要申请
 */
object AudioPermissions {

    val required: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasPermission(context: Context): Boolean = required.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isGranted(granted: Map<String, Boolean>): Boolean =
        required.all { granted[it] == true }
}

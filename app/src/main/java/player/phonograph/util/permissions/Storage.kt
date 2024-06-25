/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment

val necessaryStorageReadPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
val necessaryStorageWritePermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Manifest.permission.MANAGE_EXTERNAL_STORAGE else Manifest.permission.WRITE_EXTERNAL_STORAGE

fun hasStorageReadPermission(context: Context): Boolean =
    hasPermission(context, necessaryStorageReadPermission)

fun hasStorageWritePermission(context: Context): Boolean = when {
    /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
    else                                           -> hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
}
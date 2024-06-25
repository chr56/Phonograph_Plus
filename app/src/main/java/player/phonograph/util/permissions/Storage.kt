/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment


interface IStoragePermissionChecker {
    val necessaryStorageReadPermission: String
    val necessaryStorageWritePermission: String
    fun hasStorageReadPermission(context: Context): Boolean
    fun hasStorageWritePermission(context: Context): Boolean
}

object StoragePermissionChecker : IStoragePermissionChecker {

    override val necessaryStorageReadPermission: String
        get() = if (SDK_INT >= VERSION_CODES.TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE
    override val necessaryStorageWritePermission: String
        get() = if (SDK_INT >= VERSION_CODES.R) MANAGE_EXTERNAL_STORAGE else WRITE_EXTERNAL_STORAGE

    override fun hasStorageReadPermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageReadPermission)

    override fun hasStorageWritePermission(context: Context): Boolean = when {
        /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
        SDK_INT >= VERSION_CODES.R -> Environment.isExternalStorageManager()
        else                       -> hasPermission(context, WRITE_EXTERNAL_STORAGE)
    }
}
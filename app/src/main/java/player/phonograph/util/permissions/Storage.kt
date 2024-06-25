/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import androidx.annotation.RequiresApi
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
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

val StoragePermissionChecker: IStoragePermissionChecker
    get() = when {
        SDK_INT >= VERSION_CODES.R -> StoragePermissionCheckerR
        else                       -> StoragePermissionCheckerM
    }

@RequiresApi(VERSION_CODES.R)
object StoragePermissionCheckerR : IStoragePermissionChecker {

    override val necessaryStorageReadPermission: String
        get() = if (SDK_INT >= VERSION_CODES.TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE
    override val necessaryStorageWritePermission: String
        get() = MANAGE_EXTERNAL_STORAGE

    override fun hasStorageReadPermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageReadPermission)

    /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
    override fun hasStorageWritePermission(context: Context): Boolean =
        Environment.isExternalStorageManager()
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(VERSION_CODES.M)
object StoragePermissionCheckerM : IStoragePermissionChecker {

    override val necessaryStorageReadPermission: String = READ_EXTERNAL_STORAGE
    override val necessaryStorageWritePermission: String = WRITE_EXTERNAL_STORAGE

    override fun hasStorageReadPermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageReadPermission)

    override fun hasStorageWritePermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageWritePermission)
}
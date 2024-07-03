/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import androidx.annotation.RequiresApi
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment


////////////////////////////////////////////
/////////// Normal Flavor Variant //////////
////////////////////////////////////////////


/**
 * [IStoragePermissionChecker] implementation for Android R and above
 */
@RequiresApi(Build.VERSION_CODES.R)
object StoragePermissionCheckerR : IStoragePermissionChecker {

    override val necessaryStorageReadPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    override val necessaryStorageWritePermission: String
        get() = Manifest.permission.MANAGE_EXTERNAL_STORAGE

    override fun hasStorageReadPermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageReadPermission)

    /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
    override fun hasStorageWritePermission(context: Context): Boolean =
        Environment.isExternalStorageManager()
}

/**
 * [IStoragePermissionChecker] implementation for Android M and above
 */
@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.M)
object StoragePermissionCheckerM : IStoragePermissionChecker {

    override val necessaryStorageReadPermission: String = Manifest.permission.READ_EXTERNAL_STORAGE
    override val necessaryStorageWritePermission: String = Manifest.permission.WRITE_EXTERNAL_STORAGE

    override fun hasStorageReadPermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageReadPermission)

    override fun hasStorageWritePermission(context: Context): Boolean =
        hasPermission(context, necessaryStorageWritePermission)
}
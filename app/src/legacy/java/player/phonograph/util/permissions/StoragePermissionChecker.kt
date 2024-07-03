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
/////////// Legacy Flavor Variant //////////
////////////////////////////////////////////

/**
 * [IStoragePermissionChecker] implementation for Android R and above
 *
 * **_NOTE_: This is for product flavor `legacy` (which has low `targetApi` to surpass Scope Storage)**
 */
@RequiresApi(Build.VERSION_CODES.R)
object StoragePermissionCheckerR : IStoragePermissionChecker by StoragePermissionCheckerM // legacy variant use low target sdk


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
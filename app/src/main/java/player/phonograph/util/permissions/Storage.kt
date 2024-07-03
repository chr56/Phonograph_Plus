/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES


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
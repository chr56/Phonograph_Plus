/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.permissions

import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PermissionResult
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Environment

fun hasPermission(context: Context, permissionId: String): Boolean =
    PermissionChecker.checkSelfPermission(context, permissionId) == PermissionChecker.PERMISSION_GRANTED

fun hasPermissions(context: Context, permissionIds: Array<String>): Map<String, Boolean> =
    permissionIds.associateWith { hasPermission(context, it) }

@PermissionResult
fun checkPermission(context: Context, permissionId: String): Int =
    PermissionChecker.checkSelfPermission(context, permissionId)

fun checkPermissions(context: Context, permissionIds: Array<String>): Map<String, Int> =
    permissionIds.associateWith { checkPermission(context, it) }

val necessaryStorageReadPermission: String
    get() = if (SDK_INT >= TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE

val necessaryStorageWritePermission: String
    get() = if (SDK_INT >= VERSION_CODES.R) MANAGE_EXTERNAL_STORAGE else WRITE_EXTERNAL_STORAGE

fun hasStorageReadPermission(context: Context): Boolean =
    hasPermission(context, necessaryStorageReadPermission)

fun hasStorageWritePermission(context: Context): Boolean = when {
    /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
    SDK_INT >= VERSION_CODES.R -> Environment.isExternalStorageManager()
    else                       -> hasPermission(context, WRITE_EXTERNAL_STORAGE)
}

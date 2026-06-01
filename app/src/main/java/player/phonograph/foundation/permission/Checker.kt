/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.foundation.permission

import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PermissionResult
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.provider.Settings

fun hasPermission(context: Context, permissionId: String): Boolean =
    PermissionChecker.checkSelfPermission(context, permissionId) == PermissionChecker.PERMISSION_GRANTED

fun hasPermissions(context: Context, permissionIds: Array<String>): Map<String, Boolean> =
    permissionIds.associateWith { hasPermission(context, it) }

@PermissionResult
fun checkPermission(context: Context, permissionId: String): Int =
    PermissionChecker.checkSelfPermission(context, permissionId)

fun checkPermissions(context: Context, permissionIds: Array<String>): Map<String, Int> =
    permissionIds.associateWith { checkPermission(context, it) }

fun checkNotificationPermission(context: Context): Boolean =
    if (SDK_INT > TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

fun checkModificationSystemSettingsPermission(context: Context): Boolean = !Settings.System.canWrite(context)
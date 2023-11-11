/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.permissions

import androidx.core.content.PermissionChecker
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Environment

fun checkPermission(context: Context, permissionId: String): Permission {
    val result = PermissionChecker.checkSelfPermission(context, permissionId)
    return if (result == PermissionChecker.PERMISSION_GRANTED) {
        GrantedPermission(permissionId)
    } else {
        NonGrantedPermission(permissionId)
    }
}


fun checkPermissions(context: Context, permissionIds: Array<String>): List<Permission> =
    permissionIds.map { permissionId ->
        checkPermission(context, permissionId)
    }

fun checkStorageReadPermission(context: Context): Permission =
    checkPermission(
        context, if (SDK_INT >= TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE
    )

fun hasStorageReadPermission(context: Context): Boolean =
    checkStorageReadPermission(context) is GrantedPermission

fun hasStorageWritePermission(context: Context): Boolean = when {
    /** check [MANAGE_EXTERNAL_STORAGE] on Android R and above **/
    SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
    else                             -> checkPermission(context, WRITE_EXTERNAL_STORAGE) is GrantedPermission
}

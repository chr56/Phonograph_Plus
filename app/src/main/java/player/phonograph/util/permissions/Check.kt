/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import androidx.core.content.PermissionChecker
import android.Manifest
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU

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

fun checkStorageReadPermission(context: Context) =
    checkPermission(
        context, if (SDK_INT >= TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE
    )

fun hasStorageReadPermission(context: Context): Boolean =
    checkStorageReadPermission(context) is GrantedPermission

fun checkStorageWritePermission(context: Context): Permission = when {
    SDK_INT >= Build.VERSION_CODES.R -> checkPermission(context, MANAGE_EXTERNAL_STORAGE)
    else                             -> checkPermission(context, WRITE_EXTERNAL_STORAGE)
}

fun hasStorageWritePermission(context: Context): Boolean =
    checkStorageWritePermission(context) is GrantedPermission


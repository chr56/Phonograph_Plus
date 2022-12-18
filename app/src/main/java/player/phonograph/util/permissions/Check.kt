/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import androidx.core.content.PermissionChecker
import android.content.Context

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
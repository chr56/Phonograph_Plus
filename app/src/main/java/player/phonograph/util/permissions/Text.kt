/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import android.content.Context
import android.content.pm.PermissionInfo

fun permissionInfo(context: Context, permissionId: String): PermissionInfo =
    context.packageManager.getPermissionInfo(permissionId, 0)

fun permissionName(context: Context, permissionId: String): CharSequence {
    val info = permissionInfo(context, permissionId)
    return info.loadLabel(context.packageManager)
}

fun permissionDescription(context: Context, permissionId: String): CharSequence? {
    val info = permissionInfo(context, permissionId)
    return info.loadDescription(context.packageManager)
}
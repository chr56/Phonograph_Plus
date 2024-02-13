/*
 * Copyright (c) 2022 chr_56
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


sealed class Permission(val permissionId: String) {
    fun permissionInfo(context: Context): PermissionInfo = permissionInfo(context, permissionId)
    fun permissionName(context: Context): CharSequence = permissionName(context, permissionId)
    fun permissionDescription(context: Context): CharSequence? = permissionDescription(context, permissionId)
}


/**
 * gained, no matter what kind it is.
 */
class GrantedPermission(permissionId: String) : Permission(permissionId)

open class NonGrantedPermission(permissionId: String) : Permission(permissionId) {

    /**
     * manually grant in setting required
     */
    open class PermanentlyDeniedPermission(permissionId: String) :
            NonGrantedPermission(permissionId) {

        class SpecialPermission(permissionId: String) :
                PermanentlyDeniedPermission(permissionId)
    }

    /**
     * grand via Dialog
     */
    open class RequestRequiredPermission(permissionId: String) :
            NonGrantedPermission(permissionId) {

        class ShouldShowRationaleDeniedPermission(permissionId: String) :
                RequestRequiredPermission(permissionId)
    }
}

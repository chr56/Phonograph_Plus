/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.request.PermissionRequest
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


fun FragmentActivity.generatePermissionRequest(permissions: Array<String>): PermissionRequest {
    require(permissions.isNotEmpty()) { "No permissions to request!" }
    if (permissions.size == 1) return permissionsBuilder(permissions[0]).build()
    val head = permissions.first()
    val tail = permissions.sliceArray(1 until permissions.size)
    return permissionsBuilder(head, *tail).build()
}

fun Fragment.generatePermissionRequest(permissions: Array<String>): PermissionRequest {
    require(permissions.isNotEmpty()) { "No permissions to request!" }
    if (permissions.size == 1) return permissionsBuilder(permissions[0]).build()
    val head = permissions.first()
    val tail = permissions.sliceArray(1 until permissions.size)
    return permissionsBuilder(head, *tail).build()
}
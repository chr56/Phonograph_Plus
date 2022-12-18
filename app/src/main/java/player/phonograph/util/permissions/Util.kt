/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import com.fondesa.kpermissions.PermissionStatus
import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.request.PermissionRequest
import player.phonograph.MusicServiceMsgConst
import android.content.Context
import android.content.Intent


/**
 * @return list of Pair<permission: String, requireGotoSetting: Boolean>
 */
suspend fun requestOrCheckPermissionStatus(
    context: Context,
    request: PermissionRequest,
    checkOnly: Boolean
): List<Pair<String, Boolean>> {
    val result = if (checkOnly) request.checkStatus() else request.sendSuspend()
    // checking
    val missingPermissions = mutableListOf<Pair<String, Boolean>>()
    for (permissionStatus in result) {
        if (permissionStatus is PermissionStatus.Granted) {
            context.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
            continue
        }
        val requireGotoSetting = permissionStatus is PermissionStatus.Denied.Permanently
        missingPermissions.add(permissionStatus.permission to requireGotoSetting)
    }
    return missingPermissions
}


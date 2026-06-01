/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.permission

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

class PermissionDetail(
    /** full permission string **/
    val permission: String,
    val required: Boolean = true,
)

val necessaryPermissions: List<PermissionDetail>
    get() = when {
        SDK_INT >= VERSION_CODES.TIRAMISU -> NecessaryPermissionsT.all
        else                              -> NecessaryPermissionsM.all
    }



interface INecessaryPermissions {
    val all: List<PermissionDetail>
}

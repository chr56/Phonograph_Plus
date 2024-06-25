/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import androidx.annotation.RequiresApi
import android.Manifest
import android.annotation.SuppressLint
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

@RequiresApi(VERSION_CODES.TIRAMISU)
object NecessaryPermissionsT : INecessaryPermissions {
    override val all: List<PermissionDetail>
        get() = listOf(
            PermissionDetail(Manifest.permission.POST_NOTIFICATIONS),
            PermissionDetail(Manifest.permission.READ_MEDIA_AUDIO),
        )
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(VERSION_CODES.M)
object NecessaryPermissionsM : INecessaryPermissions {
    override val all: List<PermissionDetail>
        get() = listOf(
            PermissionDetail(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
}
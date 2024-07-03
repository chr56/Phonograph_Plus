/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import androidx.annotation.RequiresApi
import android.Manifest
import android.annotation.SuppressLint
import android.os.Build


////////////////////////////////////////////
/////////// Normal Flavor Variant //////////
////////////////////////////////////////////

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object NecessaryPermissionsT : INecessaryPermissions {
    override val all: List<PermissionDetail>
        get() = listOf(
            PermissionDetail(Manifest.permission.POST_NOTIFICATIONS),
            PermissionDetail(Manifest.permission.READ_MEDIA_AUDIO),
        )
}
@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.M)
object NecessaryPermissionsM : INecessaryPermissions {
    override val all: List<PermissionDetail>
        get() = listOf(
            PermissionDetail(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
}
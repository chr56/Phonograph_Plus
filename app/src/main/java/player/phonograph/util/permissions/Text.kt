/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import player.phonograph.R
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PermissionInfo

fun permissionInfo(context: Context, permissionId: String): PermissionInfo =
    context.packageManager.getPermissionInfo(permissionId, 0)

fun permissionName(context: Context, permissionId: String): CharSequence {
    val stringRes = when (permissionId) {
        POST_NOTIFICATIONS      -> R.string.permission_name_post_notifications
        READ_MEDIA_AUDIO        -> R.string.permission_name_read_media_audio
        READ_EXTERNAL_STORAGE   -> R.string.permission_name_read_external_storage
        WRITE_EXTERNAL_STORAGE  -> R.string.permission_name_write_external_storage
        MANAGE_EXTERNAL_STORAGE -> R.string.permission_name_manage_external_storage
        else                    -> 0
    }
    return if (stringRes > 0) {
        context.getString(stringRes)
    } else {
        permissionInfo(context, permissionId).loadLabel(context.packageManager)
    }
}

fun permissionDescription(context: Context, permissionId: String): CharSequence? {
    val stringRes = when (permissionId) {
        POST_NOTIFICATIONS      -> R.string.permission_desc_post_notifications
        READ_MEDIA_AUDIO        -> R.string.permission_desc_read_media_audio
        READ_EXTERNAL_STORAGE   -> R.string.permission_desc_read_external_storage
        WRITE_EXTERNAL_STORAGE  -> R.string.permission_desc_write_external_storage
        MANAGE_EXTERNAL_STORAGE -> R.string.permission_desc_manage_external_storage
        else                    -> 0
    }
    return if (stringRes > 0) {
        context.getString(stringRes)
    } else {
        permissionInfo(context, permissionId).loadDescription(context.packageManager)
    }
}
/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

fun navigateToStorageSetting(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent()
    intent.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            data = uri
        } else {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = uri
        }
    }
    try {
        context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "${e.message?.take(48)}", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
    }
}

fun navigateToAppDetailSetting(context: Context) {
    context.startActivity(
        Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
        }
    )
}
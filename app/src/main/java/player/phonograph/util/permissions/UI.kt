/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import com.google.android.material.snackbar.Snackbar
import mt.pref.ThemeColor
import player.phonograph.R
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast

@MainThread
fun notifyUser(
    context: FragmentActivity,
    missingPermissions: List<Permission>,
    snackBarContainer: View?,
    retryCallback: (() -> Unit)?
) {
    if (missingPermissions.isEmpty()) return

    val message = StringBuffer("${context.getString(R.string.permissions_denied)}:")
    var requireGotoSetting = false
    for (permission in missingPermissions) {
        message.append(permission.permissionName(context))
        if (permission is NonGrantedPermission.PermanentlyDeniedPermission)
            requireGotoSetting = true
    }

    if (snackBarContainer != null) {
        val snackBar = Snackbar.make(snackBarContainer, message, Snackbar.LENGTH_INDEFINITE)
        if (requireGotoSetting) {
            snackBar.setAction(R.string.action_settings) { navigateToAppDetailSetting(context) }
        } else {
            snackBar.setAction(R.string.action_grant) { retryCallback?.invoke() }
        }
        snackBar.setActionTextColor(ThemeColor.accentColor(context))
        mainThread { snackBar.show() }
    } else {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        mainThread { toast.show() }
    }
}

private inline fun mainThread(crossinline block: () -> Unit) =
    Handler(Looper.getMainLooper()).post { block() }

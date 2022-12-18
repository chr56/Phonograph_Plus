/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import com.google.android.material.snackbar.Snackbar
import mt.pref.ThemeColor
import player.phonograph.R
import android.content.Context
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun notifyUser(
    context: Context,
    missingPermissions: List<Pair<String, Boolean>>,
    snackBarContainer: View?,
    retry: (() -> Unit)?
) {
    if (missingPermissions.isEmpty()) return

    val msg = missingPermissions.fold("") { acc, pair -> "$acc,${pair.first}" }
    val requireGotoSetting = missingPermissions.asSequence()
        .map { it.second }.reduce { acc, b -> if (acc) true else b }

    if (snackBarContainer != null) {
        val snackBar = Snackbar.make(
            snackBarContainer,
            "${context.getString(R.string.permissions_denied)}\n${msg}",
            Snackbar.LENGTH_INDEFINITE
        )
        if (requireGotoSetting) {
            snackBar.setAction(R.string.action_settings) { navigateToAppDetailSetting(context) }
        } else {
            snackBar.setAction(R.string.action_grant) { retry?.invoke() }
        }
        snackBar.setActionTextColor(ThemeColor.accentColor(context))
        withContext(Dispatchers.Main) { snackBar.show() }
    } else {
        val toast = Toast.makeText(context, R.string.permissions_denied, Toast.LENGTH_SHORT)
        withContext(Dispatchers.Main) { toast.show() }
    }
}
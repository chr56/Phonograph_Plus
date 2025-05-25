/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.basis

import com.google.android.material.snackbar.Snackbar
import lib.activityresultcontract.IRequestMultiplePermission
import lib.activityresultcontract.RequestMultiplePermissionsDelegate
import player.phonograph.R
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.concurrent.runOnMainHandler
import player.phonograph.util.permissions.hasPermissions
import player.phonograph.util.permissions.navigateToAppDetailSetting
import player.phonograph.util.permissions.permissionDescription
import player.phonograph.util.permissions.permissionName
import android.os.Bundle

/**
 * @author chr_56, Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ThemeActivity(), IRequestMultiplePermission {

    override val requestMultiplePermissionsDelegate: RequestMultiplePermissionsDelegate =
        RequestMultiplePermissionsDelegate()

    /**
     * Runtime Permissions to request
     */
    protected open fun runtimePermissionsToRequest(): Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestMultiplePermissionsDelegate.register(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    /**
     * Check permissions from [runtimePermissionsToRequest]
     * @return true if all granted
     */
    protected fun checkPermissions(): Boolean {
        val permissions = runtimePermissionsToRequest() ?: return true
        val result = hasPermissions(this, permissions)
        return checkResult(result)
    }

    /**
     * Request permissions from [runtimePermissionsToRequest]
     */
    protected fun requestPermissions() {
        val permissions = runtimePermissionsToRequest() ?: return
        requestMultiplePermissionsDelegate.launch(permissions) {
            checkResult(it)
        }
    }

    /**
     * check and notify permission grant result to user if denied
     * @param result All result
     */
    private fun checkResult(result: Map<String, Boolean>): Boolean {
        val denied = result.filterValues { !it }
        return if (denied.isNotEmpty()) {
            notifyPermissionDeniedUser(denied.keys.toList(), ::requestPermissions)
            false
        } else {
            true
        }
    }

    /**
     * Show SnackBar message if permission denied
     * @param retryCallback callback of SnackBar
     */
    protected fun notifyPermissionDeniedUser(
        missingPermissions: List<String>,
        retryCallback: (() -> Unit)?,
    ) {
        if (missingPermissions.isEmpty()) return

        val message = StringBuffer(getString(R.string.err_permissions_denied)).append('\n')
        var requireGotoSetting = false
        for (permission in missingPermissions) {
            message
                .append(permissionName(this, permission)).append('\n')
                .append(permissionDescription(this, permission)).append('\n')
            if (shouldShowRequestPermissionRationale(permission)) requireGotoSetting = true
        }

        val snackBar = Snackbar.make(snackBarContainer, message, Snackbar.LENGTH_INDEFINITE)
        if (requireGotoSetting) {
            snackBar.setAction(R.string.action_settings) { navigateToAppDetailSetting(this) }
        } else {
            snackBar.setAction(R.string.action_grant) { retryCallback?.invoke() }
        }
        snackBar.setActionTextColor(ThemeSetting.accentColor(this)).setTextMaxLines(Int.MAX_VALUE)
        runOnMainHandler { snackBar.show() }
    }
}
/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import com.google.android.material.snackbar.Snackbar
import player.phonograph.R
import player.phonograph.util.permissions.GrantResult
import player.phonograph.util.permissions.PermissionDelegate
import player.phonograph.util.permissions.hasPermissions
import player.phonograph.util.permissions.navigateToAppDetailSetting
import player.phonograph.util.permissions.permissionDescription
import player.phonograph.util.permissions.permissionName
import player.phonograph.util.runOnMainHandler
import android.os.Bundle

/**
 * @author chr_56, Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ThemeActivity() {

    private val permissionDelegate: PermissionDelegate = PermissionDelegate()

    /**
     * Runtime Permissions to request
     */
    protected open fun runtimePermissionsToRequest(): Array<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        permissionDelegate.register(lifecycle, activityResultRegistry)
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
        permissionDelegate.grant(permissions) { result: GrantResult ->
            checkResult(result)
        }
    }

    /**
     * check and notify permission grant result to user if denied
     * @param result All result
     */
    private fun checkResult(result: GrantResult): Boolean {
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

        val message = StringBuffer(getString(R.string.permissions_denied)).append('\n')
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
        snackBar.setActionTextColor(accentColor).setTextMaxLines(Int.MAX_VALUE)
        runOnMainHandler { snackBar.show() }
    }
}
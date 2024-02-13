/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import com.google.android.material.snackbar.Snackbar
import player.phonograph.R
import player.phonograph.util.permissions.GrantedPermission
import player.phonograph.util.permissions.NonGrantedPermission
import player.phonograph.util.permissions.Permission
import player.phonograph.util.permissions.PermissionDelegate
import player.phonograph.util.permissions.RequestCallback
import player.phonograph.util.permissions.checkPermissions
import player.phonograph.util.permissions.convertPermissionsResult
import player.phonograph.util.permissions.navigateToAppDetailSetting
import player.phonograph.util.runOnMainHandler
import android.os.Bundle

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ThemeActivity() {

    private val permissionDelegate: PermissionDelegate = PermissionDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        permissionDelegate.attach(this)
        super.onCreate(savedInstanceState)
    }

    protected fun requestPermissionImpl(permissions: Array<String>, callback: RequestCallback) {
        permissionDelegate.grant(permissions, callback)
    }

    protected open fun runtimePermissionsToRequest(): Array<String>? = null

    protected open fun requestPermissions() {
        val permissions = runtimePermissionsToRequest() ?: return
        requestPermissionImpl(permissions) { result ->
            notifyResult(convertPermissionsResult(result))
        }
    }

    protected open fun checkPermissions(): Boolean {
        val permissions = runtimePermissionsToRequest() ?: return true
        val result = checkPermissions(this, permissions)
        return notifyResult(result)
    }

    private fun notifyResult(result: List<Permission>): Boolean {
        val allGranted = result.fold(true) { acc, i -> if (!acc) false else i is GrantedPermission }
        if (!allGranted) {
            val nonGranted = result.filterIsInstance<NonGrantedPermission>()
            notifyPermissionDeniedUser(nonGranted, ::requestPermissions)
        }
        return allGranted
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    protected open fun missingPermissionCallback() {}

    protected fun notifyPermissionDeniedUser(
        missingPermissions: List<Permission>,
        retryCallback: (() -> Unit)?
    ) {
        if (missingPermissions.isEmpty()) return

        val message = StringBuffer(getString(R.string.permissions_denied)).append('\n')
        var requireGotoSetting = false
        for (permission in missingPermissions) {
            message.append(permission.permissionName(this)).append('\n')
            if (permission is NonGrantedPermission.PermanentlyDeniedPermission)
                requireGotoSetting = true
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
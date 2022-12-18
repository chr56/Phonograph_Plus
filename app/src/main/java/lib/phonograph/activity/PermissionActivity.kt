/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import player.phonograph.util.permissions.GrantedPermission
import player.phonograph.util.permissions.NonGrantedPermission
import player.phonograph.util.permissions.Permission
import player.phonograph.util.permissions.PermissionDelegate
import player.phonograph.util.permissions.RequestCallback
import player.phonograph.util.permissions.checkPermissions
import player.phonograph.util.permissions.convertPermissionsResult
import player.phonograph.util.permissions.notifyPermissionUser
import android.os.Bundle
import android.os.PersistableBundle

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
            val other = result.filterIsInstance<NonGrantedPermission>()
            notifyPermissionUser(this, other, snackBarContainer, ::requestPermissions)
        }
        return allGranted
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    protected open fun missingPermissionCallback() {}
}

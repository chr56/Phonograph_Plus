/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import player.phonograph.util.permissions.generatePermissionRequest
import player.phonograph.util.permissions.notifyUser
import player.phonograph.util.permissions.requestOrCheckPermissionStatus
import android.os.Bundle
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ThemeActivity() {

    protected open fun runtimePermissionsToRequest(): Array<String>? = null

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private fun requestOrCheckPermission(permissions: Array<String>, checkOnly: Boolean) {
        scope.launch {
            val result = requestOrCheckPermissionStatus(
                this@PermissionActivity, generatePermissionRequest(permissions), checkOnly
            )
            if (result.isNotEmpty()) {
                notifyUser(this@PermissionActivity, result, snackBarContainer) {
                    requestOrCheckPermission(result.map { it.first }.toTypedArray(), false)
                }
                missingPermissionCallback()
            }
        }
    }

    protected open fun requestPermissions() {
        val permissions = runtimePermissionsToRequest() ?: return
        requestOrCheckPermission(permissions = permissions, checkOnly = false)
    }

    protected open fun checkPermissions() {
        val permissions = runtimePermissionsToRequest() ?: return
        requestOrCheckPermission(permissions = permissions, checkOnly = true)
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

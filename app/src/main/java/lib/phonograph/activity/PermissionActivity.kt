/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.isGranted
import com.fondesa.kpermissions.request.PermissionRequest
import com.fondesa.kpermissions.shouldShowRationale
import com.google.android.material.snackbar.Snackbar
import player.phonograph.R
import player.phonograph.util.PermissionUtil
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ThemeActivity() {

    protected open fun runtimePermissionsToRequest(): Array<String>? = null

    private fun requestOrCheckPermission(checkOnly: Boolean) {
        val permissions = runtimePermissionsToRequest() ?: return
        PermissionUtil.requestOrCheckPermission(
            this,
            permissions,
            checkOnly,
            snackBarContainer,
            ::missingPermissionCallback
        )
    }

    protected open fun requestPermissions() {
        requestOrCheckPermission(checkOnly = false)
    }

    protected open fun checkPermissions() {
        requestOrCheckPermission(checkOnly = true)
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

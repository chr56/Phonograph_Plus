/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import player.phonograph.R
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ToolbarActivity() {

    private var permissions: Array<String>? = null
    protected open fun getPermissionsToRequest(): Array<String>? = null

    private var hadPermissions = false
    protected fun hasPermissions(): Boolean {
        permissions?.let {
// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions!!) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
// }
        }
        return true
    }

    protected open fun requestPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        permissions?.let { requestPermissions(it, PERMISSION_REQUEST) }
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissions = getPermissionsToRequest()
        hadPermissions = hasPermissions()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!hasPermissions()) {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = hasPermissions()
        if (hasPermissions != hadPermissions) {
            hadPermissions = hasPermissions
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onHasPermissionsChanged(hasPermissions) // callback
//            }
        }
    }
    protected open fun onHasPermissionsChanged(hasPermissions: Boolean) { /*implemented by sub classes */ }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@PermissionActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        // User has deny from permission dialog
                        Snackbar.make(
                            snackBarContainer, permissionDeniedMessage!!,
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(R.string.action_grant) { requestPermissions() }
                            .setActionTextColor(ThemeColor.accentColor(this))
                            .show()
                    } else {
                        // User has deny permission and checked never show permission dialog so you can redirect to Application settings page
                        Snackbar.make(
                            snackBarContainer, permissionDeniedMessage!!,
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(R.string.action_settings) {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package", this@PermissionActivity.packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                            .setActionTextColor(ThemeColor.accentColor(this))
                            .show()
                    }
                    return
                }
            }
            hadPermissions = true
            onHasPermissionsChanged(true)
        }
    }

    protected var permissionDeniedMessage: String? = null
        get() =
            if (field == null) getString(R.string.permissions_denied) else field

    companion object {
        const val PERMISSION_REQUEST = 100
    }
}

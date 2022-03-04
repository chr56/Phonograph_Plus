package player.phonograph.ui.activities.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.R
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PermissionActivity : ToolbarActivity() {

    companion object {
        const val PERMISSION_REQUEST = 100
    }

    private var hadPermissions = false
    private var permissions: Array<String>? = null

    private var permissionDeniedMessage: String? = null
    protected open fun setPermissionDeniedMessage(message: String) {
        permissionDeniedMessage = message
    }
    protected open fun getPermissionDeniedMessage(): String? {
        return if (permissionDeniedMessage == null) getString(R.string.permissions_denied) else permissionDeniedMessage
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        permissions = getPermissionsToRequest()
        hadPermissions = hasPermissions()
        permissionDeniedMessage = null
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
            onHasPermissionsChanged(hasPermissions)
//            }
        }
    }

    protected open fun onHasPermissionsChanged(hasPermissions: Boolean) {
        // implemented by sub classes
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            showOverflowMenu()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    protected open fun showOverflowMenu() {}

    protected open fun getPermissionsToRequest(): Array<String>? = null

    protected open val snackBarContainer: View get() = window.decorView

    protected open fun requestPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
        requestPermissions(permissions!!, PERMISSION_REQUEST)
//        }
    }

    protected fun hasPermissions(): Boolean {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
        for (permission in permissions!!) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
//        }
        return true
    }

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
}

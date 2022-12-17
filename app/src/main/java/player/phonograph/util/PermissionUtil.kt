/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.isGranted
import com.fondesa.kpermissions.isPermanentlyDenied
import com.fondesa.kpermissions.request.PermissionRequest
import com.google.android.material.snackbar.Snackbar
import mt.pref.ThemeColor
import player.phonograph.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PermissionUtil {

    fun FragmentActivity.generatePermissionRequest(permissions: Array<String>): PermissionRequest {
        require(permissions.isNotEmpty()) { "No permissions to request!" }
        if (permissions.size == 1) return permissionsBuilder(permissions[0]).build()
        val head = permissions.first()
        val tail = permissions.sliceArray(1 until permissions.size)
        return permissionsBuilder(head, *tail).build()
    }

    fun Fragment.generatePermissionRequest(permissions: Array<String>): PermissionRequest {
        require(permissions.isNotEmpty()) { "No permissions to request!" }
        if (permissions.size == 1) return permissionsBuilder(permissions[0]).build()
        val head = permissions.first()
        val tail = permissions.sliceArray(1 until permissions.size)
        return permissionsBuilder(head, *tail).build()
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined)


    suspend fun requestOrCheckPermission(
        context: Context,
        request: PermissionRequest,
        checkOnly: Boolean,
        snackBarContainer: View? = null,
        callback: (() -> Unit)? = null
    ) {
        val result = if (checkOnly) request.checkStatus() else request.sendSuspend()
        val missingPermissions = mutableListOf<String>()
        for (permissionStatus in result) {
            if (!permissionStatus.isGranted()) {
                if (snackBarContainer != null) {
                    val snackBar = Snackbar.make(
                        snackBarContainer,
                        "${context.getString(R.string.permissions_denied)}\n${permissionStatus.permission}",
                        Snackbar.LENGTH_SHORT
                    )
                    if (permissionStatus.isPermanentlyDenied()) {
                        snackBar.setAction(R.string.action_settings) {
                            context.startActivity(
                                Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    } else {
                        snackBar.setAction(R.string.action_grant) {
                            scope.launch {
                                requestOrCheckPermission(context, request, false, snackBarContainer)
                            }
                        }
                    }
                    snackBar.setActionTextColor(ThemeColor.accentColor(context))
                    withContext(Dispatchers.Main) {
                        snackBar.show()
                    }
                } else {
                    val toast =
                        Toast.makeText(
                            context,
                            R.string.permissions_denied,
                            Toast.LENGTH_SHORT
                        )
                    withContext(Dispatchers.Main) {
                        toast.show()
                    }
                }
                missingPermissions.add(permissionStatus.permission)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            callback?.invoke()
        }
    }


    fun requestOrCheckPermission(
        fragmentActivity: FragmentActivity,
        permissions: Array<String>,
        checkOnly: Boolean,
        snackBarContainer: View? = null,
        callback: (() -> Unit)? = null
    ) {
        if (permissions.isNotEmpty()) {
            val request = fragmentActivity.generatePermissionRequest(permissions)
            scope.launch {
                requestOrCheckPermission(
                    fragmentActivity,
                    request,
                    checkOnly,
                    snackBarContainer,
                    callback
                )
            }
        }
    }

    fun requestOrCheckPermission(
        fragment: Fragment,
        permissions: Array<String>,
        checkOnly: Boolean,
        snackBarContainer: View? = null,
        callback: (() -> Unit)? = null
    ) {
        if (permissions.isNotEmpty()) {
            val context = fragment.requireContext()
            val request = fragment.generatePermissionRequest(permissions)
            scope.launch {
                requestOrCheckPermission(
                    context,
                    request,
                    checkOnly,
                    snackBarContainer,
                    callback
                )
            }
        }
    }



    fun navigateToStorageSetting(context: Context) {
        val uri = Uri.parse("package:${context.packageName}")
        val intent = Intent()
        intent.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                data = uri
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = uri
            }
        }
        try {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "${e.message?.take(48)}", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
        }
    }
}
/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import com.fondesa.kpermissions.PermissionStatus
import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.request.PermissionRequest
import com.google.android.material.snackbar.Snackbar
import mt.pref.ThemeColor
import player.phonograph.MusicServiceMsgConst
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

    suspend fun requestOrCheckPermission(
        context: Context,
        request: PermissionRequest,
        checkOnly: Boolean,
        snackBarContainer: View? = null,
        callback: (() -> Unit)? = null
    ) {
        val result = requestOrCheckPermissionStatus(request, checkOnly)
        if (result.isNotEmpty()) {
            notifyUser(context, result, snackBarContainer) {
                request.send()
                context.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
                scope.launch {
                    requestOrCheckPermission(
                        context,
                        request,
                        true,
                        snackBarContainer,
                        callback
                    )
                }
            }
            callback?.invoke()
        }
    }

    /**
     * @return list of Pair<permission: String, requireGotoSetting: Boolean>
     */
    suspend fun requestOrCheckPermissionStatus(
        request: PermissionRequest,
        checkOnly: Boolean
    ): List<Pair<String, Boolean>> {
        val result = if (checkOnly) request.checkStatus() else request.sendSuspend()
        // checking
        val missingPermissions = mutableListOf<Pair<String, Boolean>>()
        for (permissionStatus in result) {
            if (permissionStatus is PermissionStatus.Granted) continue
            val requireGotoSetting = permissionStatus is PermissionStatus.Denied.Permanently
            missingPermissions.add(permissionStatus.permission to requireGotoSetting)
        }
        return missingPermissions
    }

    private suspend fun notifyUser(
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


    fun navigateToAppDetailSetting(context: Context) {
        context.startActivity(
            Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
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
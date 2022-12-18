/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import lib.phonograph.activity.PermissionActivity
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun requestPermission(activity: PermissionActivity, permissionId: String): Permission =
    suspendCancellableCoroutine { continuation ->
        activity.requestPermission(arrayOf(permissionId)) {
            val (id, result) = it.entries.first()
            val ret = if (result) {
                GrantedPermission(id)
            } else {
                NonGrantedPermission(id)
            }
            continuation.resume(ret) { e ->
                Log.w(TAG, e)
            }
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun requestPermissions(activity: PermissionActivity, permissionIds: Array<String>): List<Permission> =
    suspendCancellableCoroutine { continuation ->
        activity.requestPermission(permissionIds) {
            val ret = it.entries.map { (id, result) ->
                if (result) {
                    GrantedPermission(id)
                } else {
                    NonGrantedPermission(id)
                }
            }
            continuation.resume(ret) { e ->
                Log.w(TAG, e)
            }
        }
    }

private const val TAG = "PermissionRequest"
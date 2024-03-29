/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import lib.activityresultcontract.ActivityResultContractTool
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle

typealias GrantResult = Map<String, Boolean>

class RequestPermissionsTool : ActivityResultContractTool<Array<String>, GrantResult>() {

    override fun key(): String = "GrantPermissions"

    override fun contract(): ActivityResultContract<Array<String>, GrantResult> =
        ActivityResultContracts.RequestMultiplePermissions()
}

class PermissionDelegate {

    private val requestPermissionsTool: RequestPermissionsTool = RequestPermissionsTool()

    fun register(lifeCycle: Lifecycle, registry: ActivityResultRegistry) {
        requestPermissionsTool.register(lifeCycle, registry)
    }

    fun grant(permissions: Array<String>, callback: (GrantResult) -> Unit) {
        requestPermissionsTool.launch(permissions, callback)
    }
}
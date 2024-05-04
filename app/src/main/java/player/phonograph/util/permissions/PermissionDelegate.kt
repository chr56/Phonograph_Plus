/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import lib.activityresultcontract.ActivityResultContractTool
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

typealias GrantResult = Map<String, Boolean>

class RequestPermissionsDelegate : ActivityResultContractTool<Array<String>, GrantResult>() {

    override fun key(): String = "GrantPermissions"

    override fun contract(): ActivityResultContract<Array<String>, GrantResult> =
        ActivityResultContracts.RequestMultiplePermissions()
}
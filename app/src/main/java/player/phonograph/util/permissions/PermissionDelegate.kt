/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.permissions

import lib.activityresultcontract.ActivityResultLauncherDelegate
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

typealias GrantResult = Map<String, Boolean>

class RequestPermissionsDelegate : ActivityResultLauncherDelegate<Array<String>, GrantResult>() {

    override val key: String = "GrantPermissions"
    override val contract: ActivityResultContract<Array<String>, GrantResult> =
        ActivityResultContracts.RequestMultiplePermissions()
}
/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import lib.activityresultcontract.ActivityResultLauncherDelegate
import androidx.activity.result.contract.ActivityResultContract

class PathSelectorContractTool : ActivityResultLauncherDelegate<String?, String?>() {
    override val key: String = "FileChooserContractTool"
    override val contract: ActivityResultContract<String?, String?> =
        PathSelectorDialogActivity.PathSelectorActivityResultContract()
}

interface PathSelectorRequester {
    val pathSelectorContractTool: PathSelectorContractTool
}
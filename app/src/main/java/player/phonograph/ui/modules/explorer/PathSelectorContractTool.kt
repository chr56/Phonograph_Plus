/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import lib.activityresultcontract.ActivityResultContractTool
import androidx.activity.result.contract.ActivityResultContract

class PathSelectorContractTool : ActivityResultContractTool<String?, String?>() {
    override fun key(): String = "FileChooserContractTool"
    override fun contract(): ActivityResultContract<String?, String?> =
        PathSelectorDialogActivity.PathSelectorActivityResultContract()
}

interface PathSelectorRequester {
    val pathSelectorContractTool: PathSelectorContractTool
}
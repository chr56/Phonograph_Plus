/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import lib.activityresultcontract.ActivityResultContractTool
import androidx.activity.result.contract.ActivityResultContract

class FileChooserContractTool : ActivityResultContractTool<String?, String?>() {
    override fun key(): String = "FileChooserContractTool"
    override fun contract(): ActivityResultContract<String?, String?> =
        FileChooserDialogActivity.FileChooserActivityResultContract()
}

interface FileChooserRequester {
    val fileChooserContractTool: FileChooserContractTool
}
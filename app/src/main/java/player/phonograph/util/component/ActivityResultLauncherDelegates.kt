/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.component

import lib.activityresultcontract.ActivityResultLauncherDelegate
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

class GetContentDelegate : ActivityResultLauncherDelegate<String, Uri?>() {
    override val key: String = "GetContent"
    override val contract: ActivityResultContract<String, Uri?> = ActivityResultContracts.GetContent()
}

interface IGetContentRequester {
    val getContentDelegate: GetContentDelegate
}
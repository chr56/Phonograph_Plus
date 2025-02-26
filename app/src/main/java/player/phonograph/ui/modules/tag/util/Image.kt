/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag.util

import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun selectImage(accessTool: OpenFileStorageAccessDelegate): Uri? {
    val cfg = OpenDocumentContract.Config(mimeTypes = arrayOf("image/*"))
    return suspendCancellableCoroutine {
        accessTool.launch(cfg) { uri: Uri? ->
            it.resume(uri) { _, _, _ -> }
        }
    }
}

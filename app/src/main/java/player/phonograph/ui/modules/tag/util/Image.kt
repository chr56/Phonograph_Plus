/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag.util

import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.util.ui.paletteColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

suspend fun selectImage(accessTool: OpenFileStorageAccessDelegate): Uri? {
    val cfg = OpenDocumentContract.Config(mimeTypes = arrayOf("image/*"))
    return suspendCancellableCoroutine {
        accessTool.launch(cfg) { uri: Uri? ->
            it.resume(uri) { _, _, _ -> }
        }
    }
}

suspend fun readImage(path: String): Pair<Bitmap?, Color?> {
    val imageBytes = JAudioTaggerExtractor.readImage(File(path))
    if (imageBytes == null || imageBytes.isEmpty()) return Pair(null, null)
    return try {
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return Pair(null, null)
        val paletteColor = paletteColor(bitmap, Color.Gray.toArgb())
        Pair(bitmap, Color(paletteColor))
    } catch (e: Exception) {
        Pair(null, null)
    }
}

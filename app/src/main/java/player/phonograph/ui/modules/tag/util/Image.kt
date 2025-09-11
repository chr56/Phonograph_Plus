/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag.util

import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.util.ui.BitmapUtil.decodeBitmapWithRestrictions
import player.phonograph.util.ui.paletteColor
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File

suspend fun selectImage(accessTool: OpenFileStorageAccessDelegate): Uri? {
    val cfg = OpenDocumentContract.Config(mimeTypes = arrayOf("image/*"))
    return suspendCancellableCoroutine {
        accessTool.launch(cfg) { uri: Uri? ->
            it.resume(uri) { _, _, _ -> }
        }
    }
}

private const val MAX_PIXELS_IMAGE = 3_686_400
private const val MAX_PIXELS_PALETTE_CALCULATION = 147_456

/**
 * Read embedded image from metadata of [songFilePath], with its palette color (or [fallbackColor] if failed).
 */
suspend fun readEmbeddedImage(songFilePath: String, @ColorInt fallbackColor: Int): Pair<Bitmap?, Color?> {
    val imageBytes = withContext(Dispatchers.IO) {
        JAudioTaggerExtractor.readImage(File(songFilePath))
    } ?: return Pair(null, null)
    return try {
        withContext(Dispatchers.Default) {
            val bitmap = decodeBitmapWithRestrictions(imageBytes, MAX_PIXELS_IMAGE)
            if (bitmap != null) {
                val downsampled = decodeBitmapWithRestrictions(imageBytes, MAX_PIXELS_PALETTE_CALCULATION)
                val color = Color(paletteColor(downsampled, fallbackColor, recycle = true))
                Pair(bitmap, color)
            } else {
                Pair(null, null)
            }
        }
    } catch (_: Exception) {
        Pair(null, null)
    }
}

/**
 * Read raw embedded image from metadata of [songFilePath]
 */
suspend fun readRawEmbeddedImage(songFilePath: String): Bitmap? {
    val imageBytes = withContext(Dispatchers.IO) {
        JAudioTaggerExtractor.readImage(File(songFilePath))
    } ?: return null
    return withContext(Dispatchers.Default) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, null)
    }
}

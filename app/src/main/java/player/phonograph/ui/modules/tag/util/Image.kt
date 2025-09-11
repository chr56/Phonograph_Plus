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
import kotlinx.coroutines.yield
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
private suspend fun decodeBitmap(bytes: ByteArray, maxPixels: Int): Bitmap? {
    if (bytes.isEmpty()) return null

    return if (shouldDownSampling(bytes, maxPixels)) {
        var sample = 2
        while (measureTotalPixel(bytes, sample) > maxPixels) {
            sample *= 2
            yield()
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } else {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

private const val BYTES_LIMITATION = 1_048_576
private fun shouldDownSampling(bytes: ByteArray, maxPixels: Int): Boolean {
    if (bytes.size > BYTES_LIMITATION) return true
    if (measureTotalPixel(bytes) > maxPixels) return true
    return false
}

private fun measureTotalPixel(bytes: ByteArray, sample: Int = 1): Long {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inSampleSize = sample
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    val srcWidth = options.outWidth
    val srcHeight = options.outHeight
    val size = srcHeight.toLong() * srcWidth
    return size
}

suspend fun readImage(path: String): Pair<Bitmap?, Color?> {
    val imageBytes = JAudioTaggerExtractor.readImage(File(path)) ?: return Pair(null, null)
    return try {
        val bitmap: Bitmap = decodeBitmap(imageBytes, MAX_PIXELS_IMAGE) ?: return Pair(null, null)
        val downsampled = decodeBitmap(imageBytes, MAX_PIXELS_PALETTE_CALCULATION)
        val paletteColor = if (downsampled != null) {
            paletteColor(downsampled, Color.Gray.toArgb()).let {
                downsampled.recycle() // the downsampled is used only for once
                Color(it)
            }
        } else {
            null
        }
        Pair(bitmap, paletteColor)
    } catch (_: Exception) {
        Pair(null, null)
    }
}

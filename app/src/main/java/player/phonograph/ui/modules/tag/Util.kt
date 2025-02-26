/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.warning
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

suspend fun selectImage(accessTool: OpenFileStorageAccessDelegate): Uri? {
    val cfg = OpenDocumentContract.Config(mimeTypes = arrayOf("image/*"))
    return suspendCancellableCoroutine {
        accessTool.launch(cfg) { uri: Uri? ->
            it.resume(uri) { _, _, _ -> }
        }
    }
}

suspend fun saveArtwork(context: Context, uri: Uri, bitmap: Bitmap) {
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("can't open uri $uri")
    withContext(Dispatchers.IO) {
        stream.buffered(4096).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }
}

fun fileName(song: Song) = song.data.substringAfterLast('/').substringBeforeLast('.')

fun createCacheFile(context: Context, name: String, uri: Uri): File {
    val cacheFile = File(context.externalCacheDir, "Cover_$name.png")
    if (cacheFile.exists()) cacheFile.delete() else cacheFile.createNewFile()
    context.contentResolver.openInputStream(uri).use { inputStream ->
        if (inputStream != null) {
            inputStream.buffered(8192).use { bufferedInputStream ->
                cacheFile.outputStream().buffered(8192).use { outputStream ->
                    // transfer stream
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (bufferedInputStream.read(buffer, 0, 8192).also { read = it } >= 0
                    ) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
        } else {
            warning("Cache", "Can not open selected file! (uri: $uri)")
        }
    }
    cacheFile.deleteOnExit()
    return cacheFile
}

suspend fun loadCover(context: Context, data: Any): Pair<Bitmap?, Color?> =
    suspendCancellableCoroutine { continuation ->
        loadImage(context) {
            data(data)
            parameters(PARAMETERS_RAW)
            target(
                PaletteTargetBuilder()
                    .defaultColor(themeFooterColor(context))
                    .onResourceReady { result: Drawable, paletteColor: Int ->
                        continuation.resume(result.toBitmap() to Color(paletteColor)) { _, _, _ -> }
                    }
                    .onFail {
                        continuation.resume(null to null) { _, _, _ -> }
                    }
                    .build()
            )
        }
    }

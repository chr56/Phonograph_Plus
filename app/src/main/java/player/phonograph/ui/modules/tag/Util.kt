/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.warning
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException

suspend fun selectImage(accessTool: OpenFileStorageAccessDelegate): Uri? {
    val cfg = OpenDocumentContract.Config(mimeTypes = arrayOf("image/*"))
    return suspendCancellableCoroutine {
        accessTool.launch(cfg) { uri: Uri? ->
            it.resume(uri) { _, _, _ -> }
        }
    }
}

fun saveArtwork(activity: Context, bitmap: Bitmap, fileName: String) {
    if (activity is ICreateFileStorageAccessible) {
        val accessTool = activity.createFileStorageAccessDelegate
        accessTool.launch("$fileName.jpg") { uri ->
            if (uri != null) {
                activity.lifecycleScopeOrNewOne().launch {
                    saveArtwork(activity, uri, bitmap)
                }
            } else {
                warning("SaveArtWorkImpl", "Failed to create File")
            }
        }
    } else {
        throw IllegalStateException("${activity.javaClass} can not create file!")
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

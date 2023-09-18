/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import player.phonograph.util.warning
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.io.OutputStream


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun loadCover(context: Context, data: Any): Pair<Bitmap?, Color?> {
    return suspendCancellableCoroutine { continuation ->
        loadImage(context) {
            data(data)
            parameters(PARAMETERS_RAW)
            target(
                PaletteTargetBuilder(context)
                    .onResourceReady { result: Drawable, paletteColor: Int ->
                        continuation.resume(result.toBitmap() to Color(paletteColor)) {}
                    }
                    .onFail {
                        continuation.resume(null to null) {}
                    }
                    .build()
            )
        }
    }
}

fun fileName(song: Song) =
    song.data.substringAfterLast('/').substringBeforeLast('.')

fun saveArtwork(
    coroutineScope: CoroutineScope,
    activity: Context,
    bitmap: Bitmap,
    fileName: String,
) {
    if (activity is ICreateFileStorageAccess) {
        val accessTool = activity.createFileStorageAccessTool
        accessTool.launch("$fileName.jpg") { uri ->
            if (uri != null) {
                saveArtworkImpl(coroutineScope, activity, uri, bitmap)
            } else {
                warning("SaveArtWorkImpl", "Failed to create File")
            }
        }
    } else {
        throw IllegalStateException("${activity.javaClass} can not create file!")
    }
}

private fun saveArtworkImpl(
    coroutineScope: CoroutineScope,
    context: Context,
    uri: Uri,
    bitmap: Bitmap,
) {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IOException("can't open uri $uri")
    writeArtwork(coroutineScope, stream, bitmap)
}

private fun writeArtwork(
    coroutineScope: CoroutineScope,
    outputStream: OutputStream,
    bitmap: Bitmap,
) {
    // write
    coroutineScope.launch(Dispatchers.IO) {
        outputStream.buffered(4096).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }
}
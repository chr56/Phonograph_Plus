/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import lib.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.BitmapPaletteWrapper
import player.phonograph.util.warning
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream


fun loadArtwork(
    context: Context,
    container: MutableStateFlow<BitmapPaletteWrapper?>,
    data: Any,
) {
    loadImage(context) {
        data(data)
        parameters(PARAMETERS_RAW)
        target(
            PaletteTargetBuilder(context)
                .onResourceReady { result: Drawable, paletteColor: Int ->
                    val success =
                        container.tryEmit(
                            BitmapPaletteWrapper(result.toBitmap(), paletteColor)
                        )
                    if (!success) warning("LoadArtwork", "Failed to load artwork!")
                }
                .build()
        )
    }
}

fun saveArtwork(
    coroutineScope: CoroutineScope,
    activity: Context,
    wrapper: BitmapPaletteWrapper,
    fileName: String,
) {
    if (activity is ICreateFileStorageAccess) {
        val accessTool = activity.createFileStorageAccessTool
        accessTool.launch("$fileName.jpg") { uri ->
            if (uri != null) {
                saveArtworkImpl(coroutineScope, activity, uri, wrapper)
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
    wrapper: BitmapPaletteWrapper,
) {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IOException("can't open uri $uri")
    writeArtwork(coroutineScope, stream, wrapper)
}

private fun writeArtwork(
    coroutineScope: CoroutineScope,
    outputStream: OutputStream,
    wrapper: BitmapPaletteWrapper,
) {
    // write
    coroutineScope.launch(Dispatchers.IO) {
        outputStream.buffered(4096).use { outputStream ->
            wrapper.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }
}
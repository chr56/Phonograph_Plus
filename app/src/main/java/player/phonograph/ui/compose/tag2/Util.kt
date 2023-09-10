/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import lib.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.model.Song
import player.phonograph.util.warning
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream


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
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.util

import player.phonograph.foundation.error.warning
import player.phonograph.model.Song
import android.content.Context
import android.net.Uri
import java.io.File

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
            warning(context, "Cache", "Can not open selected file! (uri: $uri)")
        }
    }
    cacheFile.deleteOnExit()
    return cacheFile
}

fun fileName(song: Song) = song.data.substringAfterLast('/').substringBeforeLast('.')
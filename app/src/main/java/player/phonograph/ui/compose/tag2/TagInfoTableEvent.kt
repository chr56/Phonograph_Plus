/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import org.jaudiotagger.tag.FieldKey
import player.phonograph.util.warning
import android.content.Context
import android.net.Uri
import java.io.File

sealed interface TagInfoTableEvent {
    data class UpdateTag(val fieldKey: FieldKey, val newValue: String) : TagInfoTableEvent
    data class AddNewTag(val fieldKey: FieldKey) : TagInfoTableEvent
    data class RemoveTag(val fieldKey: FieldKey) : TagInfoTableEvent

    object RemoveArtwork : TagInfoTableEvent
    data class UpdateArtwork(val file: File) : TagInfoTableEvent {
        companion object {
            fun from(context: Context, uri: Uri): UpdateArtwork {
                val cacheFile = File(context.externalCacheDir, "Cover_${uri.path}.png")
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
                        warning("UpdateArtwork", "Can not open selected file! (uri: $uri)")
                    }
                }
                return UpdateArtwork(cacheFile)
            }
        }
    }
}
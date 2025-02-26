/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.modules.tag.util.createCacheFile
import android.content.Context
import android.net.Uri
import java.io.File

sealed interface MetadataUIEvent {
    data object Save : MetadataUIEvent
    sealed interface Edit : MetadataUIEvent {

        data class AddNewTag(val fieldKey: ConventionalMusicMetadataKey) : Edit
        data class RemoveTag(val fieldKey: ConventionalMusicMetadataKey) : Edit
        data class UpdateTag(val fieldKey: ConventionalMusicMetadataKey, val newValue: String) : Edit

        data object RemoveArtwork : Edit
        data class UpdateArtwork(val file: File) : Edit {
            companion object {
                fun from(context: Context, uri: Uri, name: String): UpdateArtwork {
                    val cacheFile = createCacheFile(context, name, uri)
                    return UpdateArtwork(cacheFile)
                }
            }
        }
    }
    data object ExtractArtwork: MetadataUIEvent
}
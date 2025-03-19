/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata.read

import player.phonograph.coil.loadImage
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toFieldKey
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.InteractiveAction
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.util.theme.themeFooterColor
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.graphics.Bitmap
import java.io.File

data class SongDetail(
    val song: Song,
    val metadata: AudioMetadata,
    val errors: List<Throwable>,
    val image: Bitmap?,
    val color: Color?,
) {

    /**
     * create new state by [event]
     */
    suspend fun edit(context: Context, event: InteractiveAction.Edit): SongDetail = when (event) {
        is InteractiveAction.Edit.AddNewTag     -> {
            val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                musicMetadata.genericTagFields + (event.fieldKey to Metadata.TextualField(""))
            }
            copy(metadata = audioMetadata)
        }

        is InteractiveAction.Edit.UpdateTag     -> {
            val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                musicMetadata.genericTagFields.toMutableMap().also { genericTagFields ->
                    genericTagFields[event.fieldKey] = Metadata.TextualField(event.newValue)
                }
            }
            copy(metadata = audioMetadata)
        }

        is InteractiveAction.Edit.RemoveTag     -> {
            val audioMetadata = modifyMusicMetadataField { musicMetadata ->
                musicMetadata.genericTagFields.toMutableMap().also { genericTagFields ->
                    genericTagFields.remove(event.fieldKey)
                }
            }
            copy(metadata = audioMetadata)
        }

        is InteractiveAction.Edit.UpdateArtwork -> {
            val (bitmap, color) = loadImage(
                context, File(event.path),
                defaultColor = themeFooterColor(context), raw = true,
            )
            copy(image = bitmap, color = color)
        }

        is InteractiveAction.Edit.RemoveArtwork -> {
            copy(image = null)
        }
    }


    private fun modifyMusicMetadataField(
        block: (MusicMetadata) -> Map<ConventionalMusicMetadataKey, Metadata.Field>,
    ): AudioMetadata = modifyMusicMetadata { musicMetadata ->
        val fields = block(musicMetadata)
        if (musicMetadata is JAudioTaggerMetadata) {
            musicMetadata.copy(_genericTagFields = fields.mapKeys { it.key.toFieldKey() })
        } else {
            musicMetadata
        }
    }

    private fun modifyMusicMetadata(block: (MusicMetadata) -> MusicMetadata): AudioMetadata =
        metadata.copy(musicMetadata = block(metadata.musicMetadata))
}
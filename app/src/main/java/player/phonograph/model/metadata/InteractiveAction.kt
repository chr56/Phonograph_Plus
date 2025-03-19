/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.metadata

sealed interface InteractiveAction {
    data object Save : InteractiveAction
    sealed interface Edit : InteractiveAction {

        data class AddNewTag(val fieldKey: ConventionalMusicMetadataKey) : Edit
        data class RemoveTag(val fieldKey: ConventionalMusicMetadataKey) : Edit
        data class UpdateTag(val fieldKey: ConventionalMusicMetadataKey, val newValue: String) : Edit

        data object RemoveArtwork : Edit
        data class UpdateArtwork(val path: String) : Edit
    }

    data object ExtractArtwork : InteractiveAction
}
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.metadata

import player.phonograph.model.TagFormat


data class AudioMetadata(
    val fileProperties: FileProperties,
    val audioProperties: AudioProperties,
    val audioMetadataFormat: TagFormat,
    val musicMetadata: MusicMetadata,
) {
    companion object {
        val EMPTY: AudioMetadata
            get() = AudioMetadata(
                FileProperties("-", "", -1),
                AudioProperties("", 0, "", ""),
                TagFormat.Unknown,
                EmptyMusicMetadata,
            )
    }
}


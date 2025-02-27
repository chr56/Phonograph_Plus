/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.metadata

data class AudioMetadata(
    val fileProperties: FileProperties,
    val audioProperties: AudioProperties,
    val audioMetadataFormat: MusicTagFormat,
    val musicMetadata: MusicMetadata,
) {
    companion object {
        val EMPTY: AudioMetadata
            get() = AudioMetadata(
                FileProperties("-", "", -1),
                AudioProperties("", 0, "", ""),
                MusicTagFormat.Unknown,
                EmptyMusicMetadata,
            )
    }
}


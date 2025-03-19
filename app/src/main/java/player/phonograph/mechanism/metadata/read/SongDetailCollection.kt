/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata.read

import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata

data class SongDetailCollection(
    val raw: Map<Song, AudioMetadata>,
    val errors: Map<Song, List<Throwable>>,
    val fields: Map<ConventionalMusicMetadataKey, List<Metadata.Field>>,
) {
    val songs get() = raw.keys
    val metadata get() = raw.values
    val keys get() = fields.keys

    companion object {

        fun from(all: Map<Song, AudioMetadata>, errors: Map<Song, List<Throwable>>): SongDetailCollection {
            val reduced = reducedTagFields(all.values)
            return SongDetailCollection(all, errors, reduced)
        }

        private fun reducedTagFields(all: Collection<AudioMetadata>): Map<ConventionalMusicMetadataKey, List<Metadata.Field>> =
            all.fold(mutableMapOf()) { acc, model ->
                val musicMetadata = model.musicMetadata
                if (musicMetadata is JAudioTaggerMetadata)
                    for ((key, value) in musicMetadata.textTagFields) {
                        val oldValue = acc[key]
                        val newValue = if (oldValue != null) {
                            oldValue + listOf(value)
                        } else {
                            listOf(value)
                        }
                        acc[key] = newValue
                    }
                acc
            }
    }
}
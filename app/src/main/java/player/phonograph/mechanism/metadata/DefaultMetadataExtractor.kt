/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.AudioProperties
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.ALBUM
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.ALBUM_ARTIST
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.ARTIST
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.COMPOSER
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.TITLE
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.TRACK
import player.phonograph.model.metadata.ConventionalMusicMetadataKey.YEAR
import player.phonograph.model.metadata.FileProperties
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.Metadata.PlainStringField
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.model.metadata.MusicTagFormat
import android.content.Context
import kotlin.collections.component1
import kotlin.collections.component2
import java.io.File

/**
 * **default MetadataExtractor**
 *
 * do not extract or read anything, just using existed information
 */
object DefaultMetadataExtractor : MetadataExtractor {
    override fun extractSongMetadata(context: Context, song: Song): AudioMetadata {
        val songFile: File = File(song.data)
        return AudioMetadata(
            fileProperties = FileProperties(songFile.name, songFile.absolutePath, songFile.length()),
            audioProperties = AudioProperties(songFile.extension, song.duration, "-", "-"),
            audioMetadataFormat = MusicTagFormat.Unknown,
            musicMetadata = PlainMusicMetadata(
                mapOf(
                    TITLE to PlainStringField(song.title),
                    ARTIST to PlainStringField(song.artistName.orEmpty()),
                    ALBUM to PlainStringField(song.albumName.orEmpty()),
                    ALBUM_ARTIST to PlainStringField(song.albumArtistName.orEmpty()),
                    COMPOSER to PlainStringField(song.composer.orEmpty()),
                    YEAR to PlainStringField(song.year.toString()),
                    TRACK to PlainStringField(song.trackNumber.toString()),
                )
            ),
        )
    }

    private class PlainMusicMetadata(
        override val genericTagFields: Map<ConventionalMusicMetadataKey, Metadata.Field>,
    ) : MusicMetadata {

        override fun get(key: Metadata.Key): Metadata.Field? = genericTagFields[key]
        override fun contains(key: Metadata.Key): Boolean = genericTagFields.containsKey(key)
        override val fields: List<Metadata.Entry>
            get() = genericTagFields.map { (key, field) -> Metadata.PlainEntry(key, field) }
        override val textTagFields: Map<ConventionalMusicMetadataKey, Metadata.Field> get() = genericTagFields
        override val allTagFields: Map<String, Metadata.Field> get() = genericTagFields.mapKeys { it.key.name }
    }
}
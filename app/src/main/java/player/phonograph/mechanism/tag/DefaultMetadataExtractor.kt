/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.FilePropertyField
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagData
import player.phonograph.model.TagField
import player.phonograph.model.TagFormat
import android.content.Context
import java.io.File

/**
 * **default MetadataExtractor**
 *
 * do not extract or read anything, just using existed information
 */
object DefaultMetadataExtractor : MetadataExtractor {
    override fun extractSongMetadata(
        context: Context,
        song: Song,
    ): SongInfoModel {
        val songFile: File = File(song.data)
        return SongInfoModel(
            fileName = StringFilePropertyField(songFile.name),
            filePath = StringFilePropertyField(songFile.absolutePath),
            fileSize = LongFilePropertyField(songFile.length()),
            audioPropertyFields = mapOf(
                FilePropertyField.Key.FILE_FORMAT to StringFilePropertyField(songFile.extension),
                FilePropertyField.Key.TRACK_LENGTH to LongFilePropertyField(song.duration)
            ),
            tagFields = mapOf(
                FieldKey.TITLE to TagField(FieldKey.TITLE, TagData.TextData(song.title)),
                FieldKey.ARTIST to TagField(FieldKey.ARTIST, TagData.TextData(song.artistName.orEmpty())),
                FieldKey.ALBUM to TagField(FieldKey.ALBUM, TagData.TextData(song.albumName.orEmpty())),
                FieldKey.ALBUM_ARTIST to TagField(
                    FieldKey.ALBUM_ARTIST,
                    TagData.TextData(song.albumArtistName.orEmpty())
                ),
                FieldKey.COMPOSER to TagField(FieldKey.COMPOSER, TagData.TextData(song.composer.orEmpty())),
                FieldKey.YEAR to TagField(FieldKey.YEAR, TagData.TextData(song.year.toString())),
                FieldKey.TRACK to TagField(FieldKey.TRACK, TagData.TextData(song.trackNumber.toString())),
            ),
            tagFormat = TagFormat.Unknown,
            allTags = emptyMap(),
        )
    }
}
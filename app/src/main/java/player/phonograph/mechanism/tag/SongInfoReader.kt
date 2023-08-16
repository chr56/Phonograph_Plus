/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import player.phonograph.model.FilePropertyField
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagField
import player.phonograph.util.reportError
import java.io.File

fun loadSongInfo(song: Song): SongInfoModel = loadSongInfo(File(song.data))

fun loadSongInfo(songFile: File): SongInfoModel {
    require(songFile.exists()) { "${songFile.path} doesn't exist! please check file or permission of device storage." }
    val fileName = songFile.name
    val filePath = songFile.absolutePath
    val fileSize = songFile.length()
    try {
        val audioFile: AudioFile = AudioFileIO.read(songFile)

        val audioPropertyFields = readAudioPropertyFields(audioFile.audioHeader)
        val tagFields = readTagFields(audioFile)
        val tagFormat = TagFormat.of(audioFile.tag)
        val allTags = readAllTags(audioFile)

        return SongInfoModel(
            fileName = StringFilePropertyField(fileName),
            filePath = StringFilePropertyField(filePath),
            fileSize = LongFilePropertyField(fileSize),
            audioPropertyFields = audioPropertyFields,
            tagFields = tagFields,
            tagFormat = tagFormat,
            allTags,
        )
    } catch (e: Exception) {
        reportError(e, "TagRead", "error while reading the song file")
        return SongInfoModel(
            fileName = StringFilePropertyField(fileName),
            filePath = StringFilePropertyField(filePath),
            fileSize = LongFilePropertyField(fileSize),
            audioPropertyFields = emptyMap(),
            tagFields = mapOf(FieldKey.TITLE to TagField(FieldKey.TITLE, "error while reading the song file")),
            tagFormat = TagFormat.Unknown,
            null,
        )
    }
}


private fun readAudioPropertyFields(audioHeader: AudioHeader): Map<FilePropertyField.Key, FilePropertyField<out Any>> {
    val fileFormat = audioHeader.format
    val trackLength = (audioHeader.trackLength * 1000).toLong()
    val bitRate = "${audioHeader.bitRate} kb/s"
    val samplingRate = "${audioHeader.sampleRate} Hz"
    return mapOf(
        FilePropertyField.Key.TRACK_LENGTH to StringFilePropertyField(fileFormat),
        FilePropertyField.Key.FILE_FORMAT to StringFilePropertyField(bitRate),
        FilePropertyField.Key.BIT_RATE to StringFilePropertyField(samplingRate),
        FilePropertyField.Key.SAMPLING_RATE to LongFilePropertyField(trackLength),
    )
}


private fun readTagFields(audioFile: AudioFile): Map<FieldKey, TagField> =
    readTagFieldsImpl(
        audioFile, arrayOf(
            FieldKey.TITLE,
            FieldKey.ARTIST,
            FieldKey.ALBUM,
            FieldKey.ALBUM_ARTIST,
            FieldKey.COMPOSER,
            FieldKey.LYRICIST,
            FieldKey.YEAR,
            FieldKey.GENRE,
            FieldKey.DISC_NO,
            FieldKey.TRACK,
            FieldKey.COMMENT,
        )
    )

private fun readTagFieldsImpl(audioFile: AudioFile, keys: Array<FieldKey>): Map<FieldKey, TagField> =
    keys.associateWith { key ->
        TagField(key, audioFile.tag.getFirst(key))
    }

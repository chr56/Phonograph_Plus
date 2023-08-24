/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import player.phonograph.model.FilePropertyField
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagData.EmptyData
import player.phonograph.model.TagData.TextData
import player.phonograph.model.TagField
import player.phonograph.model.allFieldKey
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
            tagFields = mapOf(FieldKey.TITLE to TagField(FieldKey.TITLE, TextData("ERR"))),
            tagFormat = TagFormat.Unknown,
            allTags = emptyMap(),
        )
    }
}


private fun readAudioPropertyFields(audioHeader: AudioHeader): Map<FilePropertyField.Key, FilePropertyField<out Any>> {
    val fileFormat = audioHeader.format
    val trackLength = (audioHeader.trackLength * 1000).toLong()
    val bitRate = "${audioHeader.bitRate} kb/s"
    val samplingRate = "${audioHeader.sampleRate} Hz"
    return mapOf(
        FilePropertyField.Key.FILE_FORMAT to StringFilePropertyField(fileFormat),
        FilePropertyField.Key.BIT_RATE to StringFilePropertyField(bitRate),
        FilePropertyField.Key.SAMPLING_RATE to StringFilePropertyField(samplingRate),
        FilePropertyField.Key.TRACK_LENGTH to LongFilePropertyField(trackLength),
    )
}


private fun readTagFields(audioFile: AudioFile): Map<FieldKey, TagField> =
    readTagFieldsImpl(audioFile, allFieldKey)

private fun readTagFieldsImpl(audioFile: AudioFile, keys: Set<FieldKey>): Map<FieldKey, TagField> =
    keys.associateWith { key ->
        val value = try {
            val text =
                audioFile.tag.getFirst(key)
            if (text.isNotEmpty()) TextData(text) else EmptyData
        } catch (e: KeyNotFoundException) {
            EmptyData
        }
        TagField(key, value)
    }

/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import player.phonograph.R
import player.phonograph.model.FilePropertyField
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagData.BinaryData
import player.phonograph.model.TagData.EmptyData
import player.phonograph.model.TagData.TextData
import player.phonograph.model.TagField
import player.phonograph.model.TagFormat
import player.phonograph.model.allFieldKey
import player.phonograph.util.reportError
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File

fun loadSongInfo(context: Context, song: Song): SongInfoModel {
    val songFile = File(song.data)
    if (!songFile.exists()) {
        Handler(Looper.getMainLooper()).post {
            context.getString(R.string.file_not_found, songFile.path)
        }
        return readSongInfoFallback(song, songFile)
    }
    val fileName = songFile.name
    val filePath = songFile.absolutePath
    val fileSize = songFile.length()
    try {
        val audioFile: AudioFile = AudioFileIO.read(songFile)

        val audioPropertyFields = readAudioPropertyFields(audioFile.audioHeader)
        val tagFields = readTagFields(audioFile)
        val tagFormat = TagFormat.of(audioFile)
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
        val suffix = songFile.extension
        if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(suffix) == e.message) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, context.getString(R.string.unsupported_format, suffix), Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            reportError(e, TAG, String.format(ERR_MSG_FAILED_TO_READ, filePath, suffix))
        }
        return readSongInfoFallback(song, songFile)
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

private fun readTagFieldsImpl(audioFile: AudioFile, keys: Set<FieldKey>): Map<FieldKey, TagField> = run {
    val fields = audioFile.tag ?: return@run emptyMap()
    keys.associateWith { key ->
        val value = try {
            val field = fields.getFirstField(key)
            if (field != null) {
                if (field.isBinary) {
                    BinaryData
                } else if (field.isEmpty) {
                    EmptyData
                } else {
                    val full = fields.getFirst(key)
                    val text = if (full.length > 512) "${full.take(512)}\n..." else full
                    TextData(text)
                }
            } else {
                EmptyData
            }
        } catch (e: KeyNotFoundException) {
            EmptyData
        }
        TagField(key, value)
    }
}


private fun readSongInfoFallback(song: Song, songFile: File): SongInfoModel =
    SongInfoModel(
        fileName = StringFilePropertyField(songFile.name),
        filePath = StringFilePropertyField(songFile.absolutePath),
        fileSize = LongFilePropertyField(songFile.length()),
        audioPropertyFields = mapOf(
            FilePropertyField.Key.FILE_FORMAT to StringFilePropertyField(songFile.extension),
            FilePropertyField.Key.TRACK_LENGTH to LongFilePropertyField(song.duration)
        ),
        tagFields = mapOf(
            FieldKey.TITLE to TagField(FieldKey.TITLE, TextData(song.title)),
            FieldKey.ARTIST to TagField(FieldKey.ARTIST, TextData(song.artistName.orEmpty())),
            FieldKey.ALBUM to TagField(FieldKey.ALBUM, TextData(song.albumName.orEmpty())),
            FieldKey.ALBUM_ARTIST to TagField(FieldKey.ALBUM_ARTIST, TextData(song.albumArtistName.orEmpty())),
            FieldKey.COMPOSER to TagField(FieldKey.COMPOSER, TextData(song.composer.orEmpty())),
            FieldKey.YEAR to TagField(FieldKey.YEAR, TextData(song.year.toString())),
            FieldKey.TRACK to TagField(FieldKey.TRACK, TextData(song.trackNumber.toString())),
        ),
        tagFormat = TagFormat.Unknown,
        allTags = emptyMap(),
    )

private const val TAG = "SongInfoReader"

private const val ERR_MSG_FAILED_TO_READ =
    "Failed to read metadata of song (%s). This might cause by: " +
            "1) storage permission is not fully granted. " +
            "2) the format (%s) is not supported."
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.audio.real.RealTag
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.aiff.AiffTag
import org.jaudiotagger.tag.asf.AsfTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v11Tag
import org.jaudiotagger.tag.id3.ID3v1Tag
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.wav.WavInfoTag
import org.jaudiotagger.tag.wav.WavTag
import player.phonograph.R
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata.Field
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.AudioProperties
import player.phonograph.model.metadata.EmptyMusicMetadata
import player.phonograph.model.metadata.FileProperties
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.model.metadata.MusicTagFormat
import player.phonograph.util.reportError
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * **JAudioTagger Metadata Extractor**
 *
 * extract metadata from JAudioTagger
 */
object JAudioTaggerExtractor : MetadataExtractor {

    override fun extractSongMetadata(context: Context, song: Song): AudioMetadata? {
        val songFile = File(song.data)
        if (!songFile.exists()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, context.getString(R.string.file_not_found, songFile.path), Toast.LENGTH_SHORT)
                    .show()
            }
            return null
        }

        val fileName = songFile.name
        val filePath = songFile.absolutePath
        val fileSize = songFile.length()

        try {
            val audioFile: AudioFile = readAudioFile(songFile) ?: return null

            val audioPropertyFields = readAudioProperties(audioFile.audioHeader)
            val tagFormat = readTagFormat(audioFile)
            val musicMetadata = readMusicMetadata(audioFile)
            return AudioMetadata(
                fileProperties = FileProperties(fileName, filePath, fileSize),
                audioProperties = audioPropertyFields,
                audioMetadataFormat = tagFormat,
                musicMetadata = musicMetadata,
            )
        } catch (e: Exception) {
            reportError(e, TAG, analyzeException(e, songFile))
            return null
        }
    }

    private fun readAudioProperties(audioHeader: AudioHeader): AudioProperties {
        val audioFormat = audioHeader.format
        val trackLength = (audioHeader.trackLength * 1000).toLong()
        val bitRate = "${audioHeader.bitRate} kb/s"
        val samplingRate = "${audioHeader.sampleRate} Hz"
        return AudioProperties(
            audioFormat = audioFormat,
            trackLength = trackLength,
            bitRate = bitRate,
            samplingRate = samplingRate,
        )
    }

    private fun readMusicMetadata(audioFile: AudioFile): MusicMetadata {
        val fields = audioFile.tag ?: return EmptyMusicMetadata
        // Generic
        val genericTagFields: Map<FieldKey, Metadata.Field> =
            try {
                GenericFieldsWithPriority.mapNotNull { key ->
                    val tagField: TagField? = fields.getFirstField(key)
                    val value = readGenericTagFields(tagField)
                    if (value != null) key to value else null
                }.toMap()
            } catch (e: Exception) {
                reportError(e, TAG, "Failed to read all tags for ${audioFile.file.absolutePath}")
                emptyMap()
            }
        // all
        val allTagFields: Map<String, Field> =
            try {
                when (val tag = audioFile.tag) {
                    is AbstractID3v2Tag -> ID3v2Readers.ID3v2Reader.read(tag)
                    is AiffTag          -> ID3v2Readers.AiffTagReader.read(tag)
                    is WavTag           -> ID3v2Readers.WavTagReader.read(tag)
                    is ID3v11Tag        -> ID3v1TagReaders.ID3v11TagReader.read(tag)
                    is ID3v1Tag         -> ID3v1TagReaders.ID3v1TagReader.read(tag)
                    is FlacTag          -> FlacTagReader.read(tag)
                    is Mp4Tag           -> Mp4TagReader.read(tag)
                    is VorbisCommentTag -> VorbisCommentTagReader.read(tag)
                    is AbstractTag      -> SimpleKeyValueReader.read(tag)
                    else                -> emptyMap()
                }
            } catch (e: Exception) {
                reportError(e, TAG, "Failed to read all tags for ${audioFile.file.absolutePath}")
                emptyMap()
            }

        return JAudioTaggerMetadata(genericTagFields, allTagFields)
    }

    private fun readGenericTagFields(field: TagField?): Metadata.Field? =
        if (field != null) {
            when {
                field.isEmpty         -> null
                field.isBinary        -> RawBinaryField(field.rawContent)
                field is TagTextField -> Metadata.PlainStringField(field.content)
                else                  -> RawBinaryField(field.rawContent)
            }
        } else {
            null
        }

    private val GenericFieldsWithPriority = setOf(
        FieldKey.TITLE,
        FieldKey.ARTIST,
        FieldKey.ALBUM,
        FieldKey.ALBUM_ARTIST,
        FieldKey.YEAR,
        FieldKey.TRACK,
        FieldKey.TRACK_TOTAL,
        FieldKey.DISC_NO,
        FieldKey.DISC_TOTAL,
        FieldKey.GENRE,
        FieldKey.COMPOSER,
        FieldKey.LYRICIST,
        FieldKey.RATING,
        FieldKey.COMMENT,
        FieldKey.LYRICS,
    ) + FieldKey.entries

    private fun readTagFormat(audioFile: AudioFile): MusicTagFormat = when (audioFile.tag) {
        is Mp4Tag           -> MusicTagFormat.Mp4
        is ID3v24Tag        -> MusicTagFormat.ID3v24
        is ID3v23Tag        -> MusicTagFormat.ID3v23
        is ID3v22Tag        -> MusicTagFormat.ID3v22
        is ID3v11Tag        -> MusicTagFormat.ID3v11
        is ID3v1Tag         -> MusicTagFormat.ID3v1
        is FlacTag          -> MusicTagFormat.Flac
        is AiffTag          -> MusicTagFormat.Aiff
        is AsfTag           -> MusicTagFormat.Asf
        is RealTag          -> MusicTagFormat.Real
        is WavTag           -> MusicTagFormat.Wav
        is WavInfoTag       -> MusicTagFormat.WavInfo
        is VorbisCommentTag -> MusicTagFormat.VorbisComment
        else                -> MusicTagFormat.Unknown
    }

    /**
     * read embed lyrics via JAudioTagger
     */
    fun readLyrics(file: File): String? {
        try {
            val metadata = readAudioFile(file)?.tag ?: return null
            val value = metadata.getFirst(FieldKey.LYRICS)
            return if (!value.isNullOrBlank()) value else null
        } catch (e: CannotReadException) {
            return if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(file.extension) == e.message) {
                // ignore
                null
            } else {
                val message = "Error: Failed to read ${file.path}: ${e.message}\n${Log.getStackTraceString(e)}"
                Log.i(TAG, message)
                message
            }
        }
    }

    /**
     * read embed images via JAudioTagger
     */
    fun readImage(file: File): ByteArray? {
        try {
            val metadata = readAudioFile(file)?.tag ?: return null
            return metadata.firstArtwork?.binaryData
        } catch (e: Exception) {
            return null
        }
    }

    private fun readAudioFile(file: File): AudioFile? {
        return try {
            if (file.extension.isNotEmpty()) {
                AudioFileIO.read(file)
            } else {
                AudioFileIO.readMagic(file)
            }
        } catch (e: CannotReadException) {
            reportError(e, TAG, analyzeException(e, file))
            null
        }
    }

    /**
     * @return error message
     */
    private fun analyzeException(e: Exception, file: File): String =
        if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(file.extension) == e.message) {
            String.format(ERR_MSG_UNSUPPORTED_FILE_FORMAT, file.path, file.extension)
        } else {
            String.format(ERR_MSG_FAILED_TO_READ, file.path)
        }

    private const val TAG = "JAudioTaggerExtractor"
    private const val ERR_MSG_UNSUPPORTED_FILE_FORMAT =
        "Failed to read metadata of song (%s): format (%s) is not supported."
    private const val ERR_MSG_FAILED_TO_READ =
        "Failed to read metadata of song (%s): storage permission may not be fully granted."
}
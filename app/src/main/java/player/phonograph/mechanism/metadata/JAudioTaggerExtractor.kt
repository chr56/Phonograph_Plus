/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.audio.real.RealTag
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
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
import android.widget.Toast
import kotlin.collections.associateWith
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
            val audioFile: AudioFile = AudioFileIO.read(songFile)

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
            val suffix = songFile.extension
            if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(suffix) == e.message) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, context.getString(R.string.unsupported_format, suffix), Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                reportError(e, TAG, String.format(ERR_MSG_FAILED_TO_READ, filePath, suffix))
            }
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
                FieldKey.entries.associateWith { key ->
                    try {
                        val field: TagField? = fields.getFirstField(key)
                        readGenericTagFields(field)
                    } catch (_: KeyNotFoundException) {
                        Metadata.EmptyField
                    }
                }
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

    private fun readGenericTagFields(field: TagField?): Metadata.Field =
        if (field != null) {
            when {
                field.isEmpty         -> Metadata.EmptyField
                field is TagTextField -> Metadata.PlainStringField(field.content)
                else                  -> RawBinaryField(field.rawContent)
            }
        } else {
            Metadata.EmptyField
        }

    private fun readTagFormat(audioFile: AudioFile): MusicTagFormat {
        return when (audioFile.tag) {
            (ID3v1Tag::class.java)         -> MusicTagFormat.ID3v1
            (ID3v11Tag::class.java)        -> MusicTagFormat.ID3v11
            (ID3v24Tag::class.java)        -> MusicTagFormat.ID3v24
            (ID3v22Tag::class.java)        -> MusicTagFormat.ID3v22
            (ID3v23Tag::class.java)        -> MusicTagFormat.ID3v23
            (Mp4Tag::class.java)           -> MusicTagFormat.Mp4
            (VorbisCommentTag::class.java) -> MusicTagFormat.VorbisComment
            (FlacTag::class.java)          -> MusicTagFormat.Flac
            (AiffTag::class.java)          -> MusicTagFormat.Aiff
            (AsfTag::class.java)           -> MusicTagFormat.Asf
            (RealTag::class.java)          -> MusicTagFormat.Real
            (WavTag::class.java)           -> MusicTagFormat.Wav
            (WavInfoTag::class.java)       -> MusicTagFormat.WavInfo
            else                           -> MusicTagFormat.Unknown
        }
    }


    private const val TAG = "JAudioTaggerExtractor"
    private const val ERR_MSG_FAILED_TO_READ =
        "Failed to read metadata of song (%s). This might cause by: " +
                "1) storage permission is not fully granted. " +
                "2) the format (%s) is not supported."
}
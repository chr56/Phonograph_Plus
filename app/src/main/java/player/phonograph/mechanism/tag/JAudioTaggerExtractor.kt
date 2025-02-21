/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.aiff.AiffTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v11Tag
import org.jaudiotagger.tag.id3.ID3v1Tag
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.wav.WavTag
import player.phonograph.R
import player.phonograph.model.FilePropertyField
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.RawTag
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

/**
 * **JAudioTagger Metadata Extractor**
 *
 * extract metadata from JAudioTagger
 */
object JAudioTaggerExtractor : MetadataExtractor {

    override fun extractSongMetadata(context: Context, song: Song): SongInfoModel? {
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

            val audioPropertyFields = readAudioPropertyFields(audioFile.audioHeader)
            val tagFields = readTagFields(audioFile)
            val tagFormat = TagFormat.of(audioFile)
            val allTags = readAllTagFields(audioFile)

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
            return null
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

    private fun readTagFields(audioFile: AudioFile): Map<FieldKey, TagField> = readTagFieldsImpl(audioFile, allFieldKey)
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

    fun readAllTagFields(audioFile: AudioFile): Map<String, RawTag> {
        val items: Map<String, RawTag> = try {
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
            reportError(e, "TagReader", "Failed to read all tags for ${audioFile.file.absolutePath}")
            emptyMap()
        }
        return items
    }

    private const val TAG = "JAudioTaggerExtractor"
    private const val ERR_MSG_FAILED_TO_READ =
        "Failed to read metadata of song (%s). This might cause by: " +
                "1) storage permission is not fully granted. " +
                "2) the format (%s) is not supported."
}
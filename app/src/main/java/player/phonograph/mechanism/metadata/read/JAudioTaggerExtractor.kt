/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata.read

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.audio.generic.Utils
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
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata.Field
import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.AudioProperties
import player.phonograph.model.metadata.EmptyMusicMetadata
import player.phonograph.model.metadata.ExceptionCollector
import player.phonograph.model.metadata.FileProperties
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MetadataExtractor
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.model.metadata.MusicTagFormat
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * **JAudioTagger Metadata Extractor**
 *
 * extract metadata from JAudioTagger
 */
object JAudioTaggerExtractor : MetadataExtractor {

    override fun extractMetadata(path: String, collector: ExceptionCollector?): AudioMetadata? {
        val songFile = File(path)
        if (!songFile.exists()) {
            collector?.collect(FileNotFoundException(path))
            return null
        }

        val (dateAdded, dateModified) =
            if (SDK_INT >= VERSION_CODES.O) {
                val attributes: BasicFileAttributes =
                    Files.readAttributes(songFile.toPath(), BasicFileAttributes::class.java)
                attributes.creationTime().toMillis() to attributes.lastModifiedTime().toMillis()
            } else {
                songFile.lastModified() to songFile.lastModified()
            }

        return extractAll(
            file = songFile,
            dateAdded = dateAdded,
            dateModified = dateModified,
            collector = collector
        )
    }

    override fun extractMetadata(song: Song, collector: ExceptionCollector?): AudioMetadata? {
        val songFile = File(song.data)
        if (!songFile.exists()) {
            collector?.collect(FileNotFoundException(song.data))
            return null
        }

        val dateModified = song.dateModified
        val dateAdded = song.dateAdded

        return extractAll(
            file = songFile,
            dateAdded = dateAdded,
            dateModified = dateModified,
            collector = collector
        )
    }

    private fun extractAll(
        file: File,
        dateAdded: Long,
        dateModified: Long,
        collector: ExceptionCollector?,
    ): AudioMetadata? {
        val fileName = file.name
        val filePath = file.absolutePath
        val fileSize = file.length()

        try {
            val audioFile: AudioFile = readAudioFile(file).getOrThrow()
            val audioPropertyFields = readAudioProperties(audioFile.audioHeader)
            val tagFormat = readTagFormat(audioFile)
            val musicMetadata = readMusicMetadata(audioFile, collector)

            val metadata = AudioMetadata(
                fileProperties = FileProperties(fileName, filePath, fileSize, dateAdded, dateModified),
                audioProperties = audioPropertyFields,
                audioMetadataFormat = tagFormat,
                musicMetadata = musicMetadata,
            )
            return metadata
        } catch (e: Exception) {
            collector?.collect(MetadataExtractingException(analyzeException(e, file), e))
            return null
        }
    }

    private fun readAudioProperties(audioHeader: AudioHeader): AudioProperties {
        val audioFormat = audioHeader.format
        val trackLength = (audioHeader.trackLength * 1000).toLong()
        val bitRate = audioHeader.bitRate.toLongOrNull() ?: -1
        val samplingRate = audioHeader.sampleRate.toLongOrNull() ?: -1
        return AudioProperties(
            audioFormat = audioFormat,
            trackLength = trackLength,
            bitRate = bitRate,
            samplingRate = samplingRate,
        )
    }

    private fun readMusicMetadata(audioFile: AudioFile, collector: ExceptionCollector?): MusicMetadata {
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
                collector?.collect(e)
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
                collector?.collect(e)
                emptyMap()
            }

        return JAudioTaggerMetadata(genericTagFields, allTagFields)
    }

    private fun readGenericTagFields(field: TagField?): Metadata.Field? =
        if (field != null) {
            when {
                field.isEmpty         -> null
                field.isBinary        -> Metadata.BinaryField(field.rawContent)
                field is TagTextField -> Metadata.TextualField(field.content)
                else                  -> Metadata.BinaryField(field.rawContent)
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

    override fun extractLyrics(path: String, collector: ExceptionCollector?): String? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val metadata = readAudioFile(file).getOrThrow().tag
            val value = metadata.getFirst(FieldKey.LYRICS)
            if (!value.isNullOrBlank()) value else null
        } catch (e: CannotReadException) {
            // ignore unsupported format
            if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(file.extension) != e.message) {
                collector?.collect(MetadataExtractingException(e))
            }
            null
        } catch (e: Exception) {
            collector?.collect(MetadataExtractingException(e))
            null
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "JAudioTagger library is corrupted due to obfuscating", e)
            null
        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, "JAudioTagger library is corrupted due to obfuscating", e)
            null
        }
    }

    override fun extractRawImage(path: String, collector: ExceptionCollector?): ByteArray? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val metadata = readAudioFile(file).getOrThrow().tag
            metadata.firstArtwork?.binaryData
        } catch (e: Exception) {
            collector?.collect(e)
            null
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "JAudioTagger library is corrupted due to obfuscating", e)
            null
        } catch (e: ExceptionInInitializerError) {
            Log.e(TAG, "JAudioTagger library is corrupted due to obfuscating", e)
            null
        }
    }

    private fun readAudioFile(file: File): Result<AudioFile> {
        var extension: String = Utils.getExtension(file)
        if (extension.isEmpty()) {
            extension = Utils.getMagicExtension(file)
        }
        return if (extension.isNotEmpty()) {
            try {
                val audioFile = AudioFileIO.readAs(file, extension)
                Result.success(audioFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalStateException("Unable to determine type type for ${file.path}"))
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
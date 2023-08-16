/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.aiff.AiffTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.*
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.mp4.Mp4TagField
import org.jaudiotagger.tag.mp4.field.Mp4TagBinaryField
import org.jaudiotagger.tag.mp4.field.Mp4TagCoverField
import org.jaudiotagger.tag.mp4.field.Mp4TagRawBinaryField
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import org.jaudiotagger.tag.mp4.field.Mp4TagTextField
import org.jaudiotagger.tag.wav.WavTag
import player.phonograph.util.reportError


fun readAllTags(audioFile: AudioFile): Map<String, String> {
    val items: Map<String, String> =
        when (val tag = audioFile.tag) {
            is AbstractID3v2Tag -> ID3v2Readers.ID3v2Reader.read(tag)
            is AiffTag          -> ID3v2Readers.AiffTagReader.read(tag)
            is WavTag           -> ID3v2Readers.WavTagReader.read(tag)
            is ID3v11Tag        -> ID3v1TagReaders.ID3v11TagReader.read(tag)
            is ID3v1Tag         -> ID3v1TagReaders.ID3v1TagReader.read(tag)
            is FlacTag          -> FlacTagReader.read(tag)
            is Mp4Tag           -> Mp4TagReader.read(tag)
            is AbstractTag      -> SimpleKeyValueReader.read(tag)
            else                -> emptyMap()
        }
    return items
}

sealed interface TagReader<T : Tag> {
    fun read(tag: T): Map<String, String>
}

object ID3v1TagReaders {

    private fun readID3v1Tag(tag: ID3v1Tag): Map<String, String> {
        return listOf(
            (tag.title as TagTextField),
            (tag.artist as TagTextField),
            (tag.album as TagTextField),
            (tag.genre as TagTextField),
            (tag.year as TagTextField),
            (tag.comment as TagTextField)
        ).associate {
            (it.id ?: "") to (it.content ?: "")
        }
    }

    private fun readID3v11Tag(tag: ID3v11Tag): Map<String, String> {
        val track = tag.track as TagTextField
        return readID3v1Tag(tag) + mapOf(Pair(track.id, track.content))
    }

    object ID3v1TagReader : TagReader<ID3v1Tag> {
        override fun read(tag: ID3v1Tag): Map<String, String> = readID3v1Tag(tag)
    }

    object ID3v11TagReader : TagReader<ID3v11Tag> {
        override fun read(tag: ID3v11Tag): Map<String, String> = readID3v11Tag(tag)
    }

}

object ID3v2Readers {

    object ID3v2Reader : TagReader<AbstractID3v2Tag> {

        override fun read(tag: AbstractID3v2Tag): Map<String, String> {
            return tag.frameMap
                .mapKeys { (key, frame) ->
                    val name = when (tag) {
                        is ID3v24Tag -> ID3v24FieldKey.values().firstOrNull { key == it.frameId }?.name
                        is ID3v23Tag -> ID3v23FieldKey.values().firstOrNull { key == it.frameId }?.name
                        is ID3v22Tag -> ID3v22FieldKey.values().firstOrNull { key == it.frameId }?.name
                        else         -> null
                    }
                    val frames = when (tag) {
                        is ID3v24Tag -> ID3v24Frames.getInstanceOf()
                        is ID3v23Tag -> ID3v23Frames.getInstanceOf()
                        is ID3v22Tag -> ID3v22Frames.getInstanceOf()
                        else         -> null
                    }
                    if (frames != null) {
                        val description = frames.idToValueMap.getOrDefault(key, ERR_PARSE_KEY)
                        "[$key]$name($description)"
                    } else if (name != null) {
                        "[$key]$name"
                    } else {
                        key
                    }
                }
                .mapValues { (key, data) ->
                    when (data) {
                        is TagField -> {
                            preprocessTagField(data) {
                                if (data is AbstractID3v2Frame) {
                                    parseID3v2Frame(data)
                                } else {
                                    data.rawContent.toString()
                                }
                            }
                        }

                        is List<*>  -> {
                            data.map { item ->
                                if (item is TagField)
                                    preprocessTagField(item) {
                                        if (it is AbstractID3v2Frame) {
                                            parseID3v2Frame(it)
                                        } else {
                                            it.rawContent.toString()
                                        }
                                    }
                                else
                                    item.toString()
                            }.joinToString(separator = "\n") { it }
                        }

                        else        -> data.toString()
                    }
                }
        }

        private fun parseID3v2Frame(frame: AbstractID3v2Frame): String {
            return try {
                when (val frameBody = frame.body) {
                    is FrameBodyTXXX -> "${frameBody.description}:\n\t${frameBody.userFriendlyValue}"
                    else             -> frameBody.userFriendlyValue
                }
            } catch (e: Exception) {
                reportError(e, "readID3v2Tags", ERR_PARSE_FIELD)
                ERR_PARSE_FIELD
            }
        }
    }

    private fun readId3SupportingTag(tag: Id3SupportingTag): Map<String, String> = ID3v2Reader.read(tag.iD3Tag)

    object AiffTagReader : TagReader<AiffTag> {
        override fun read(tag: AiffTag): Map<String, String> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

    object WavTagReader : TagReader<WavTag> {
        override fun read(tag: WavTag): Map<String, String> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

}


object FlacTagReader : TagReader<FlacTag> {
    override fun read(tag: FlacTag): Map<String, String> = SimpleKeyValueReader.read(tag.vorbisCommentTag)
}


object SimpleKeyValueReader : TagReader<AbstractTag> {
    override fun read(tag: AbstractTag): Map<String, String> {
        val mappedFields: Map<String, List<TagField>> = tag.mappedFields
        return mappedFields.mapValues { entry ->
            entry.value.map { tagField ->
                preprocessTagField(tagField) {
                    when (it) {
                        is TagTextField -> it.content
                        else            -> it.rawContent.toString()
                    }
                }
            }.joinToString(separator = "\n") { it }
        }
    }
}

object Mp4TagReader : TagReader<Mp4Tag> {
    override fun read(tag: Mp4Tag): Map<String, String> {
        val fields = tag.all.filterIsInstance<Mp4TagField>()
        val keys = Mp4FieldKey.values()
        return fields.associate { field ->
            val key = run {
                val fieldKey = keys.firstOrNull { field.id == it.fieldName }
                if (fieldKey != null) {
                    "[${fieldKey.fieldName}]${fieldKey.name} ${fieldKey.identifier.orEmpty()}"
                } else {
                    "${field.id}(${field.fieldType.let { "${it.name}<${it.fileClassId}>" }})"
                }
            }
            when (field) {
                is Mp4TagCoverField      -> key to field.toString()
                is Mp4TagBinaryField     -> key to BINARY
                is Mp4TagReverseDnsField -> field.descriptor to field.content
                is Mp4TagTextField       -> key to field.content
                is Mp4TagRawBinaryField  -> key to BINARY
                else                     -> key to ERR_PARSE_FIELD
            }
        }
    }
}

private inline fun <T : TagField> preprocessTagField(
    frame: T,
    block: (frame: T) -> String,
): String =
    when {
        frame.isBinary -> BINARY
        frame.isEmpty  -> EMPTY
        else           -> block(frame)
    }

private const val BINARY = "<Binary Data>"
private const val EMPTY = "<Empty>"

private const val ERR_PARSE_FIELD = "<Err: failed to read field>"
private const val ERR_PARSE_KEY = "<Err: failed to process key>"
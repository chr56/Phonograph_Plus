/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.aiff.AiffTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v11Tag
import org.jaudiotagger.tag.id3.ID3v1Tag
import org.jaudiotagger.tag.id3.ID3v22Frames
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Frames
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.Id3SupportingTag
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
            is AbstractID3v2Tag -> readID3v2Tags(tag)
            is AiffTag          -> readAiffTag(tag)
            is WavTag           -> readWaveTag(tag)
            is ID3v11Tag        -> readID3v11Tags(tag)
            is ID3v1Tag         -> readID3v1Tags(tag)
            is FlacTag          -> readFlacTag(tag)
            is Mp4Tag           -> readMp4Tag(tag)
            is AbstractTag      -> readAbstractTag(tag)
            else                -> emptyMap()
        }
    return items
}

fun readID3v1Tags(tag: ID3v1Tag): Map<String, String> {
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

fun readID3v11Tags(tag: ID3v11Tag): Map<String, String> {
    val track = tag.track as TagTextField
    return readID3v1Tags(tag) + mapOf(Pair(track.id, track.content))
}

fun readAiffTag(tag: AiffTag): Map<String, String> {
    return if (tag.isExistingId3Tag) {
        readId3SupportingTag(tag)
    } else {
        emptyMap()
    }
}

fun readWaveTag(tag: WavTag): Map<String, String> {
    return if (tag.isExistingId3Tag) {
        readId3SupportingTag(tag)
    } else {
        emptyMap()
    }
}

fun readId3SupportingTag(tag: Id3SupportingTag): Map<String, String> = readID3v2Tags(tag.iD3Tag)

fun readID3v2Tags(tag: AbstractID3v2Tag): Map<String, String> {
    return tag.frameMap
        .mapKeys { (key, frame) ->
            val frames = when (tag) {
                is ID3v24Tag -> ID3v24Frames.getInstanceOf()
                is ID3v23Tag -> ID3v23Frames.getInstanceOf()
                is ID3v22Tag -> ID3v22Frames.getInstanceOf()
                else         -> null
            }
            if (frames != null) {
                val description = frames.idToValueMap.getOrDefault(key, ERR_PARSE_KEY)
                "[$key]$description"
            } else {
                key
            }
        }
        .mapValues { (key, data) ->
            when (data) {
                is TagField -> {
                    parseTagField(data) {
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
                            parseTagField(item) {
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

fun readFlacTag(tag: FlacTag): Map<String, String> {
    return readAbstractTag(tag.vorbisCommentTag)
}

fun readAbstractTag(tag: AbstractTag): Map<String, String> {
    val mappedFields: Map<String, List<TagField>> = tag.mappedFields
    return mappedFields.mapValues { entry ->
        entry.value.map { tagField ->
            parseTagField(tagField) {
                when (it) {
                    is TagTextField -> it.content
                    else            -> it.rawContent.toString()
                }
            }
        }.joinToString(separator = "\n") { it }
    }
}

fun readMp4Tag(tag: Mp4Tag): Map<String, String> {
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

private inline fun parseTagField(
    frame: TagField,
    block: (frame: TagField) -> String,
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
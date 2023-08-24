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
import player.phonograph.model.RawTag
import player.phonograph.model.TagData
import player.phonograph.model.TagData.BinaryData
import player.phonograph.model.TagData.EmptyData
import player.phonograph.model.TagData.ErrData
import player.phonograph.model.TagData.MultipleData
import player.phonograph.model.TagData.TextData
import player.phonograph.util.reportError


fun readAllTags(audioFile: AudioFile): Map<String, RawTag> {
    val items: Map<String, RawTag> = try {
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
    } catch (e: Exception) {
        reportError(e, "TagReader", "Failed to read all tags for ${audioFile.file.absolutePath}")
        emptyMap()
    }
    return items
}

sealed interface TagReader<T : Tag> {
    fun read(tag: T): Map<String, RawTag>
}

object ID3v1TagReaders {

    private fun readID3v1Tag(tag: ID3v1Tag): Map<String, RawTag> {
        return listOf(
            (tag.title as TagTextField),
            (tag.artist as TagTextField),
            (tag.album as TagTextField),
            (tag.genre as TagTextField),
            (tag.year as TagTextField),
            (tag.comment as TagTextField)
        ).associate {
            (it.id ?: "") to RawTag(it.id, it.id, TextData(it.content ?: ""), null)
        }
    }

    private fun readID3v11Tag(tag: ID3v11Tag): Map<String, RawTag> {
        val track = tag.track as TagTextField
        return readID3v1Tag(tag) + mapOf(
            Pair(
                track.id,
                RawTag(track.id, track.id, TextData(track.content ?: ""), null)
            )
        )
    }

    object ID3v1TagReader : TagReader<ID3v1Tag> {
        override fun read(tag: ID3v1Tag): Map<String, RawTag> = readID3v1Tag(tag)
    }

    object ID3v11TagReader : TagReader<ID3v11Tag> {
        override fun read(tag: ID3v11Tag): Map<String, RawTag> = readID3v11Tag(tag)
    }

}

object ID3v2Readers {

    object ID3v2Reader : TagReader<AbstractID3v2Tag> {

        override fun read(tag: AbstractID3v2Tag): Map<String, RawTag> {
            return tag.frameMap.map { (key, data) ->

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

                val description: String? = frames?.idToValueMap?.getOrDefault(key, null)


                val value = when (data) {
                    is TagField -> {
                        preprocessTagField(data) {
                            if (data is AbstractID3v2Frame) {
                                parseID3v2Frame(data)
                            } else {
                                TextData(data.rawContent.toString())
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
                                        TextData(it.rawContent.toString())
                                    }
                                }
                            else
                                TextData(item.toString())
                        }.let { MultipleData(it) }
                    }

                    else        -> TextData(data.toString())
                }
                key to RawTag(key, name ?: "?", value, description)
            }.toMap()
        }

        private fun parseID3v2Frame(frame: AbstractID3v2Frame): TagData {
            return try {
                val text = when (val frameBody = frame.body) {
                    is FrameBodyTXXX -> "${frameBody.description}:\t${frameBody.userFriendlyValue}"
                    else             -> frameBody.userFriendlyValue
                }
                TextData(text)
            } catch (e: Exception) {
                reportError(e, "readID3v2Tags", "Failed to read $frame")
                ErrData("Failed to read $frame")
            }
        }
    }

    private fun readId3SupportingTag(tag: Id3SupportingTag): Map<String, RawTag> = ID3v2Reader.read(tag.iD3Tag)

    object AiffTagReader : TagReader<AiffTag> {
        override fun read(tag: AiffTag): Map<String, RawTag> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

    object WavTagReader : TagReader<WavTag> {
        override fun read(tag: WavTag): Map<String, RawTag> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

}


object FlacTagReader : TagReader<FlacTag> {
    override fun read(tag: FlacTag): Map<String, RawTag> = SimpleKeyValueReader.read(tag.vorbisCommentTag)
}


object SimpleKeyValueReader : TagReader<AbstractTag> {
    override fun read(tag: AbstractTag): Map<String, RawTag> {
        val mappedFields: Map<String, List<TagField>> = tag.mappedFields
        return mappedFields.mapValues { (k, tagFields) ->
            val value = tagFields.map { tagField ->
                preprocessTagField(tagField) {
                    when (it) {
                        is TagTextField -> TextData(it.content)
                        else            -> TextData(it.rawContent.toString())
                    }
                }
            }.let { MultipleData(it) }
            RawTag(k, k, value, null)
        }
    }
}

object Mp4TagReader : TagReader<Mp4Tag> {
    override fun read(tag: Mp4Tag): Map<String, RawTag> {
        val fields = tag.all.filterIsInstance<Mp4TagField>()
        val keys = Mp4FieldKey.values()
        return fields.associate { field: Mp4TagField ->
            val fieldKey = keys.firstOrNull { field.id == it.fieldName }
            val id = fieldKey?.fieldName ?: field.id
            val name = fieldKey?.name ?: field.id
            val description = fieldKey?.identifier ?: field.fieldType.let { "${it.name}(type${it.fileClassId})" }
            val value = when (field) {
                is Mp4TagCoverField      -> TextData(field.toString())
                is Mp4TagBinaryField     -> BinaryData
                is Mp4TagReverseDnsField -> TextData("${field.descriptor}: ${field.content}")
                is Mp4TagTextField       -> TextData(field.content)
                is Mp4TagRawBinaryField  -> BinaryData
                else                     -> ErrData("Unknown: $field")
            }
            id to RawTag(id, name, value, description)
        }
    }
}

private inline fun <T : TagField> preprocessTagField(
    frame: T,
    block: (frame: T) -> TagData,
): TagData =
    when {
        frame.isBinary -> BinaryData
        frame.isEmpty  -> EmptyData
        else           -> block(frame)
    }
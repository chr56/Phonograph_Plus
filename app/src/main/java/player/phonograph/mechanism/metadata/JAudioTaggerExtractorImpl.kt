/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.metadata

import org.jaudiotagger.audio.generic.AbstractTag
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
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
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField
import org.jaudiotagger.tag.wav.WavTag
import player.phonograph.App
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata.Field
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.Metadata.EmptyField
import player.phonograph.model.metadata.Metadata.MultipleField
import player.phonograph.model.metadata.Metadata.PlainStringField
import java.io.UnsupportedEncodingException


sealed interface TagReader<T : Tag> {
    fun read(tag: T): Map<String, Field>
}

object ID3v1TagReaders {

    private fun readID3v1Tag(tag: ID3v1Tag): Map<String, Field> = mapOf(
        "Title" to Field("Title", "Title", PlainStringField(tag.firstTitle), null),
        "Artist" to Field("Artist", "Artist", PlainStringField(tag.firstArtist), null),
        "Album" to Field("Album", "Album", PlainStringField(tag.firstAlbum), null),
        "Genre" to Field("Genre", "Genre", PlainStringField(tag.firstGenre), null),
        "Year" to Field("Year", "Year", PlainStringField(tag.firstYear), null),
        "Comment" to Field("Comment", "Comment", PlainStringField(tag.firstComment), null),
    )

    private fun readID3v11Tag(tag: ID3v11Tag): Map<String, Field> {
        return readID3v1Tag(tag) + mapOf(
            "Track" to Field("Track", "Track", PlainStringField(tag.firstTrack), null),
        )
    }

    object ID3v1TagReader : TagReader<ID3v1Tag> {
        override fun read(tag: ID3v1Tag): Map<String, Field> = readID3v1Tag(tag)
    }

    object ID3v11TagReader : TagReader<ID3v11Tag> {
        override fun read(tag: ID3v11Tag): Map<String, Field> = readID3v11Tag(tag)
    }

}

object ID3v2Readers {

    object ID3v2Reader : TagReader<AbstractID3v2Tag> {

        override fun read(tag: AbstractID3v2Tag): Map<String, Field> = when (tag) {
            is ID3v24Tag -> ID3v24Reader.read(tag)
            is ID3v23Tag -> ID3v23Reader.read(tag)
            is ID3v22Tag -> ID3v22Reader.read(tag)
            else         -> emptyMap()
        }

        private sealed class ID3v2ReaderImpl<T : AbstractID3v2Tag> : TagReader<AbstractID3v2Tag> {
            abstract fun queryName(id: String): String
            abstract fun queryDescription(id: String): String?
            abstract fun isCustomKey(id: String): Boolean

            override fun read(tag: AbstractID3v2Tag): Map<String, Field> =
                tag.frameMap.map { (id: String, data) -> id to parse(id, data) }.toMap()

            private fun parse(id: String, frame: Any): Field = when (frame) {
                is TagField -> {
                    parseTagField(id, frame)
                }

                is List<*>  -> {
                    val multiple = MultipleField(
                        frame.map { item ->
                            if (item is TagField) {
                                parseTagField(id, item)
                            } else {
                                otherField(id, frame)
                            }
                        }
                    )
                    val name = if (isCustomKey(id)) "USER DEFINED FIELDS" else queryName(id)
                    Field(id, name, multiple, null)
                }

                else        -> otherField(id, frame)
            }

            private fun parseTagField(id: String, frame: TagField): Field = try {
                if (frame is AbstractID3v2Frame) {
                    val identifier = frame.identifier
                    val name = when (val frameBody = frame.body) {
                        is FrameBodyTXXX -> frameBody.description
                        else             -> queryName(identifier)
                    }
                    val description = queryDescription(identifier)
                    val field = when (val frameBody = frame.body) {
                        is FrameBodyTXXX -> PlainStringField("${frameBody.description}:\t${frameBody.userFriendlyValue}")
                        else             -> preprocessTagField(frame) { PlainStringField(frameBody.userFriendlyValue) }
                    }
                    Field(identifier, name, field, description)
                } else {
                    val field = preprocessTagField(frame) {
                        PlainStringField(FieldOf(frame))
                    }
                    Field(id, id, field, null)
                }
            } catch (e: TagException) {
                warning(App.instance, "ID3v2Reader", "Failed to process Frame $frame", e)
                Field(id, id, PlainStringField(frame.toString()), null)
            }

            private fun otherField(id: String, frame: Any) =
                Field(id, id, PlainStringField(frame.toString()), null)
        }

        private object ID3v24Reader : ID3v2ReaderImpl<ID3v23Tag>() {
            override fun queryName(id: String): String =
                ID3v24FieldKey.entries.firstOrNull { id == it.frameId }?.name ?: id

            override fun queryDescription(id: String): String? =
                ID3v24Frames.getInstanceOf().idToValueMap?.getOrDefault(id, null)

            override fun isCustomKey(id: String): Boolean = id == ID3v24Frames.FRAME_ID_USER_DEFINED_INFO

        }

        private object ID3v23Reader : ID3v2ReaderImpl<ID3v23Tag>() {
            override fun queryName(id: String): String =
                ID3v23FieldKey.entries.firstOrNull { id == it.frameId }?.name ?: id

            override fun queryDescription(id: String): String? =
                ID3v23Frames.getInstanceOf().idToValueMap?.getOrDefault(id, null)

            override fun isCustomKey(id: String): Boolean = id == ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO
        }

        private object ID3v22Reader : ID3v2ReaderImpl<ID3v22Tag>() {
            override fun queryName(id: String): String =
                ID3v22FieldKey.entries.firstOrNull { id == it.frameId }?.name ?: id

            override fun queryDescription(id: String): String? =
                ID3v22Frames.getInstanceOf().idToValueMap?.getOrDefault(id, null)

            override fun isCustomKey(id: String): Boolean = id == ID3v22Frames.FRAME_ID_V2_USER_DEFINED_INFO
        }
    }

    private fun readId3SupportingTag(tag: Id3SupportingTag): Map<String, Field> = ID3v2Reader.read(tag.iD3Tag)

    object AiffTagReader : TagReader<AiffTag> {
        override fun read(tag: AiffTag): Map<String, Field> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

    object WavTagReader : TagReader<WavTag> {
        override fun read(tag: WavTag): Map<String, Field> =
            if (tag.isExistingId3Tag) {
                readId3SupportingTag(tag)
            } else {
                emptyMap()
            }
    }

}


object FlacTagReader : TagReader<FlacTag> {
    override fun read(tag: FlacTag): Map<String, Field> = SimpleKeyValueReader.read(tag.vorbisCommentTag)
}


object SimpleKeyValueReader : TagReader<AbstractTag> {
    override fun read(tag: AbstractTag): Map<String, Field> {
        val mappedFields: Map<String, List<TagField>> = tag.mappedFields
        return mappedFields.mapValues { (k, tagFields) ->
            val value = tagFields.map { tagField ->
                preprocessTagField(tagField) {
                    when (it) {
                        is TagTextField -> PlainStringField(it.content)
                        else            -> PlainStringField(FieldOf(it).take(64))
                    }
                }
            }.let { MultipleField(it) }
            Field(k, k, value, null)
        }
    }
}

object Mp4TagReader : TagReader<Mp4Tag> {
    override fun read(tag: Mp4Tag): Map<String, Field> {
        val fields = tag.all.filterIsInstance<Mp4TagField>()
        val keys = Mp4FieldKey.entries
        return fields.associate { field: Mp4TagField ->
            val fieldKey = keys.firstOrNull { field.id == it.fieldName }
            val id = fieldKey?.fieldName ?: field.id
            val name = fieldKey?.name ?: field.id
            val description = fieldKey?.identifier ?: field.fieldType.let { "${it.name}(type${it.fileClassId})" }
            val value = when (field) {
                is Mp4TagCoverField -> PlainStringField(field.toString())
                is Mp4TagReverseDnsField -> PlainStringField("${field.descriptor}: ${field.content}")
                is Mp4TagTextField -> PlainStringField(field.content)
                is Mp4TagBinaryField -> RawBinaryField(field.rawContent)
                is Mp4TagRawBinaryField -> RawBinaryField(field.rawContent)
                else -> PlainStringField("Unknown: $field")
            }
            id to Field(id, name, value, description)
        }
    }
}

object VorbisCommentTagReader : TagReader<VorbisCommentTag> {
    override fun read(tag: VorbisCommentTag): Map<String, Field> {
        val mappedFields: Map<String, List<TagField>> = tag.mappedFields
        return mappedFields.mapValues { (key, tagFields) ->
            val value = tagFields.map { tagField ->
                if (tagField is VorbisCommentTagField) {
                    val imageTags = listOf(
                        VorbisCommentFieldKey.METADATA_BLOCK_PICTURE.fieldName,
                        VorbisCommentFieldKey.COVERART.fieldName,
                    )
                    if (key in imageTags) {
                        PlainStringField("<BASE64_IMAGES>")
                    } else {
                        PlainStringField(tagField.content)
                    }
                } else {
                    PlainStringField("Unknown field (${FieldOf(tagField).take(24)})")
                }
            }.let { MultipleField(it) }
            Field(key, key, value, null)
        }
    }
}


private inline fun <T : TagField> preprocessTagField(
    frame: T,
    block: (frame: T) -> Metadata.Field,
): Metadata.Field =
    when {
        frame.isBinary -> RawBinaryField(frame.rawContent)
        frame.isEmpty  -> EmptyField
        else           -> block(frame)
    }

private fun FieldOf(field: TagField): String =
    try {
        field.rawContent.toString()
    } catch (e: UnsupportedEncodingException) {
        // ID3 AggregatedFrame may throw `UnsupportedEncodingException` but has `getContent()`
        if (field is TagTextField) field.content else field.toString()
    }

class RawBinaryField(val data: ByteArray) : Metadata.BinaryField {
    override fun binary(): ByteArray = data
}
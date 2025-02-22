/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.metadata

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
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField
import org.jaudiotagger.tag.wav.WavTag
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata.Field
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.Metadata.EmptyField
import player.phonograph.model.metadata.Metadata.MultipleField
import player.phonograph.model.metadata.Metadata.PlainStringField
import player.phonograph.util.reportError
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

        override fun read(tag: AbstractID3v2Tag): Map<String, Field> {
            return tag.frameMap.map { (key, data) ->

                val name = when (tag) {
                    is ID3v24Tag -> ID3v24FieldKey.entries.firstOrNull { key == it.frameId }?.name
                    is ID3v23Tag -> ID3v23FieldKey.entries.firstOrNull { key == it.frameId }?.name
                    is ID3v22Tag -> ID3v22FieldKey.entries.firstOrNull { key == it.frameId }?.name
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
                                PlainStringField(FieldOf(data))
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
                                        PlainStringField(FieldOf(it))
                                    }
                                }
                            else
                                PlainStringField(item.toString())
                        }.let { MultipleField(it) }
                    }

                    else        -> PlainStringField(data.toString())
                }
                key to Field(key, name ?: "?", value, description)
            }.toMap()
        }

        private fun parseID3v2Frame(frame: AbstractID3v2Frame): PlainStringField {
            return try {
                val text = when (val frameBody = frame.body) {
                    is FrameBodyTXXX -> "${frameBody.description}:\t${frameBody.userFriendlyValue}"
                    else             -> frameBody.userFriendlyValue
                }
                PlainStringField(text)
            } catch (e: Exception) {
                reportError(e, "readID3v2Tags", "Failed to read $frame")
                PlainStringField("Failed to read $frame")
            }
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
                is Mp4TagCoverField      -> PlainStringField(field.toString())
                is Mp4TagReverseDnsField -> PlainStringField("${field.descriptor}: ${field.content}")
                is Mp4TagTextField       -> PlainStringField(field.content)
                is Mp4TagBinaryField     -> RawBinaryField(field.rawContent)
                is Mp4TagRawBinaryField  -> RawBinaryField(field.rawContent)
                else                     -> PlainStringField("Unknown: $field")
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
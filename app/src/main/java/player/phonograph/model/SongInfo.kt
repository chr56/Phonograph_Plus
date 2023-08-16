/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.TagFormat
import androidx.annotation.StringRes

/**
 * class describing a song file
 */
class SongInfoModel(
    //
    // file
    //
    var fileName: StringFilePropertyField,
    var filePath: StringFilePropertyField,
    var fileFormat: StringFilePropertyField,
    var bitRate: StringFilePropertyField,
    var samplingRate: StringFilePropertyField,
    var fileSize: LongFilePropertyField,
    var trackLength: LongFilePropertyField,
    //
    // tags
    //
    var tagFields: Map<FieldKey, TagField>,
    var tagFormat: TagFormat = TagFormat.Unknown,
    var allTags: Map<String, String>? = null,
) {


    /**
     * retrieve corresponding [TagField] for a music tag
     */
    fun tagValue(key: FieldKey): TagField =
        tagFields[key] ?: throw IllegalStateException("unknown field: ${key.name}")

    companion object {
        @Suppress("FunctionName")
        fun EMPTY(): SongInfoModel =
            SongInfoModel(
                StringFilePropertyField(null),
                StringFilePropertyField(null),
                StringFilePropertyField(null),
                StringFilePropertyField(null),
                StringFilePropertyField(null),
                LongFilePropertyField(-1),
                LongFilePropertyField(-1),
                emptyMap(),
                TagFormat.Unknown,
                null,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SongInfoModel) return false

        if (fileName != other.fileName) return false
        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int = fileName.hashCode() * 31 + filePath.hashCode()

}


/**
 * retrieve corresponding a tag name string resource id for a music tag
 */
@StringRes
fun songTagNameRes(field: FieldKey): Int =
    when (field) {
        FieldKey.TITLE        -> R.string.title
        FieldKey.ARTIST       -> R.string.artist
        FieldKey.ALBUM        -> R.string.album
        FieldKey.ALBUM_ARTIST -> R.string.album_artist
        FieldKey.COMPOSER     -> R.string.composer
        FieldKey.LYRICIST     -> R.string.lyricist
        FieldKey.YEAR         -> R.string.year
        FieldKey.GENRE        -> R.string.genre
        FieldKey.DISC_NO      -> R.string.disk_number
        FieldKey.TRACK        -> R.string.track
        FieldKey.COMMENT      -> R.string.comment
        else                  -> -1
    }

sealed interface Field<T> {
    fun value(): T
}

// class FilePropertyField<T>(protected val _value: T) : Field<T> {
//     override fun value(): T = _value
// }

class StringFilePropertyField(private val _value: String?) : Field<String> {
    override fun value(): String = _value ?: ""
}

class LongFilePropertyField(private val _value: Long) : Field<Long> {
    override fun value(): Long = _value
}

class TagField(val key: FieldKey, private val _value: String?) : Field<String> {
    override fun value(): String = _value ?: ""
    fun copy(newValue: String?): TagField = TagField(key, newValue)
    fun songTagName(): Int = songTagNameRes(key)
}


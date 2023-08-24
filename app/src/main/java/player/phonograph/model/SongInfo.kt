/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.TagFormat
import androidx.annotation.StringRes
import android.content.res.Resources

/**
 * class describing a song file
 */
class SongInfoModel(
    val fileName: StringFilePropertyField,
    val filePath: StringFilePropertyField,
    val fileSize: LongFilePropertyField,
    val audioPropertyFields: Map<FilePropertyField.Key, FilePropertyField<out Any>>,
    val tagFields: Map<FieldKey, TagField>,
    val tagFormat: TagFormat,
    val allTags: Map<String, TagData>,
) {

    companion object {
        @Suppress("FunctionName")
        fun EMPTY(): SongInfoModel =
            SongInfoModel(
                StringFilePropertyField(null),
                StringFilePropertyField(null),
                LongFilePropertyField(-1),
                emptyMap(),
                emptyMap(),
                TagFormat.Unknown,
                emptyMap(),
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
fun FieldKey.res(): Int =
    when (this) {
        FieldKey.TITLE        -> R.string.title
        FieldKey.ARTIST       -> R.string.artist
        FieldKey.ALBUM        -> R.string.album
        FieldKey.ALBUM_ARTIST -> R.string.album_artist
        FieldKey.COMPOSER     -> R.string.composer
        FieldKey.LYRICIST     -> R.string.lyricist
        FieldKey.YEAR         -> R.string.year
        FieldKey.GENRE        -> R.string.genre
        FieldKey.DISC_NO      -> R.string.disk_number
        FieldKey.DISC_TOTAL   -> R.string.disk_number
        FieldKey.TRACK        -> R.string.track
        FieldKey.TRACK_TOTAL  -> R.string.track_total
        FieldKey.RATING       -> R.string.rating
        FieldKey.COMMENT      -> R.string.comment
        else                  -> -1
    }

fun FieldKey.text(resources: Resources): String {
    val stringRes = res()
    return if (stringRes > 0) resources.getString(stringRes) else name
}

val allFieldKey =
    setOf(
        FieldKey.TITLE,
        FieldKey.ARTIST,
        FieldKey.ALBUM,
        FieldKey.ALBUM_ARTIST,
        FieldKey.COMPOSER,
        FieldKey.LYRICIST,
        FieldKey.YEAR,
        FieldKey.GENRE,
        FieldKey.DISC_NO,
        FieldKey.DISC_TOTAL,
        FieldKey.TRACK,
        FieldKey.TRACK_TOTAL,
        FieldKey.RATING,
        FieldKey.COMMENT,
    ) + FieldKey.values()

sealed interface Field<T> {
    fun value(): T
}

abstract class FilePropertyField<T> : Field<T> {
    enum class Key(@StringRes val res: Int) {
        TRACK_LENGTH(R.string.label_track_length),
        FILE_FORMAT(R.string.label_file_format),
        BIT_RATE(R.string.label_bit_rate),
        SAMPLING_RATE(R.string.label_sampling_rate),
        ;

        fun label(resources: Resources): String = resources.getString(res)
    }
}

class StringFilePropertyField(private val _value: String?) : FilePropertyField<String>() {
    override fun value(): String = _value ?: ""
}

class LongFilePropertyField(private val _value: Long) : FilePropertyField<Long>() {
    override fun value(): Long = _value
}

class TagField(val key: FieldKey, val content: TagData) : Field<String> {
    override fun value(): String = content.text()
}


sealed interface TagData {

    fun text(): String

    data class TextData(val content: String) : TagData {
        override fun text(): String = content
    }

    data class MultipleData(val contents: Collection<*>) : TagData {
        override fun text(): String {
            return contents.joinToString(separator = "\n") { it.toString() }
        }
    }

    object EmptyData : TagData {
        override fun text(): String = "<Empty>"
    }

    object BinaryData : TagData {
        override fun text(): String = "<Binary>"
    }

    class ErrData(val message: CharSequence) : TagData {
        override fun text(): String = "<Error: $message>"
    }
}


data class RawTag(
    val id: String,
    val name: String,
    val value: TagData,
    val description: String?,
)
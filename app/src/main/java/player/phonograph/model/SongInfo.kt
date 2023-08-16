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
    val tagFormat: TagFormat = TagFormat.Unknown,
    val allTags: Map<String, String>? = null,
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
                LongFilePropertyField(-1),
                emptyMap(),
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

class TagField(val key: FieldKey, private val _value: String?) : Field<String> {
    override fun value(): String = _value ?: ""
    fun copy(newValue: String?): TagField = TagField(key, newValue)
    fun songTagName(): Int = songTagNameRes(key)
}


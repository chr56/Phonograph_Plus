/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import org.jaudiotagger.tag.FieldKey


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
    // title
    //
    var title: TagField,
    var artist: TagField,
    var album: TagField,
    var albumArtist: TagField,
    var composer: TagField,
    var lyricist: TagField,
    var year: TagField,
    var genre: TagField,
    var track: TagField,
    //
    // other
    //
    var comment: TagField,
    var otherTags: MutableMap<String, String>? = null,
) {
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
                TagField(FieldKey.TITLE, null),
                TagField(FieldKey.ARTIST, null),
                TagField(FieldKey.ALBUM, null),
                TagField(FieldKey.ALBUM_ARTIST, null),
                TagField(FieldKey.COMPOSER, null),
                TagField(FieldKey.LYRICIST, null),
                TagField(FieldKey.YEAR, null),
                TagField(FieldKey.GENRE, null),
                TagField(FieldKey.TRACK, null),
                TagField(FieldKey.COMMENT, null),
                null
            )
    }
}



sealed interface Field<T> {
    fun value(): T
}

// class FilePropertyField<T>(protected val _value: T) : Field<T> {
//     override fun value(): T = _value
// }

class StringFilePropertyField(private val _value: String?) : Field<String> {
    override fun value(): String = _value ?: "N/A"
}

class LongFilePropertyField(private val _value: Long) : Field<Long> {
    override fun value(): Long = _value
}

class TagField(val key: FieldKey, private val _value: String?) : Field<String> {
    override fun value(): String = _value ?: "-"
    fun copy(newValue: String?): TagField = TagField(key, newValue)
}


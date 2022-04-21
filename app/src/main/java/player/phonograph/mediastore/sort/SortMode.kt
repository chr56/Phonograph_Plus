/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore.sort

import java.lang.IllegalArgumentException
import kotlin.jvm.Throws

data class SortMode(val sortRef: SortRef, val n: Boolean = false) {
    fun serialize(): String =
        "${sortRef.serializedName}:${if (!n) "0" else "1"}"
    fun deserialize(str: String): SortMode {
        val array = str.split(Regex(".*:.*"), 0)
        return SortMode(
            SortRef.deserialize(array[0]), array[1] != "0"
        )
    }
}

enum class SortRef(val serializedName: String) {

    ID("id"),
    SONG_NAME("song_name"),
    ARTIST_NAME("artist_name"),
    ALBUM_NAME("number"),
    ADDED_DATE("added_date"),
    MODIFIED_DATE("modified_date"),
    SONG_COUNT("song_count"),
    ALBUM_COUNT("album_count"),
    SONG_DURATION("song_duration"),
    GENRE_NAME("genre_name"),
    YEAR("year");

    companion object {
        @Throws(IllegalArgumentException::class)
        fun deserialize(serializedName: String): SortRef {
            return when (serializedName) {
                "id" -> ID
                "song_name" -> SONG_NAME
                "artist_name" -> ARTIST_NAME
                "number" -> ALBUM_NAME
                "added_date" -> ADDED_DATE
                "modified_date" -> MODIFIED_DATE
                "song_count" -> SONG_COUNT
                "album_count" -> ALBUM_COUNT
                "song_duration" -> SONG_DURATION
                "genre_name" -> GENRE_NAME
                "year" -> YEAR
                else -> throw IllegalArgumentException("unknown SortRef")
            }
        }
    }
}

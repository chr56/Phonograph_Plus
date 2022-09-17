/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.sort

import java.lang.IllegalArgumentException
import kotlin.jvm.Throws

enum class SortRef(val serializedName: String) {

    ID("id"),
    SONG_NAME("song_name"),
    ALBUM_NAME("album_name"),
    ARTIST_NAME("artist_name"),
    DURATION(""),
    YEAR("year"),
    ADDED_DATE("added_date"),
    MODIFIED_DATE("modified_date"),
    DISPLAY_NAME("display_name"),
    SONG_COUNT("song_count"),
    ALBUM_COUNT("album_count"),
    SIZE("size"),
    PATH("path"),
    ;

    companion object {
        @Throws(IllegalArgumentException::class)
        fun deserialize(serializedName: String): SortRef {
            return when (serializedName) {
                "id" -> ID
                "song_name" -> SONG_NAME
                "album_name" -> ALBUM_NAME
                "artist_name" -> ARTIST_NAME
                "year" -> YEAR
                "added_date" -> ADDED_DATE
                "modified_date" -> MODIFIED_DATE
                "duration" -> DURATION
                "display_name" -> DISPLAY_NAME
                "song_count" -> SONG_COUNT
                "album_count" -> ALBUM_COUNT
                "size" -> SIZE
                "path" -> PATH
                else -> ID
            }
        }
    }
}
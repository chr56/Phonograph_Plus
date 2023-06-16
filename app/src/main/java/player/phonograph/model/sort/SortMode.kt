/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.sort

import android.provider.MediaStore.Audio

data class SortMode(val sortRef: SortRef, val revert: Boolean = false) {

    companion object {
        fun deserialize(str: String): SortMode {
            val array = str.split(':')
            return if (array.size != 2) SortMode(SortRef.ID) else
                SortMode(
                    SortRef.deserialize(array[0]), array[1] != "0"
                )
        }
    }

    fun serialize(): String =
        "${sortRef.serializedName}:${if (!revert) "0" else "1"}"

    @Suppress("PropertyName")
    val SQLQuerySortOrder: String
        get() {
            val first = when (sortRef) {
                SortRef.ID                -> Audio.AudioColumns._ID
                SortRef.SONG_NAME         -> Audio.Media.DEFAULT_SORT_ORDER
                SortRef.ARTIST_NAME       -> Audio.Artists.DEFAULT_SORT_ORDER
                SortRef.ALBUM_NAME        -> Audio.Albums.DEFAULT_SORT_ORDER
                SortRef.ALBUM_ARTIST_NAME -> Audio.Media.ALBUM_ARTIST
                SortRef.COMPOSER          -> Audio.Media.COMPOSER
                SortRef.ADDED_DATE        -> Audio.Media.DATE_ADDED
                SortRef.MODIFIED_DATE     -> Audio.Media.DATE_MODIFIED
                SortRef.DURATION          -> Audio.Media.DURATION
                SortRef.YEAR              -> Audio.Media.YEAR
                // SortRef.DISPLAY_NAME      -> Audio.Genres.DEFAULT_SORT_ORDER // todo
                // SortRef.SONG_COUNT        -> "" // todo
                // SortRef.ALBUM_COUNT       -> "" // todo
                else                      -> throw IllegalStateException("invalid sort mode")
            }
            val second = if (revert) "DESC" else "ASC"

            return "$first $second"
        }
}

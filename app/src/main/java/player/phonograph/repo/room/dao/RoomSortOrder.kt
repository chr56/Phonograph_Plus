/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.room.entity.Columns.ALBUM
import player.phonograph.repo.room.entity.Columns.ALBUM_ARTIST
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.COMPOSER
import player.phonograph.repo.room.entity.Columns.DATE_ADDED
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.DURATION
import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Columns.MEDIASTORE_PATH
import player.phonograph.repo.room.entity.Columns.SONG_COUNT
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.Columns.YEAR
import player.phonograph.repo.room.entity.Columns.GENRE
import player.phonograph.repo.room.entity.Columns.GENRE_ID

object RoomSortOrder {

    fun roomSongQuerySortOrder(mode: SortMode): String =
        "${roomSongQuerySortRef(mode.sortRef)} ${if (mode.revert) "DESC" else "ASC"}"

    private fun roomSongQuerySortRef(sortRef: SortRef): String = when (sortRef) {
        SortRef.ID                -> MEDIASTORE_ID
        SortRef.PATH              -> MEDIASTORE_PATH
        SortRef.DISPLAY_NAME      -> TITLE
        SortRef.SONG_NAME         -> TITLE
        SortRef.ALBUM_NAME        -> ALBUM
        SortRef.ARTIST_NAME       -> ARTIST
        SortRef.ALBUM_ARTIST_NAME -> ALBUM_ARTIST
        SortRef.COMPOSER          -> COMPOSER
        SortRef.YEAR              -> YEAR
        SortRef.DURATION          -> DURATION
        SortRef.ADDED_DATE        -> DATE_ADDED
        SortRef.MODIFIED_DATE     -> DATE_MODIFIED
        else                      -> MEDIASTORE_ID // invalid for songs
    }

    fun roomArtistQuerySortOrder(mode: SortMode): String =
        "${roomArtistQuerySortRef(mode.sortRef)} ${if (mode.revert) "DESC" else "ASC"}"

    private fun roomArtistQuerySortRef(sortRef: SortRef): String = when (sortRef) {
        SortRef.ID          -> ARTIST_ID
        SortRef.ARTIST_NAME -> ARTIST
        SortRef.ALBUM_NAME  -> ALBUM
        SortRef.SONG_COUNT  -> SONG_COUNT
        else                -> ARTIST_ID // invalid for artists
    }


    fun roomAlbumQuerySortOrder(mode: SortMode): String =
        "${roomAlbumQuerySortRef(mode.sortRef)} ${if (mode.revert) "DESC" else "ASC"}"

    private fun roomAlbumQuerySortRef(sortRef: SortRef): String = when (sortRef) {
        SortRef.ID                -> ALBUM_ID
        SortRef.ALBUM_NAME        -> ALBUM
        SortRef.ARTIST_NAME       -> ALBUM_ARTIST
        SortRef.ALBUM_ARTIST_NAME -> ALBUM_ARTIST
        SortRef.YEAR              -> YEAR
        SortRef.MODIFIED_DATE     -> DATE_MODIFIED
        SortRef.SONG_COUNT        -> SONG_COUNT
        else                      -> ALBUM_ID // invalid for albums
    }

    fun roomGenreQuerySortOrder(mode: SortMode): String =
        "${roomGenreQuerySortRef(mode.sortRef)} ${if (mode.revert) "DESC" else "ASC"}"

    private fun roomGenreQuerySortRef(sortRef: SortRef): String = when (sortRef) {
        SortRef.ID                -> GENRE_ID
        SortRef.DISPLAY_NAME      -> GENRE
        SortRef.SONG_COUNT        -> SONG_COUNT
        else                      -> GENRE_ID // invalid for genres
    }



    val defaultSongSortMode: SortMode get() = SortMode(SortRef.MODIFIED_DATE, true)

    val defaultAlbumSortMode: SortMode get() = SortMode(SortRef.SONG_COUNT, true)

    val defaultArtistSortMode: SortMode get() = SortMode(SortRef.SONG_COUNT, true)

    val defaultGenreSortMode: SortMode get() = SortMode(SortRef.DISPLAY_NAME, true)
}


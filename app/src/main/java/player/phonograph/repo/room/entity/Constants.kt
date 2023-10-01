/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

object Tables {
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val LINKAGE_ARTIST_SONG = "linkage_artist_song"
    const val LINKAGE_ARTIST_ALBUM = "linkage_artist_album"
}

object Columns {
    const val SONG_ID = "song_id"
    const val PATH = "path"
    const val DATE_ADDED = "date_added"
    const val DATE_MODIFIED = "date_modified"
    const val TITLE = "title"
    const val RAW_ARTIST_ID = "raw_artist_id"
    const val RAW_ARTIST_NAME = "raw_artist_name"
    const val ALBUM_ID = "album_id"
    const val ALBUM_NAME = "album_name"
    const val ARTIST_ID = "artist_id"
    const val ARTIST_NAME = "artist_name"
    const val ALBUM_ARTIST_NAME = "album_artist_name"
    const val COMPOSER = "composer"
    const val YEAR = "year"
    const val DURATION = "duration"
    const val TRACK_NUMBER = "track_number"
    const val ROLE = "role"
    const val SONG_COUNT = "song_count"
    const val ALBUM_COUNT = "album_count"
}

const val UNKNOWN_ALBUM_DISPLAY_NAME = "Unnamed Album"
const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"
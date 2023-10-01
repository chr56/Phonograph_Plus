/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

object Columns {

    const val MEDIASTORE_ID = "_id" // "song_id"
    const val MEDIASTORE_PATH = "_data" // "path"

    // common
    const val DURATION = "duration"
    const val DATE_ADDED = "date_added"
    const val DATE_MODIFIED = "date_modified"
    const val TITLE = "title"
    const val ALBUM_ID = "album_id"
    const val ALBUM = "album"
    const val ARTIST_ID = "artist_id"
    const val ARTIST = "artist"
    const val YEAR = "year"
    const val TRACK = "track"
    const val ALBUM_ARTIST = "album_artist"
    const val COMPOSER = "composer"

    // playlist
    const val PLAYLIST_ID = "playlist_id"
    const val PLAYLIST_NAME = "playlist_name"
    const val PLAYLIST_SONG_ID = "playlist_song_id"

    const val POSITION = "position"
    const val PATH = "path"

    // favorite playlist

    const val TYPE = "type"
    const val PRIMARY_ID = "primary_id"
    const val SUB_ID = "sub_id"
    const val LOCATION = "location"


    // metadata
    const val METADATA_KEY = "key"
    const val METADATA_VALUE = "value"


    // Misc
    const val ROLE = "role"
    const val SONG_COUNT = "song_count"
    const val ALBUM_COUNT = "album_count"
}
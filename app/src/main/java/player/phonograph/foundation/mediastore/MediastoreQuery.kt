/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.mediastore

import player.phonograph.foundation.compat.MediaStoreCompat
import player.phonograph.model.Song
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns

val BASE_SONG_PROJECTION = arrayOf(
    BaseColumns._ID, // 0
    AudioColumns.TITLE, // 1
    AudioColumns.TRACK, // 2
    AudioColumns.YEAR, // 3
    AudioColumns.DURATION, // 4
    AudioColumns.DATA, // 5
    AudioColumns.DATE_ADDED, // 6
    AudioColumns.DATE_MODIFIED, // 7
    AudioColumns.ALBUM_ID, // 8
    AudioColumns.ALBUM, // 9
    AudioColumns.ARTIST_ID, // 10
    AudioColumns.ARTIST, // 11
    AudioColumns.ALBUM_ARTIST, // 12 (hidden api before R)
    AudioColumns.COMPOSER, // 13 (hidden api before R)
)

val EXTENDED_SONG_PROJECTION =
    BASE_SONG_PROJECTION + arrayOf(
        /////////////////////////////////////////////////
        AudioColumns.SIZE, // 14
        AudioColumns.DISPLAY_NAME, // 15
    )

const val BASE_AUDIO_SELECTION =
    "${AudioColumns.IS_MUSIC} =1 "

const val BASE_PLAYLIST_SELECTION =
    "${MediaStoreCompat.Audio.PlaylistsColumns.NAME} != '' "


/**
 * read cursor as [Song]
 *
 * **Requirement:**
 * - [cursor] is queried from **[BASE_SONG_PROJECTION]**
 * - [cursor] is **not empty**
 *
 */
fun readSong(cursor: Cursor): Song {
    val id = cursor.getLong(0)
    val title = cursor.getString(1)
    val trackNumber = cursor.getInt(2)
    val year = cursor.getInt(3)
    val duration = cursor.getLong(4)
    val data = cursor.getString(5)
    val dateAdded = cursor.getLong(6)
    val dateModified = cursor.getLong(7)
    val albumId = cursor.getLong(8)
    val albumName = cursor.getString(9)
    val artistId = cursor.getLong(10)
    val artistName = cursor.getString(11)
    val albumArtist = cursor.getString(12)
    val composer = cursor.getString(13)
    return Song(
        id = id,
        title = title,
        trackNumber = trackNumber,
        year = year,
        duration = duration,
        data = data,
        dateAdded = dateAdded,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtist,
        composer = composer,
    )
}

/**
 * consume this cursor (read & close) and convert into a song that at top of cursor
 */
fun Cursor?.intoFirstSong(): Song? =
    this?.use {
        if (moveToFirst()) readSong(this) else null
    }

/**
 * consume this cursor (read & close) and convert into a song list
 */
fun Cursor?.intoSongs(): List<Song> {
    return this?.use {
        val songs = mutableListOf<Song>()
        if (moveToFirst()) {
            do {
                songs.add(readSong(this))
            } while (moveToNext())
        }
        songs
    } ?: emptyList()
}
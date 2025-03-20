/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import android.annotation.SuppressLint
import android.database.Cursor
import android.os.storage.StorageManager


/**
 * consume cursor (read & close) and convert into a song that at top of cursor
 */
fun Cursor?.intoFirstSong(): Song? =
    this?.use {
        if (moveToFirst()) readSong(this) else null
    }

/**
 * consume cursor (read & close) and convert into song list
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

/**
 * read cursor as [Song]
 * (**require [cursor] not empty**)
 * @see [BASE_SONG_PROJECTION]
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
 * read audio file cursor as [FileEntity]
 * (**require [cursor] not empty**)
 * @param currentLocation location where treats as base
 * @see [BASE_FILE_PROJECTION]
 */
@SuppressLint("Range")
fun readFileEntity(
    cursor: Cursor,
    currentLocation: Location,
    storageManager: StorageManager,
): FileEntity {
    val id = cursor.getLong(0)
    val displayName = cursor.getString(1)
    val absolutePath = cursor.getString(2)
    val size = cursor.getLong(3)
    val dateAdded = cursor.getLong(4)
    val dateModified = cursor.getLong(5)

    val relativePath = absolutePath.stripToRelativePath(currentLocation.absolutePath)

    return if (relativePath.contains('/')) {
        val folderName = relativePath.substringBefore('/')
        val folderPath = absolutePath.substringBefore(relativePath.substringAfter('/'))
        // folder
        FileEntity.Folder(
            location = Locations.from(folderPath, storageManager),
            name = folderName,
            dateAdded = dateAdded,
            dateModified = dateModified
        )
    } else {
        // file
        FileEntity.File(
            location = Locations.from(absolutePath, storageManager),
            name = displayName,
            id = id,
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified
        )
    }
}

private fun String.stripToRelativePath(currentLocationAbsolutePath: String) =
    substringAfter(currentLocationAbsolutePath).removePrefix("/")
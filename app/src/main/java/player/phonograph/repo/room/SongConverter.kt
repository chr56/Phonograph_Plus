/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.model.Song
import androidx.room.TypeConverter

object SongConverter {
    @TypeConverter
    fun fromSongModel(song: Song): player.phonograph.repo.room.entity.Song {
        // todo
        return player.phonograph.repo.room.entity.Song(
            id = song.id,
            path = song.data,
            size = 0,
            displayName = "",
            dateAdded = song.dateModified,
            dateModified = song.dateModified,
            title = song.title,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            albumArtistName = song.albumArtistName,
            composer = song.composer,
            year = song.year,
            duration = song.duration,
            trackNumber = song.trackNumber
        )
    }

    @TypeConverter
    fun toSongModel(song: player.phonograph.repo.room.entity.Song): Song {
        return Song(
            id = song.id,
            title = song.title ?: "UNKNOWN",
            trackNumber = song.trackNumber,
            year = song.year,
            duration = song.duration,
            data = song.path,
            dateAdded = song.dateModified,
            dateModified = song.dateModified,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            albumArtistName = song.albumArtistName,
            composer = song.composer,
        )
    }
}
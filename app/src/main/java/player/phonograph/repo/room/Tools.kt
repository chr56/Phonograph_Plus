/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("unused")

package player.phonograph.repo.room

import androidx.room.TypeConverter
import player.phonograph.model.Song as OldSongModel

object SongConverter {
    @TypeConverter
    fun fromSongModel(song: OldSongModel): Song {
        // todo
        return Song(
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
    fun toSongModel(song: Song): OldSongModel {
        return OldSongModel(
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

object SongMarker {

    @TypeConverter
    fun getAlbum(song: Song): Album {
        return Album(song.albumId, song.albumName)
    }
    @TypeConverter
    fun getArtist(song: Song): Artist {
        return Artist(song.artistId, song.artistName ?: "")
    }
}


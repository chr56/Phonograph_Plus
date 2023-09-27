/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("unused")

package player.phonograph.repo.room

import player.phonograph.repo.room.entity.Album
import player.phonograph.repo.room.entity.Artist
import player.phonograph.repo.room.entity.Song
import androidx.room.TypeConverter

object SongMarker {

    @TypeConverter
    fun getAlbum(song: Song): Album {
        return Album(song.albumId, song.albumName, song.albumArtistName)
    }
    @TypeConverter
    fun getArtist(song: Song): Artist {
        return Artist(song.artistId, song.artistName ?: "")
    }
}


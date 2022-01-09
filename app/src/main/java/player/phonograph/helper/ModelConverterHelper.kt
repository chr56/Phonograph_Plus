/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.helper

import player.phonograph.database.mediastore.Converter
import player.phonograph.database.mediastore.Song as NewSong
import player.phonograph.model.Song as OldSong
import player.phonograph.database.mediastore.Album as NewAlbum
import player.phonograph.model.Album as OldAlbum
import player.phonograph.database.mediastore.Artist as NewArtist
import player.phonograph.model.Artist as OldArtist

object ModelConverterHelper {

    @JvmStatic
    fun convertSong(songs: List<NewSong>): List<OldSong> {
        return List(songs.size) { index ->
            Converter.toSongModel(songs[index])
        }
    }

    @JvmStatic
    fun convertSongBack(songs: List<OldSong>): List<NewSong> {
        return List(songs.size) { index ->
            Converter.fromSongModel(songs[index])
        }
    }

    @JvmStatic
    fun convertAlbum(albums: List<NewAlbum>): List<OldAlbum> {
        return List(albums.size) { index ->
            Converter.toAlbumModel(albums[index])
        }
    }

    @JvmStatic
    fun convertAlbumBack(albums: List<OldAlbum>): List<NewAlbum> {
        return List(albums.size) { index ->
            Converter.fromAlbumModel(albums[index])
        }
    }


    @JvmStatic
    fun convertArtist(artists: List<NewArtist>): List<OldArtist> {
        return List(artists.size) { index ->
            Converter.toArtistModel(artists[index])
        }
    }

    @JvmStatic
    fun convertArtistBack(artists: List<OldArtist>): List<NewArtist> {
        return List(artists.size) { index ->
            Converter.fromArtistModel(artists[index])
        }
    }

}

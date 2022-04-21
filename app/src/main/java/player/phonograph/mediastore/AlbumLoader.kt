/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import java.util.*
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object AlbumLoader {

    fun getAllAlbums(context: Context): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, null, null, sortOrder)
        )
        return splitIntoAlbums(songs)
    }

    fun getAlbums(context: Context, query: String): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), sortOrder)
        )
        return splitIntoAlbums(songs)
    }

    @JvmStatic
    fun getAlbum(context: Context, albumId: Long): Album {
        val songs =
            getSongs(makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), sortOrder))
        val album = Album(songs)
        sortSongsByTrackNumber(album)
        return album
    }

    fun splitIntoAlbums(songs: List<Song>?): List<Album> {
        val albums: MutableList<Album> = ArrayList()
        if (songs != null) {
            for (song in songs) {
                getOrCreateAlbum(albums, song.albumId).songs.toMutableList().add(song)
            }
        }
        for (album in albums) {
            sortSongsByTrackNumber(album)
        }
        return albums
    }

    private fun getOrCreateAlbum(albums: MutableList<Album>, albumId: Long): Album {
        for (album in albums) {
            if (album.songs.isNotEmpty() && album.songs[0].albumId == albumId) {
                return album
            }
        }
        val album = Album()
        albums.add(album)
        return album
    }

    private fun sortSongsByTrackNumber(album: Album) {
        album.songs.toMutableList().sortWith { o1: Song, o2: Song -> o1.trackNumber - o2.trackNumber }
    }

    val sortOrder: String by lazy {
        "${Setting.instance.albumSortOrder}, ${Setting.instance.albumSongSortOrder}"
    }
}

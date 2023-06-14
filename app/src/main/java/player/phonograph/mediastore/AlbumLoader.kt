/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import player.phonograph.model.Album
import player.phonograph.model.Song
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object AlbumLoader {

    fun all(context: Context): List<Album> {
        val songs = querySongs(context, sortOrder = null).getSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

    fun getAlbums(context: Context, query: String): List<Album> {
        val songs = querySongs(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null).getSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

    fun id(context: Context, albumId: Long): Album {
        val songs = querySongs(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), null).getSongs()
        return Album(albumId, getAlbumTitle(songs), songs.toMutableList().sortedBy { it.trackNumber })
    }

    private fun getAlbumTitle(list: List<Song>): String? {
        if (list.isEmpty()) return null
        return list[0].albumName
    }

    fun List<Album>.allSongs(): List<Song> =
        this.flatMap { it.songs }
}

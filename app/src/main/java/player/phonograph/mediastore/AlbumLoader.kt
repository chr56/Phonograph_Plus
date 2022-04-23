/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.model.Album
import player.phonograph.model.Song

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object AlbumLoader {

    fun getAllAlbums(context: Context): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, null, null, null)
        )
        return if (songs.isNullOrEmpty()) return emptyList() else songs.toAlbumList()
    }

    fun getAlbums(context: Context, query: String): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null)
        )
        return if (songs.isNullOrEmpty()) return emptyList() else songs.toAlbumList()
    }

    fun getAlbum(context: Context, albumId: Long): Album {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), null)
        )
        return Album(albumId, getAlbumTitle(songs), songs.toMutableList().sortedBy { it.trackNumber })
    }

    private fun getAlbumTitle(list: List<Song>): String? {
        if (list.isEmpty()) return null
        return list[0].albumName
    }
}

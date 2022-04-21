/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import player.phonograph.helper.SortOrder
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
            makeSongCursor(context, null, null, null)
        )
        return splitIntoAlbums(songs)
    }

    fun getAlbums(context: Context, query: String): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null)
        )
        return splitIntoAlbums(songs)
    }

    fun getAlbum(context: Context, albumId: Long): Album {
        val songs =
            getSongs(makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), null))
        return Album(songs.toMutableList().sortedBy { it.trackNumber })
    }

    fun splitIntoAlbums(songs: List<Song>?): List<Album> {
        if (songs == null) return ArrayList()

        // AlbumID <-> List of song
        val idMap: MutableMap<Long, MutableList<Song>> = ArrayMap()
        for (song in songs) {
            if (idMap[song.albumId] == null) {
                // create new
                idMap[song.albumId] = ArrayList<Song>(1).apply { add(song) }
            } else {
                // add to existed
                idMap[song.albumId]!!.add(song)
            }
        }

        // map to list
        return idMap.map { entry ->
            // create album from songs
            Album(
                // list of song
                entry.value.apply {
                    sortBy { it.trackNumber } // sort songs before create album
                }
            )
        }.sort()
    }

    // todo
    private fun List<Album>.sort(): List<Album> {
        return when (Setting.instance.albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z -> this.sortedBy { it.title.lowercase() }
            SortOrder.AlbumSortOrder.ALBUM_Z_A -> this.sortedByDescending { it.title.lowercase() }
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> this.sortedBy { it.artistName.lowercase() }
            SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT -> this.sortedByDescending { it.artistName.lowercase() }
            SortOrder.AlbumSortOrder.ALBUM_YEAR -> this.sortedBy { it.year }
            SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT -> { this.sortedByDescending { it.year } }
            else -> { this }
        }
    }
}

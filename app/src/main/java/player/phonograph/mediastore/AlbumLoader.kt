/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import kotlin.collections.ArrayList
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

    fun getAlbum(context: Context, albumId: Long): Album {
        val songs =
            getSongs(makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), sortOrder))
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
        }
    }

    val sortOrder: String by lazy {
        "${Setting.instance.albumSortOrder}, ${Setting.instance.albumSongSortOrder}"
    }
}

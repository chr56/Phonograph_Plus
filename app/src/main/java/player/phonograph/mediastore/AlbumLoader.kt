/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.mediastore.sort.SortRef
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
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), null)
        )
        return Album(albumId, getAlbumTitle(songs), songs.toMutableList().sortedBy { it.trackNumber })
    }

    fun splitIntoAlbums(songs: List<Song>?): List<Album> {
        if (songs == null) return ArrayList()

        val albums = CoroutineScope(Dispatchers.Default).async {
            val innerCoroutineScope = CoroutineScope(Dispatchers.Default)

            val flow = songs.asFlow()
            var completed = false

            // AlbumID <-> List of song
            val table: MutableMap<Long, MutableList<Song>> = ArrayMap()

            // AlbumID <-> albumName
            val albumName: MutableMap<Long, String?> = ArrayMap()

            innerCoroutineScope.launch {
                delay(100)
                try {
                    flow.collect { song -> // check album
                        if (table[song.albumId] == null) {
                            // create new album
                            albumName[song.albumId] = song.albumName
                            table[song.albumId] = ArrayList<Song>(1).apply { add(song) }
                        } else {
                            // add to existed album
                            table[song.albumId]!!.add(song)
                        }
                    }
                } finally {
                    completed = true
                }
            }

            while (!completed) yield() // wait until result is ready

            // handle result
            // todo: use flow
            val result: MutableList<Album> = ArrayList(table.size)
            table.forEach { (albumId, songList) ->
                // create album & add
                result.add(
                    Album(albumId, albumName[albumId], songList)
                )
            }

            return@async result
        }

        return runBlocking {
            return@runBlocking albums.await().sortAll()
        }
    }

    private fun getAlbumTitle(list: List<Song>): String? {
        if (list.isEmpty()) return null
        return list[0].albumName
    }

    private fun List<Album>.sortAll(): List<Album> {
        val revert = Setting.instance.albumSortMode.revert
        return when (Setting.instance.albumSortMode.sortRef) {
            SortRef.ALBUM_NAME -> this.sort(revert) { it.title }
            SortRef.ARTIST_NAME -> this.sort(revert) { it.artistName }
            SortRef.YEAR -> this.sort(revert) { it.year }
            SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
            else -> this
        }
    }

    private inline fun List<Album>.sort(
        revert: Boolean,
        crossinline selector: (Album) -> Comparable<*>?,
    ): List<Album> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}

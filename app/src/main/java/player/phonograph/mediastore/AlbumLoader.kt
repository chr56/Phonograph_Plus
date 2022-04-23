/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import player.phonograph.mediastore.AlbumLoader.sortAll
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting

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

    fun List<Song>.toAlbumList(): List<Album> {
        val songs = this
        val albums = CoroutineScope(Dispatchers.Default).async {

            var completed = false

            val flow = flow {
                for (song in songs) emit(song)
            }.catch { e ->
                Log.e("Loader", e.message.orEmpty())
                ErrorNotification.postErrorNotification(e, "Fail to load albums")
            }

            // AlbumID <-> List of song
            val table: MutableMap<Long, MutableList<Song>> = ArrayMap()

            // AlbumID <-> albumName
            val albumNames: MutableMap<Long, String?> = ArrayMap()

            flow.onCompletion { completed = true }
                .collect { song ->
                    if (table[song.albumId] == null) {
                        // create new album
                        albumNames[song.albumId] = song.albumName
                        table[song.albumId] = ArrayList<Song>(1).apply { add(song) }
                    } else {
                        // add to existed album
                        table[song.albumId]!!.add(song)
                    }
                }

            while (!completed) yield() // wait until result is ready

            // handle result
            return@async flow {
                for ((id, list) in table) {
                    emit(Pair(id, list))
                }
            }.map { (id, list) ->
                Album(id, albumNames[id], list)
            }.catch { e ->
                Log.e("Loader", e.message.orEmpty())
                ErrorNotification.postErrorNotification(e, "Fail to load albums")
            }.toList()
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

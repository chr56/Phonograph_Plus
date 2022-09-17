/*
 * Copyright (c) 2022 chr_56
 */

/**
 *  A file containing tools and utils for model convert/transform
 *  @author chr_56
 */
package player.phonograph.mediastore

import android.util.ArrayMap
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import player.phonograph.model.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.SortOrderSettings

//
// Albums
//

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
        return@runBlocking albums.await().sortAllAlbums()
    }
}

fun List<Album>.sortAllAlbums(): List<Album> {
    val revert = SortOrderSettings.instance.albumSortMode.revert
    return when (SortOrderSettings.instance.albumSortMode.sortRef) {
        SortRef.ALBUM_NAME -> this.sort(revert) { it.title }
        SortRef.ARTIST_NAME -> this.sort(revert) { it.artistName }
        SortRef.YEAR -> this.sort(revert) { it.year }
        SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
        else -> this
    }
}

//
// Artists
//

fun List<Song>.toArtistList(): List<Artist> {
    val songs = this

    val artists = CoroutineScope(Dispatchers.Default).async {

        var completed = false

        val flow = flow {
            for (song in songs) emit(song)
        }.catch { e ->
            Log.e("Loader", e.message.orEmpty())
            ErrorNotification.postErrorNotification(e, "Fail to load albums")
        }

        // artistID <-> List of songs which are grouped by song
        val table: MutableMap<Long, MutableMap<Long, MutableList<Song>>> = ArrayMap()

        // artistID <-> artistName
        val artistNames: MutableMap<Long, String?> = ArrayMap()
        // albumID <-> albumName
        val albumNames: MutableMap<Long, String?> = ArrayMap()

        flow.onCompletion { completed = true }
            .collect { song -> // check artist
                if (table[song.artistId] == null) {
                    // create new artist
                    artistNames[song.artistId] = song.artistName
                    table[song.artistId] = ArrayMap()
                    // check album
                    if (table[song.artistId]!![song.albumId] == null) {
                        // create new album
                        albumNames[song.albumId] = song.albumName
                        table[song.artistId]!![song.albumId] = ArrayList<Song>(1).apply { add(song) }
                    } else {
                        // add to existed album
                        table[song.artistId]!![song.albumId]!!.add(song)
                    }
                    //
                } else {
                    // add to existed artist
                    // (no ops)
                    // check album
                    if (table[song.artistId]!![song.albumId] == null) {
                        // create new album
                        albumNames[song.albumId] = song.albumName
                        table[song.artistId]!![song.albumId] = ArrayList<Song>(1).apply { add(song) }
                    } else {
                        // add to existed album
                        table[song.artistId]!![song.albumId]!!.add(song)
                    }
                    //
                }
            }

        while (!completed) yield() // wait until result is ready

        // handle result
        return@async flow {
            for ((id, map) in table) {
                emit(Pair(id, map))
            }
        }.map { (artistId, map) ->
            val albumList = flow {
                for ((id, list) in map) {
                    emit(Pair(id, list))
                }
            }.map { (id, list) ->
                Album(id, albumNames[id], list)
            }.catch { e ->
                Log.e("Loader", e.message.orEmpty())
                ErrorNotification.postErrorNotification(e, "Fail to load albums")
            }.toList()

            Artist(artistId, artistNames[artistId], albumList)
        }.catch { e ->
            Log.e("Loader", e.message.orEmpty())
            ErrorNotification.postErrorNotification(e, "Fail to load albums")
        }.toList()
    }

    return runBlocking {
        return@runBlocking artists.await().sortAllArtist()
    }
}

fun List<Artist>.sortAllArtist(): List<Artist> {
    val revert = SortOrderSettings.instance.artistSortMode.revert
    return when (SortOrderSettings.instance.artistSortMode.sortRef) {
        SortRef.ARTIST_NAME -> this.sort(revert) { it.name.lowercase() }
        SortRef.ALBUM_COUNT -> this.sort(revert) { it.albumCount }
        SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
        else -> this
    }
}

//
// Misc
//

inline fun <T> List<T>.sort(
    revert: Boolean,
    crossinline selector: (T) -> Comparable<*>?,
): List<T> {
    return if (revert) this.sortedWith(compareByDescending(selector))
    else this.sortedWith(compareBy(selector))
}

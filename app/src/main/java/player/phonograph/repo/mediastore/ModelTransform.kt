/*
 *  Copyright (c) 2022~2023 chr_56
 */

/**
 *  A file containing tools and utils for model convert/transform
 *  @author chr_56
 */
package player.phonograph.repo.mediastore

import player.phonograph.App
import player.phonograph.model.Album
import player.phonograph.model.Album.Companion.UNKNOWN_ALBUM_DISPLAY_NAME
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.reportError
import android.util.ArrayMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

//
// Albums
//

fun createAlbum(id: Long, songs: List<Song>): Album {
    if (songs.isNotEmpty()) {
        val songLatest = songs.maxBy { it.dateModified }
        val songWithAlbumArtist = songs.firstOrNull { !it.albumArtistName.isNullOrEmpty() }
        val songWithArtist = songs.firstOrNull { !it.artistName.isNullOrEmpty() }
        val candidateSong = songWithAlbumArtist ?: songWithArtist ?: songLatest

        val title = songs.firstNotNullOfOrNull { it.albumName } ?: UNKNOWN_ALBUM_DISPLAY_NAME
        val artistId = candidateSong.artistId
        val artistName = candidateSong.artistName
        val year = songLatest.year
        val dateModified = songLatest.dateModified
        return Album(
            id = id,
            title = title,
            songCount = songs.size,
            artistId = artistId,
            artistName = artistName,
            year = year,
            dateModified = dateModified,
        )
    } else {
        return Album()
    }
}

fun List<Song>.toAlbumList(): List<Album> {
    val songs = this
    val albums = CoroutineScope(Dispatchers.Default).async {

        var completed = false

        val flow = flow {
            for (song in songs) emit(song)
        }.catch { e ->
            reportError(e, "Loader", "Fail to load albums")
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
                    table[song.albumId] = mutableListOf(song)
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
        }.flowOn(Dispatchers.Default).map { (id, list) ->
            createAlbum(id, list)
        }.catch { e ->
            reportError(e, "Loader", "Fail to load albums")
        }.toList()
    }

    return runBlocking {
        return@runBlocking albums.await().sortAllAlbums()
    }
}

fun List<Album>.sortAllAlbums(): List<Album> {
    val sortMode = Setting(App.instance).Composites[Keys.albumSortMode].data
    val revert = sortMode.revert
    return when (sortMode.sortRef) {
        SortRef.ALBUM_NAME  -> this.sort(revert) { it.title }
        SortRef.ARTIST_NAME -> this.sort(revert) { it.artistName }
        SortRef.YEAR        -> this.sort(revert) { it.year }
        SortRef.SONG_COUNT  -> this.sort(revert) { it.songCount }
        else                -> this
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
            reportError(e, TAG, "Fail to load artists")
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
                        table[song.artistId]!![song.albumId] = mutableListOf(song)
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
                        table[song.artistId]!![song.albumId] = mutableListOf(song)
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
        }.flowOn(Dispatchers.Default).map { (artistId, map) ->
            val albumList = flow {
                for ((id, list) in map) {
                    emit(Pair(id, list))
                }
            }.map { (id, list) ->
                createAlbum(id, list)
            }.catch { e ->
                reportError(e, TAG, "Fail to load artists")
            }.toList()

            Artist(artistId, artistNames[artistId] ?: Artist.UNKNOWN_ARTIST_DISPLAY_NAME, albumList.size, size)
        }.catch { e ->
            reportError(e, TAG, "Fail to load artists")
        }.toList()
    }

    return runBlocking {
        return@runBlocking artists.await().sortAllArtist()
    }
}

fun List<Artist>.sortAllArtist(): List<Artist> {
    val sortMode = Setting(App.instance).Composites[Keys.artistSortMode].data
    val revert = sortMode.revert
    return when (sortMode.sortRef) {
        SortRef.ARTIST_NAME -> this.sort(revert) { it.name.lowercase() }
        SortRef.ALBUM_COUNT -> this.sort(revert) { it.albumCount }
        SortRef.SONG_COUNT  -> this.sort(revert) { it.songCount }
        else                -> this
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

private const val TAG = "Loader"
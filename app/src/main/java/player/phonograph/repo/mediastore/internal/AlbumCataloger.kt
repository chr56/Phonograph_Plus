/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.foundation.error.warning
import player.phonograph.foundation.mediastore.mediastoreAlbumSortRefKey
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.sort
import android.content.Context
import android.util.ArrayMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.yield

fun createAlbum(id: Long, songs: List<Song>): Album {
    if (songs.isNotEmpty()) {
        val songLatest = songs.maxBy { it.dateModified }
        val songWithAlbumArtist = songs.firstOrNull { !it.albumArtistName.isNullOrEmpty() }
        val songWithArtist = songs.firstOrNull { !it.artistName.isNullOrEmpty() }
        val candidateSong = songWithAlbumArtist ?: songWithArtist ?: songLatest

        val title = songs.firstNotNullOfOrNull { it.albumName } ?: Album.UNKNOWN_ALBUM_DISPLAY_NAME
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

suspend fun generateArtistAlbums(context: Context, songs: List<Song>): List<Album> =
    catalogAlbums(context, songs, SortMode(SortRef.YEAR, false)).await()

suspend fun generateAlbums(context: Context, songs: List<Song>): List<Album> =
    catalogAlbums(context, songs, Setting(context)[Keys.albumSortMode].read()).await()

private suspend fun catalogAlbums(
    context: Context,
    songs: List<Song>,
    sortMode: SortMode,
): Deferred<List<Album>> = coroutineScope {
    async {

        var completed = false

        val flow = flow {
            for (song in songs) emit(song)
        }.catch { e ->
            warning(context, TAG_ALBUM, "Fail to load albums", e)
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
            warning(context, TAG_ALBUM, "Fail to load albums", e)
        }.toList().sortAllAlbums(sortMode)
    }
}

private fun List<Album>.sortAllAlbums(sortMode: SortMode): List<Album> =
    this.sort(sortMode.revert, mediastoreAlbumSortRefKey(sortMode.sortRef))

private const val TAG_ALBUM = "AlbumCataloger"
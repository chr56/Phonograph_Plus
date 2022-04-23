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
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ArtistLoader {

    fun getAllArtists(context: Context): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, null, null, null)
        )
        return splitIntoArtists(songs)
    }

    fun getArtists(context: Context, query: String): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), null)
        )
        return splitIntoArtists(songs)
    }

    fun getArtist(context: Context, artistId: Long): Artist {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null)
        )
        return if (songs.isEmpty()) Artist(artistId, Artist.UNKNOWN_ARTIST_DISPLAY_NAME, ArrayList())
        else Artist(artistId, songs[0].artistName, AlbumLoader.splitIntoAlbums(songs))
    }

    fun splitIntoArtists(songs: List<Song>): List<Artist> {
        if (songs.isEmpty()) return ArrayList()

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
            return@runBlocking artists.await().sortAll()
        }
    }

    private fun List<Artist>.sortAll(): List<Artist> {
        val revert = Setting.instance.artistSortMode.revert
        return when (Setting.instance.artistSortMode.sortRef) {
            SortRef.ARTIST_NAME -> this.sort(revert) { it.name.lowercase() }
            SortRef.ALBUM_COUNT -> this.sort(revert) { it.albumCount }
            SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
            else -> this
        }
    }

    private inline fun List<Artist>.sort(
        revert: Boolean,
        crossinline selector: (Artist) -> Comparable<*>?,
    ): List<Artist> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}

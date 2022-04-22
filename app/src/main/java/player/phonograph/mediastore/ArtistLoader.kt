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
import player.phonograph.model.Artist
import player.phonograph.model.Song
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

            val innerCoroutineScope = CoroutineScope(Dispatchers.Default)

            val flow = songs.asFlow()
            var completed = false

            // artistID <-> List of songs which are grouped by song
            val table: MutableMap<Long, MutableMap<Long, MutableList<Song>>> = ArrayMap()

            // artistID <-> artistName
            val artistName: MutableMap<Long, String?> = ArrayMap()
            // albumID <-> albumName
            val albumName: MutableMap<Long, String?> = ArrayMap()

            // process data
            innerCoroutineScope.launch {
                delay(100)
                try {
                    flow.collect { song ->
                        // check artist
                        if (table[song.artistId] == null) {
                            // create new artist
                            artistName[song.artistId] = song.artistName
                            table[song.artistId] = ArrayMap()
                            // check album
                            if (table[song.artistId]!![song.albumId] == null) {
                                // create new album
                                albumName[song.albumId] = song.albumName
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
                                albumName[song.albumId] = song.albumName
                                table[song.artistId]!![song.albumId] = ArrayList<Song>(1).apply { add(song) }
                            } else {
                                // add to existed album
                                table[song.artistId]!![song.albumId]!!.add(song)
                            }
                            //
                        }
                    }
                } finally {
                    completed = true
                }
            }

            while (!completed) yield() // wait until result is ready

            // handle result
            // todo: use flow
            val result: MutableList<Artist> = ArrayList(table.size)
            table.forEach { (artistId, albumMap) ->
                // create album
                val albums = albumMap
                    .map { (albumId, songList) ->
                        Album(albumId, albumName[albumId], songList)
                    }

                // create artist & add
                result.add(
                    Artist(
                        artistId, artistName[artistId], albums
                    )
                )
            }

            return@async result
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
        crossinline selector: (Artist) -> Comparable<*>?
    ): List<Artist> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}

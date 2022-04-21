/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
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
        return Artist(AlbumLoader.splitIntoAlbums(songs))
    }

    fun splitIntoArtists(songs: List<Song>): List<Artist> {
        if (songs.isEmpty()) return ArrayList()

        // split artists

        // artistID <-> List of song
        val idMap: MutableMap<Long, MutableList<Song>> = ArrayMap()
        for (song in songs) {
            if (idMap[song.artistId] == null) {
                // create new
                idMap[song.artistId] = ArrayList<Song>(1).apply { add(song) }
            } else {
                // add to existed
                idMap[song.artistId]!!.add(song)
            }
        }

        // to albums
        // list of every artists' list of albums
        val artistAlbumsList: List<List<Album>> = idMap.values.map { artistSongs ->
            AlbumLoader.splitIntoAlbums(artistSongs)
        }

        return artistAlbumsList.map { Artist(it) }.sortAll()
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

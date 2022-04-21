/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
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
            makeSongCursor(context, null, null, sortOrder)
        )
        return splitIntoArtists(songs)
    }

    fun getArtists(context: Context, query: String): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), sortOrder)
        )
        return splitIntoArtists(songs)
    }

    fun getArtist(context: Context, artistId: Long): Artist {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), sortOrder)
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

        return artistAlbumsList.map { Artist(it) }
    }

    val sortOrder: String by lazy {
        "${Setting.instance.artistSortOrder}, ${Setting.instance.artistAlbumSortOrder}, ${Setting.instance.albumSongSortOrder}"
    }
}

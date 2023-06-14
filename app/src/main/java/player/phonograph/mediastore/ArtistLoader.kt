/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import player.phonograph.model.Artist
import player.phonograph.model.Song
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ArtistLoader {

    fun all(context: Context): List<Artist> {
        val songs = querySongs(context, sortOrder = null).getSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toArtistList()
    }

    fun getArtists(context: Context, query: String): List<Artist> {
        val songs = querySongs(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), null).getSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toArtistList()
    }

    fun id(context: Context, artistId: Long): Artist {
        val songs = querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null).getSongs()
        return if (songs.isEmpty()) Artist(artistId, Artist.UNKNOWN_ARTIST_DISPLAY_NAME, ArrayList())
        else Artist(artistId, songs[0].artistName, songs.toAlbumList())
    }

    fun List<Artist>.allArtistSongs(): List<Song> =
        this.flatMap { it.songs }
}

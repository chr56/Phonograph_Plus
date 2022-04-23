/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.model.Artist

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ArtistLoader {

    fun getAllArtists(context: Context): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, null, null, null)
        )
        return if (songs.isNullOrEmpty()) return emptyList() else songs.toArtistList()
    }

    fun getArtists(context: Context, query: String): List<Artist> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), null)
        )
        return if (songs.isNullOrEmpty()) return emptyList() else songs.toArtistList()
    }

    fun getArtist(context: Context, artistId: Long): Artist {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null)
        )
        return if (songs.isEmpty()) Artist(artistId, Artist.UNKNOWN_ARTIST_DISPLAY_NAME, ArrayList())
        else Artist(artistId, songs[0].artistName, songs.toAlbumList())
    }
}

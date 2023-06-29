/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import player.phonograph.repo.mediastore.toAlbumList
import player.phonograph.repo.mediastore.toArtistList
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ArtistLoader {

    fun all(context: Context): List<Artist> {
        val songs = querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toArtistList()
    }

    fun searchByName(context: Context, query: String): List<Artist> {
        val songs = querySongs(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toArtistList()
    }

    fun id(context: Context, artistId: Long): Artist {
        val songs = querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null).intoSongs()
        return if (songs.isEmpty()) Artist(artistId, Artist.UNKNOWN_ARTIST_DISPLAY_NAME, ArrayList())
        else Artist(artistId, songs[0].artistName, songs.toAlbumList())
    }

    fun List<Artist>.allArtistSongs(): List<Song> =
        this.flatMap { it.songs }
}

/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.model.Album
import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.mediastore.internal.createAlbum
import player.phonograph.repo.mediastore.internal.generateAlbums
import player.phonograph.repo.mediastore.internal.generateArtistAlbums
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object MediaStoreAlbums : IAlbums {

    override suspend fun all(context: Context): List<Album> {
        val songs = querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else generateAlbums(context, songs)
    }

    override suspend fun id(context: Context, id: Long): Album {
        val songs = MediaStoreSongs.album(context, id)
        return createAlbum(id, songs)
    }

    override suspend fun searchByName(context: Context, query: String): List<Album> {
        val songs = querySongs(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else generateAlbums(context, songs)
    }

    override suspend fun artist(context: Context, artistId: Long): List<Album> =
        querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null)
            .intoSongs().let { generateArtistAlbums(context, it) }

}
/*
 *  Copyright (c) 2022~2023 chr_56 & Karim Abou Zeid (kabouzeid)
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.catalogAlbums
import player.phonograph.repo.mediastore.internal.createAlbum
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object AlbumLoader : Loader<Album> {

    override suspend fun all(context: Context): List<Album> {
        val songs = querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

    override suspend fun id(context: Context, id: Long): Album {
        val songs = AlbumSongLoader.id(context, id)
        return createAlbum(id, songs)
    }

    suspend fun searchByName(context: Context, query: String): List<Album> {
        val songs = querySongs(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

    private suspend fun List<Song>.toAlbumList(): List<Album> = catalogAlbums(this@toAlbumList).await()
}


/*
 *  Copyright (c) 2022~2023 chr_56 & Karim Abou Zeid (kabouzeid)
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Album
import player.phonograph.repo.mediastore.createAlbum
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import player.phonograph.repo.mediastore.toAlbumList
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object AlbumLoader : Loader<Album> {

    override fun all(context: Context): List<Album> {
        val songs = querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

    override fun id(context: Context, id: Long): Album {
        val songs = AlbumSongLoader.id(context, id)
        return createAlbum(id, songs)
    }

    fun searchByName(context: Context, query: String): List<Album> {
        val songs = querySongs(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null).intoSongs()
        return if (songs.isEmpty()) return emptyList() else songs.toAlbumList()
    }

}

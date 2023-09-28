/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.catalogAlbums
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader
import android.content.Context
import kotlinx.coroutines.Deferred

object Albums {

    fun all(context: Context): List<Album> = AlbumLoader.all(context)

    fun id(context: Context, id: Long): Album = AlbumLoader.id(context, id)
    fun searchByName(context: Context, query: String) = AlbumLoader.searchByName(context, query)

    fun songs(context: Context, albumId: Long): List<Song> = AlbumSongLoader.id(context, albumId)

    private suspend fun List<Song>.toAlbumList(): Deferred<List<Album>> = catalogAlbums(this)
}
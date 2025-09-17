/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Album
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.mediastore.MediaStoreAlbums
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing albums
 */
object Albums : IAlbums, Delegated<IAlbums>() {
    override fun onCreateDelegate(context: Context): IAlbums {
        val preference = Setting(context)[Keys.musicLibraryBackend]
        val impl: IAlbums = when (preference.data) {
            else                       -> MediaStoreAlbums
        }
        return impl
    }

    override suspend fun all(context: Context): List<Album> =
        delegate(context).all(context)

    override suspend fun id(context: Context, id: Long): Album =
        delegate(context).id(context, id)

    override suspend fun searchByName(context: Context, query: String): List<Album> =
        delegate(context).searchByName(context, query)

    override suspend fun artist(context: Context, artistId: Long): List<Album> =
        delegate(context).artist(context, artistId)

}
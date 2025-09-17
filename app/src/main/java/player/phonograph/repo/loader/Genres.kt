/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing genres
 */
object Genres : IGenres, Delegated<IGenres>() {
    override fun onCreateDelegate(context: Context): IGenres {
        val preference = Setting(context)[Keys.musicLibraryBackend]
        val impl: IGenres = when (preference.data) {
            else                       -> MediaStoreGenres
        }
        return impl
    }

    override suspend fun all(context: Context): List<Genre> =
        delegate(context).all(context)

    override suspend fun id(context: Context, id: Long): Genre? =
        delegate(context).id(context, id)

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        MediaStoreGenres.songs(context, genreId) // todo

}
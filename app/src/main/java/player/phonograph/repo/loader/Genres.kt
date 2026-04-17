/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.room.domain.RoomGenres
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing genres
 */
object Genres : IGenres, Delegated<IGenres>() {
    override fun onCreateDelegate(context: Context): IGenres {
        val backend = Setting(context)[Keys.musicLibraryBackend].data
        val impl: IGenres = if (backend.useMediaStoreGenres) MediaStoreGenres else RoomGenres
        return impl
    }

    override suspend fun all(context: Context): List<Genre> =
        delegate(context).all(context)

    override suspend fun id(context: Context, id: Long): Genre? =
        delegate(context).id(context, id)

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        delegate(context).songs(context, genreId)

    override suspend fun of(context: Context, songId: Long): List<Genre> =
        delegate(context).of(context, songId)

    override suspend fun searchByName(context: Context, query: String): List<Genre> =
        delegate(context).searchByName(context, query)
}
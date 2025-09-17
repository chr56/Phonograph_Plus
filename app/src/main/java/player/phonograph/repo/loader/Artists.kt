/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Artist
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_PARSED
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.repo.mediastore.MediaStoreArtists
import player.phonograph.repo.room.domain.RoomArtists
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing artists
 */
object Artists : IArtists, Delegated<IArtists>() {
    override fun onCreateDelegate(context: Context): IArtists {
        val preference = Setting(context)[Keys.musicLibraryBackend]
        val impl: IArtists = when (preference.data) {
            PROVIDER_MEDIASTORE_PARSED -> RoomArtists
            else                       -> MediaStoreArtists
        }
        return impl
    }


    override suspend fun all(context: Context): List<Artist> =
        delegate(context).all(context)

    override suspend fun id(context: Context, id: Long): Artist =
        delegate(context).id(context, id)

    override suspend fun searchByName(context: Context, query: String): List<Artist> =
        delegate(context).searchByName(context, query)
}
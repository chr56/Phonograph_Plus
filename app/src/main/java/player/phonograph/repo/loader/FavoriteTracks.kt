/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IFavoriteTracks
import player.phonograph.repo.mediastore.PlaylistFavoriteTracks
import player.phonograph.repo.room.domain.RoomFavoriteTracks
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import android.content.Context

/**
 * Endpoint for accessing favorite songs
 */
object FavoriteTracks : IFavoriteTracks, Delegated<IFavoriteTracks>() {

    override fun onCreateDelegate(context: Context): IFavoriteTracks {
        val preference = Settings(context)[Keys.useLegacyFavoritePlaylistImpl]
        val impl: IFavoriteTracks = if (preference.data) PlaylistFavoriteTracks() else RoomFavoriteTracks
        return impl
    }

    override suspend fun all(context: Context): List<Song> =
        delegate(context).all(context)

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        delegate(context).isFavorite(context, song)

    override suspend fun add(context: Context, song: Song): Boolean =
        delegate(context).add(context, song)

    override suspend fun add(context: Context, songs: List<Song>): Boolean =
        delegate(context).add(context, songs)

    override suspend fun remove(context: Context, song: Song): Boolean =
        delegate(context).remove(context, song)

    override suspend fun toggleState(context: Context, song: Song): Boolean =
        delegate(context).toggleState(context, song)

    override suspend fun cleanMissing(context: Context): Boolean =
        delegate(context).cleanMissing(context)

    override suspend fun clearAll(context: Context): Boolean =
        delegate(context).clearAll(context)

}
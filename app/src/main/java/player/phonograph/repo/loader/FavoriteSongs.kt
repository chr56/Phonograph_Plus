/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.repo.mediastore.PlaylistFavoriteSongs
import player.phonograph.repo.room.domain.RoomFavoriteSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing favorite songs
 */
object FavoriteSongs : IFavoriteSongs, Delegated<IFavoriteSongs>() {

    override fun onCreateDelegate(context: Context): IFavoriteSongs {
        val preference = Setting(context)[Keys.useLegacyFavoritePlaylistImpl]
        val impl: IFavoriteSongs = if (preference.data) PlaylistFavoriteSongs() else RoomFavoriteSongs
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
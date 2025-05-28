/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.repo.database.loaders.DatabaseFavoriteSongs
import player.phonograph.repo.mediastore.loaders.PlaylistFavoriteSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

object RouterFavoriteSongs : IFavoriteSongs {

    @Volatile
    private var _implement: IFavoriteSongs? = null
    private fun implement(context: Context): IFavoriteSongs =
        _implement ?: synchronized(this) {
            _implement ?: run { // double check
                val preference = Setting(context)[Keys.useLegacyFavoritePlaylistImpl]
                val impl: IFavoriteSongs =
                    if (preference.data) PlaylistFavoriteSongs() else DatabaseFavoriteSongs()
                _implement = impl
                impl
            }
        }

    override suspend fun allSongs(context: Context): List<Song> =
        implement(context).allSongs(context)

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        implement(context).isFavorite(context, song)

    override suspend fun addToFavorites(context: Context, song: Song): Boolean =
        implement(context).addToFavorites(context, song)

    override suspend fun removeFromFavorites(context: Context, song: Song): Boolean =
        implement(context).removeFromFavorites(context, song)

    override suspend fun toggleFavorite(context: Context, song: Song): Boolean =
        implement(context).toggleFavorite(context, song)

    override suspend fun cleanMissing(context: Context): Boolean =
        implement(context).cleanMissing(context)

    override suspend fun clearAll(context: Context): Boolean =
        implement(context).clearAll(context)

}
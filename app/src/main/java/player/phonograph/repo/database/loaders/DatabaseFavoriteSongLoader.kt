/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import player.phonograph.model.Song
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.loader.IFavoriteSongs
import android.content.Context

class DatabaseFavoriteSongLoader : IFavoriteSongs {

    private val favoritesStore: FavoritesStore = FavoritesStore.get()

    override suspend fun allSongs(context: Context): List<Song> =
        favoritesStore.getAllSongs(context)

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        favoritesStore.containsSong(song.id, song.data)

    override suspend fun toggleFavorite(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) {
            !favoritesStore.removeSong(song)
        } else {
            favoritesStore.addSong(song)
        }

    override suspend fun cleanMissed(context: Context): Boolean {
        return favoritesStore.cleanMissingSongs(context)
    }

    override suspend fun clearAll(context: Context): Boolean {
        favoritesStore.clearAll()
        return true
    }
}
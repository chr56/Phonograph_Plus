/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import lib.storage.textparser.ExternalFilePathParser
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.loader.Songs
import android.content.Context

class DatabaseFavoriteSongLoader : IFavoriteSongs {

    private val favoritesStore: FavoritesStore = FavoritesStore.get()

    override suspend fun allSongs(context: Context): List<Song> =
        favoritesStore.getAllSongs { _, path, _, _ -> lookupSong(context, path) }

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        favoritesStore.containsSong(song.id, song.data)

    override suspend fun addToFavorites(context: Context, song: Song): Boolean =
        favoritesStore.addSong(song)

    override suspend fun removeFromFavorites(context: Context, song: Song): Boolean =
        favoritesStore.removeSong(song)

    override suspend fun toggleFavorite(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) {
            !removeFromFavorites(context, song)
        } else {
            addToFavorites(context, song)
        }

    override suspend fun cleanMissing(context: Context): Boolean {
        return favoritesStore.cleanMissingSongs { id, path -> checkSongExistence(context, id, path) }
    }

    override suspend fun clearAll(context: Context): Boolean {
        favoritesStore.clearAllSongs()
        return true
    }

    companion object {
        private suspend fun lookupSong(context: Context, path: String): Song {
            val song = Songs.path(context, path)
            return if (song == null) {
                val filename = ExternalFilePathParser.bashPath(path) ?: context.getString(R.string.state_deleted)
                Song.deleted(filename, path)
            } else {
                song
            }
        }

        private suspend fun checkSongExistence(context: Context, id: Long, path: String): Boolean =
            Songs.path(context, path) == null || Songs.id(context, id) == null
    }

}
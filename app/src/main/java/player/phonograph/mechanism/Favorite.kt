/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.mechanism

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import player.phonograph.settings.Setting
import util.phonograph.playlist.mediastore.addToPlaylistViaMediastore
import util.phonograph.playlist.mediastore.createOrFindPlaylistViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import android.content.Context
import kotlinx.coroutines.runBlocking

object Favorite {

    fun isFavorite(context: Context, song: Song): Boolean =
        if (Setting.instance.useLegacyFavoritePlaylistImpl) {
            isFavoriteLegacyImpl(context, song)
        } else {
            isFavoriteDatabaseImpl(song)
        }

    private fun isFavoriteLegacyImpl(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null)
            PlaylistSongLoader.doesPlaylistContain(context, favoritesPlaylist.id, song.id)
        else
            false
    }

    private fun isFavoriteDatabaseImpl(song: Song): Boolean =
        FavoritesStore.instance.containsSong(song.id, song.data)

    /**
     * @return new state
     */
    fun toggleFavorite(context: Context, song: Song): Boolean {
        return if (Setting.instance.useLegacyFavoritePlaylistImpl) {
            runBlocking {
                toggleFavoriteLegacyImpl(context, song)
            }
        } else {
            toggleFavoriteDatabaseImpl(context, song)
        }.also {
            notifyMediaStoreChanged()
        }
    }

    /**
     * @return new state
     */
    private suspend fun toggleFavoriteLegacyImpl(context: Context, song: Song): Boolean {
        return if (isFavorite(context, song)) {
            val favoritesPlaylist = getFavoritesPlaylist(context)
            if (favoritesPlaylist != null)
                removeFromPlaylistViaMediastore(context, song, favoritesPlaylist.id)
            false
        } else {
            addToPlaylistViaMediastore(context, song, getOrCreateFavoritesPlaylist(context).id, false)
            true
        }
    }

    /**
     * @return new state
     */
    private fun toggleFavoriteDatabaseImpl(context: Context, song: Song): Boolean {
        return if (isFavorite(context, song)) {
            !FavoritesStore.instance.removeSong(song)
        } else {
            FavoritesStore.instance.addSong(song)
        }
    }

    @Deprecated(
        "use DatabaseImpl",
        ReplaceWith(
            "playlist.name != null && playlist.name == context.getString(R.string.favorites)",
            "player.phonograph.R"
        )
    )
    fun isFavoritePlaylist(context: Context, playlist: Playlist): Boolean {
        return playlist.name == context.getString(R.string.favorites)
    }

    private fun getFavoritesPlaylist(context: Context): FilePlaylist? {
        return PlaylistLoader.playlistName(context, context.getString(R.string.favorites)).takeIf { it.id > 0 }
    }

    private suspend fun getOrCreateFavoritesPlaylist(context: Context): Playlist {
        return PlaylistLoader.id(
            context,
            createOrFindPlaylistViaMediastore(context, context.getString(R.string.favorites))
        ).also { notifyMediaStoreChanged() }
    }

    private fun notifyMediaStoreChanged() = GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()
}

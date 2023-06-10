/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.mechanism

import player.phonograph.R
import player.phonograph.mechanism.PlaylistsManagement.doesPlaylistContain
import player.phonograph.mechanism.PlaylistsManagement.getPlaylist
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.provider.FavoritesStore
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

    private fun isFavoriteLegacyImpl(context: Context, song: Song): Boolean =
        doesPlaylistContain(context, getFavoritesPlaylist(context).id, song.id)

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
            removeFromPlaylistViaMediastore(context, song, getFavoritesPlaylist(context).id)
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
        return playlist.name != null && playlist.name == context.getString(R.string.favorites)
    }

    private fun getFavoritesPlaylist(context: Context): Playlist {
        return getPlaylist(context, context.getString(R.string.favorites))
    }

    private suspend fun getOrCreateFavoritesPlaylist(context: Context): Playlist {
        return getPlaylist(
            context,
            createOrFindPlaylistViaMediastore(context, context.getString(R.string.favorites))
        ).also { notifyMediaStoreChanged() }
    }

    private fun notifyMediaStoreChanged() = MediaStoreTracker.notifyAllListeners()
}

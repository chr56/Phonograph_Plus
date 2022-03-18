/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.util

import android.content.Context
import android.content.Intent
import legacy.phonograph.LegacyPlaylistsUtil.addToPlaylist
import legacy.phonograph.LegacyPlaylistsUtil.createPlaylist
import legacy.phonograph.LegacyPlaylistsUtil.removeFromPlaylist
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.provider.FavoriteSongsStore
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting
import player.phonograph.util.PlaylistsUtil.doesPlaylistContain
import player.phonograph.util.PlaylistsUtil.getPlaylist

object FavoriteUtil {

    @JvmStatic
    fun isFavorite(context: Context, song: Song): Boolean =
        if (Setting.instance.useLegacyFavoritePlaylistImpl) {
            isFavoriteLegacyImpl(context, song)
        } else {
            isFavoriteDatabaseImpl(song)
        }

    private fun isFavoriteLegacyImpl(context: Context, song: Song): Boolean =
        doesPlaylistContain(context, getFavoritesPlaylist(context).id, song.id)

    private fun isFavoriteDatabaseImpl(song: Song): Boolean =
        FavoriteSongsStore.instance.contains(song)

    @JvmStatic
    fun toggleFavorite(context: Context, song: Song) {
        if (Setting.instance.useLegacyFavoritePlaylistImpl) {
            toggleFavoriteLegacyImpl(context, song)
        } else {
            toggleFavoriteDatabaseImpl(context, song)
        }
        notifyMediaStoreChanged()
    }

    private fun toggleFavoriteLegacyImpl(context: Context, song: Song) {
        if (isFavorite(context, song)) {
            removeFromPlaylist(context, song, getFavoritesPlaylist(context).id)
        } else {
            addToPlaylist(context, song, getOrCreateFavoritesPlaylist(context).id, false)
        }
    }
    private fun toggleFavoriteDatabaseImpl(context: Context, song: Song) {
        if (isFavorite(context, song)) {
            FavoriteSongsStore.instance.remove(song)
        } else {
            FavoriteSongsStore.instance.add(song)
        }
    }

    @Deprecated(
        "use DatabaseImpl", ReplaceWith("playlist.name != null && playlist.name == context.getString(R.string.favorites)", "player.phonograph.R")
    )
    fun isFavoritePlaylist(context: Context, playlist: Playlist): Boolean {
        return playlist.name != null && playlist.name == context.getString(R.string.favorites)
    }

    private fun getFavoritesPlaylist(context: Context): Playlist {
        return getPlaylist(context, context.getString(R.string.favorites))
    }
    private fun getOrCreateFavoritesPlaylist(context: Context): Playlist {
        return getPlaylist(context, createPlaylist(context, context.getString(R.string.favorites))).also { notifyMediaStoreChanged() }
    }
    private fun notifyMediaStoreChanged() { App.instance.sendBroadcast(Intent(MusicService.MEDIA_STORE_CHANGED)) }

}

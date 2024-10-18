/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.room.domain.RoomPlaylists
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.sort
import android.content.Context

/**
 * Forward action or mix results for both [MediaStorePlaylists] and [RoomPlaylists]
 */
object RouterPlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> =
        sorted(context, RoomPlaylists.all(context) + MediaStorePlaylists.all(context))

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        when (location) {
            is DatabasePlaylistLocation -> RoomPlaylists.of(context, location)
            is FilePlaylistLocation     -> MediaStorePlaylists.of(context, location)
            else                        -> null
        }

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> =
        when (location) {
            is DatabasePlaylistLocation -> RoomPlaylists.songs(context, location)
            is FilePlaylistLocation     -> MediaStorePlaylists.songs(context, location)
            else                        -> emptyList()
        }

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean =
        when (location) {
            is DatabasePlaylistLocation -> RoomPlaylists.contains(context, location, songId)
            is FilePlaylistLocation     -> MediaStorePlaylists.contains(context, location, songId)
            else                        -> false
        }

    override suspend fun named(context: Context, name: String): Playlist? =
        RoomPlaylists.named(context, name) ?: MediaStorePlaylists.named(context, name)

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        when (location) {
            is DatabasePlaylistLocation -> RoomPlaylists.exists(context, location)
            is FilePlaylistLocation     -> MediaStorePlaylists.exists(context, location)
            is VirtualPlaylistLocation  -> true
            else                        -> false
        }

    override suspend fun searchByName(context: Context, query: String): List<Playlist> =
        RoomPlaylists.searchByName(context, query) + MediaStorePlaylists.searchByName(context, query)

    private suspend fun sorted(context: Context, playlists: List<Playlist>): List<Playlist> {
        val sortMode = Setting(context).Composites[Keys.playlistSortMode].flowData()
        val revert = sortMode.revert
        return when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME  -> playlists.sort(revert) { it.name }
            SortRef.PATH          -> playlists.sort(revert) { it.location }
            SortRef.ADDED_DATE    -> playlists.sort(revert) { it.dateAdded }
            SortRef.MODIFIED_DATE -> playlists.sort(revert) { it.dateModified }
            else                  -> playlists
        }
    }
}
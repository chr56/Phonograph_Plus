/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.repo.loader.IPlaylists
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import android.content.Context

object MediaStorePlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> = PlaylistLoader.all(context)

    override suspend fun id(context: Context, id: Long): Playlist? = PlaylistLoader.id(context, id)

    override suspend fun songs(context: Context, playlistId: Long): List<PlaylistSong> =
        PlaylistSongLoader.songs(context, playlistId)

    override suspend fun contains(context: Context, volume: String, playlistId: Long, songId: Long): Boolean =
        PlaylistSongLoader.contains(context, volume, playlistId, songId)

    override suspend fun playlistName(context: Context, playlistName: String): Playlist? =
        PlaylistLoader.playlistName(context, playlistName)

    override suspend fun searchByPath(context: Context, path: String): Playlist? =
        PlaylistLoader.searchByPath(context, path)

    override suspend fun searchByName(context: Context, name: String): List<Playlist> =
        PlaylistLoader.searchByName(context, name)

    override suspend fun checkExistence(context: Context, name: String): Boolean =
        PlaylistLoader.checkExistence(context, name)

    override suspend fun checkExistence(context: Context, location: PlaylistLocation): Boolean =
        PlaylistLoader.checkExistence(context, (location as FilePlaylistLocation).mediastoreId)
}
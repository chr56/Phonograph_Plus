/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import android.content.Context

object MediaStorePlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> = PlaylistLoader.all(context)

    suspend fun id(context: Context, id: Long): Playlist? = PlaylistLoader.id(context, id)

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        PlaylistLoader.id(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> =
        PlaylistSongLoader.songs(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean =
        PlaylistSongLoader.contains(context, location, songId)

    override suspend fun named(context: Context, name: String): Playlist? =
        PlaylistLoader.playlistName(context, name)

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        PlaylistLoader.checkExistence(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun searchByName(context: Context, query: String): List<Playlist> =
        PlaylistLoader.searchByName(context, query)

    fun searchByPath(context: Context, path: String): Playlist? =
        PlaylistLoader.searchByPath(context, path)


    fun songs(context: Context, id: Long): List<PlaylistSong> =
        PlaylistSongLoader.songs(context, id)
}
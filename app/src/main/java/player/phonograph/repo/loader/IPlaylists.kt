/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import android.content.Context

interface IPlaylists {

    suspend fun all(context: Context): List<Playlist>

    suspend fun of(context: Context, location: PlaylistLocation): Playlist?

    suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong>

    suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean

    suspend fun named(context: Context, name: String): Playlist?

    suspend fun exists(context: Context, location: PlaylistLocation): Boolean

    suspend fun searchByName(context: Context, query: String): List<Playlist>

}
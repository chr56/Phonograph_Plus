/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.Playlist
import android.content.Context

interface IPlaylists {

    suspend fun all(context: Context): List<Playlist>

    suspend fun id(context: Context, id: Long): Playlist?

    suspend fun songs(context: Context, playlistId: Long): List<PlaylistSong>

    suspend fun contains(context: Context, volume: String, playlistId: Long, songId: Long): Boolean

    suspend fun playlistName(context: Context, playlistName: String): Playlist?

    suspend fun searchByPath(context: Context, path: String): Playlist?

    suspend fun searchByName(context: Context, name: String): List<Playlist>

    suspend fun checkExistence(context: Context, name: String): Boolean

    suspend fun checkExistence(context: Context, playlistId: Long): Boolean

}
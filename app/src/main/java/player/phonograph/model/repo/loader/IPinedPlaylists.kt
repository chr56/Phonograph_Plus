/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.playlist.Playlist
import android.content.Context

interface IPinedPlaylists {

    suspend fun all(context: Context): List<Playlist>

    suspend fun isPined(context: Context, id: Long?, path: String?): Boolean

    suspend fun isPined(context: Context, playlist: Playlist): Boolean

    suspend fun add(context: Context, playlist: Playlist): Boolean

    suspend fun add(context: Context, playlists: List<Playlist>): Boolean

    suspend fun remove(context: Context, playlist: Playlist): Boolean

    /**
     * @return new state
     */
    suspend fun toggleState(context: Context, playlist: Playlist): Boolean

    suspend fun clearAll(context: Context): Boolean

}
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Artist
import player.phonograph.model.sort.SortMode
import android.content.Context

interface IArtists : Endpoint {

    suspend fun all(context: Context): List<Artist>

    suspend fun all(context: Context, sortMode: SortMode): List<Artist>

    /**
     * @return artists who are the album artist of at least one album
     * (falling back to the track artist for albums without an album artist tag)
     */
    suspend fun allAlbumArtists(context: Context, sortMode: SortMode): List<Artist>

    suspend fun id(context: Context, id: Long): Artist

    suspend fun searchByName(context: Context, query: String): List<Artist>

}
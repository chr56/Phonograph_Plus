/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Artist
import android.content.Context

interface IArtists : Endpoint {

    suspend fun all(context: Context): List<Artist>

    suspend fun id(context: Context, id: Long): Artist

    suspend fun searchByName(context: Context, query: String): List<Artist>

}
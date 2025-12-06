/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Album
import android.content.Context

interface IAlbums : Endpoint {

    suspend fun all(context: Context): List<Album>

    suspend fun id(context: Context, id: Long): Album

    suspend fun searchByName(context: Context, query: String): List<Album>

    suspend fun artist(context: Context, artistId: Long): List<Album>

}
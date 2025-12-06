/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Song
import android.content.Context

interface ISongs : Endpoint {

    suspend fun all(context: Context): List<Song>

    suspend fun id(context: Context, id: Long): Song?

    suspend fun path(context: Context, path: String): Song?

    suspend fun artist(context: Context, artistId: Long): List<Song>

    suspend fun album(context: Context, albumId: Long): List<Song>

    /**
     * @param withoutPathFilter true if disable path filter
     */
    suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song>

    suspend fun searchByTitle(context: Context, title: String): List<Song>

    suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean = false): List<Song>

}
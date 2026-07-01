/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Song
import android.content.Context

interface ISongs : Endpoint {

    /** All songs. */
    suspend fun all(context: Context): List<Song>

    /** Look up a song by its ID. */
    suspend fun id(context: Context, id: Long): Song?

    /**
     * Look up a song by its path.
     * @param withoutPathFilter true if disable path filter
     * @see searchByPath
     */
    suspend fun path(context: Context, path: String, withoutPathFilter: Boolean = false): Song?

    /** All songs by the given artist. */
    suspend fun artist(context: Context, artistId: Long): List<Song>

    /** All songs in the given album. */
    suspend fun album(context: Context, albumId: Long): List<Song>

    /**
     * Substring search by path. Implementations are responsible for any wild-carding.
     * @param withoutPathFilter true if disable path filter
     */
    suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song>

    /**
     * Substring search by title. Implementations are responsible for any wild-carding.
     */
    suspend fun searchByTitle(context: Context, title: String): List<Song>

    /** Songs added or modified since the given timestamp. */
    suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean = false): List<Song>

    /** Most recently added song. */
    suspend fun lastest(context: Context): Song?

    /** Total number of songs in the library. */
    suspend fun total(context: Context): Int
}
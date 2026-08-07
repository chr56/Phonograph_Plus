/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.repo.sync

import player.phonograph.model.Genre
import player.phonograph.model.Song

/**
 * Data source used for syncing music library.
 * This is the unified interface to access data, as a facade or an intermediate agent.
 */
interface DataSource {

    /**
     * Access all songs.
     */
    suspend fun songs(): List<Song>

    /**
     * Access new songs after cut-off [timestamp].
     */
    suspend fun songs(timestamp: Long): List<Song>

    /**
     * Access total size of songs.
     */
    suspend fun songCount(): Int

    /**
     * Access ids for all songs.
     */
    suspend fun songIds(): Set<Long>

    /**
     * Access genres to songs by their id.
     */
    suspend fun songGenres(songIds: Collection<Long>): Map<Long, List<Genre>>
}
/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.sync.DataSource
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.mediastore.MediaStoreSongs
import android.content.Context

/**
 * [DataSource] implementation for MediaStore
 */
class MediaStoreDataSource(private val context: Context) : DataSource {
    override suspend fun songs(): List<Song> = MediaStoreSongs.all(context)
    override suspend fun songs(timestamp: Long): List<Song> = MediaStoreSongs.since(context, timestamp, useModifiedDate = true)
    override suspend fun songCount(): Int = MediaStoreSongs.total(context)
    override suspend fun songIds(): Set<Long> = MediaStoreSongs.ids(context)
    override suspend fun songGenres(songIds: Collection<Long>): Map<Long, List<Genre>> = MediaStoreGenres.of(context, songIds)
}
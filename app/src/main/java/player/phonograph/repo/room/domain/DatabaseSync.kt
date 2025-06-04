/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.SyncResult
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.withTransaction
import android.content.Context


class BasicSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    private val mediaStoreSongDao = musicDatabase.MediaStoreSongDao()

    override suspend fun check(context: Context): Boolean {

        val songsMediastore = songsFromMediastore(context)
        val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }

        val songsDatabase = songsFromDatabase()
        val latestDatabase = songsDatabase.maxByOrNull { it.dateModified }

        return if (songsMediastore.size != songsDatabase.size || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }
    }

    override suspend fun sync(
        context: Context,
        channel: SyncExecutor.SyncProgressConnection?,
    ): SyncResult {
        val songsMediastore = songsFromMediastore(context)
        val total = songsMediastore.size
        channel?.onProcessUpdate(0, total)
        musicDatabase.withTransaction {
            mediaStoreSongDao.deleteAll()
            mediaStoreSongDao.insert(songsMediastore.map(EntityConverter::fromSongModel))
        }
        channel?.onProcessUpdate(total, total)
        return SyncResult(success = true, modified = total)
    }

    private suspend fun songsFromDatabase(): List<MediastoreSongEntity> =
        mediaStoreSongDao.all(SortMode(SortRef.MODIFIED_DATE, true))

    private suspend fun songsFromMediastore(context: Context): List<Song> =
        MediaStoreSongs.all(context)
}

private const val TAG = "DatabaseSync"
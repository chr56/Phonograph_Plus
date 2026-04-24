/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.SyncReport
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import androidx.room.withTransaction
import android.content.Context

/**
 * Simple [SyncExecutor], not relationship solving
 */
class BasicSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    override suspend fun check(context: Context): Boolean = SyncExecutors.defaultCheck(context, musicDatabase)

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncReport {
        val songsMediastore = MediaStoreSongs.all(context)
        val total = songsMediastore.size
        channel?.onProcessUpdate(0, total)
        val songDao = musicDatabase.MediaStoreSongDao()
        musicDatabase.withTransaction {
            songDao.deleteAll()
            songDao.update(songsMediastore.map(EntityConverter::fromSongModel))
        }
        channel?.onProcessUpdate(total, total)
        return SyncReport(success = true, modified = total)
    }

}
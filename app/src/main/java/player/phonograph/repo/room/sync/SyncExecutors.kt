/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

object SyncExecutors {

    /**
     * Obtain the correct [SyncExecutor] based on Settings
     */
    suspend fun obtain(context: Context, musicDatabase: MusicDatabase): SyncExecutor {
        val backend = Setting(context)[Keys.musicLibraryBackend].read()
        val syncExecutor = when {
            backend.syncBasicDatabase -> BasicSyncExecutor(musicDatabase)
            else                      -> RelationshipSyncExecutor(musicDatabase, backend.syncWithGenres)
        }
        return syncExecutor
    }

    /**
     * default function to check database sync status;
     * used internally
     */
    suspend fun defaultCheck(context: Context, musicDatabase: MusicDatabase): Boolean {
        val songsCountMediastore = MediaStoreSongs.total(context)
        val latestMediastore = MediaStoreSongs.lastest(context)

        val songsCountDatabase = musicDatabase.MediaStoreSongDao().total()
        val latestDatabase = musicDatabase.MediaStoreSongDao().latest()

        return if (songsCountMediastore != songsCountDatabase || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }
    }
}
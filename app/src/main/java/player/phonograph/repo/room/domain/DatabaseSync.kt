/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.withTransaction
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSync {


    /**
     * Sync Database:
     * check MediaStore, refresh database if have changes
     */
    suspend fun checkAndRefresh(context: Context, musicDatabase: MusicDatabase) {

        val mediaStoreSongDao = musicDatabase.MediaStoreSongDao()

        val songsMediastore = MediaStoreSongs.all(context)
        val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }

        val songsDatabase = mediaStoreSongDao.all(SortMode(SortRef.MODIFIED_DATE, true))
        val latestDatabase = songsDatabase.maxByOrNull { it.dateModified }

        if (shouldRefresh(songsMediastore, songsDatabase, latestMediastore, latestDatabase)) {
            musicDatabase.withTransaction {
                mediaStoreSongDao.deleteAll()
                mediaStoreSongDao.insert(songsMediastore.map(EntityConverter::fromSongModel))
            }
        }

    }

    private fun shouldRefresh(
        songsMediastore: List<Song>,
        songsDatabase: List<MediastoreSongEntity>,
        latestMediastore: Song?,
        latestDatabase: MediastoreSongEntity?,
    ): Boolean =
        if (songsMediastore.size != songsDatabase.size || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }


    /**
     * Close Database and wipe all table
     */
    suspend fun purge(musicDatabase: MusicDatabase) {
        withContext(Dispatchers.IO) {
            synchronized(musicDatabase) {
                musicDatabase.close()
                musicDatabase.clearAllTables()
            }
        }
    }
}
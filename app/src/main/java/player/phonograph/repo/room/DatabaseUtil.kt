/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.converter.MediastoreSongConverter
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseUtil {

    suspend fun checkAndRefresh(context: Context, musicDatabase: MusicDatabase) {
        val mediaStoreSongDao = musicDatabase.MediaStoreSongDao()

        val songsMediastore = MediaStoreSongs.all(context)
        val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }


        val songsCurrent = mediaStoreSongDao.all(SortMode(SortRef.MODIFIED_DATE, true))
        val latestCurrent = songsCurrent.maxByOrNull { it.dateModified }

        val shouldRefresh =
            if (songsMediastore.size != songsCurrent.size || latestCurrent == null || latestMediastore == null) {
                true
            } else {
                latestMediastore.dateModified >= latestCurrent.dateModified
            }

        if (shouldRefresh) {
            mediaStoreSongDao.refresh(songsMediastore.map(MediastoreSongConverter::fromSongModel))
        }

    }

    suspend fun deleteAll(musicDatabase: MusicDatabase) {
        withContext(Dispatchers.IO) {
            synchronized(musicDatabase) {
                musicDatabase.close()
                musicDatabase.clearAllTables()
            }
        }
    }

    suspend fun deleteEntireDatabase(context: Context, musicDatabase: MusicDatabase) = withContext(Dispatchers.IO) {
        val path = context.getDatabasePath(MusicDatabase.DATABASE_NAME)
        try {
            synchronized(musicDatabase) {
                musicDatabase.close()
                path.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}
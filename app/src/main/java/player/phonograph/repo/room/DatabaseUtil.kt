/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.converter.EntityConverter
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DatabaseUtil {

    fun syncWithMediastore(context: Context, musicDatabase: MusicDatabase): EventHub.EventReceiver {
        class PersistentListener : EventHub.EventReceiver(EventHub.EVENT_MEDIASTORE_CHANGED) {

            override fun onEventReceived(context: Context, intent: Intent) {
                refresh()
            }


            fun refresh() {
                coroutineScope.launch {
                    checkAndRefresh(context, musicDatabase)
                }
            }

            val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        return PersistentListener().apply {
            refresh()
            registerSelf(context)
        }
    }

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
            mediaStoreSongDao.refresh(songsMediastore.map(EntityConverter::fromSongModel))
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
/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import player.phonograph.service.queue.internal.LegacyQueueMigration
import player.phonograph.service.queue.internal.ORIGINAL_QUEUE_TYPE
import player.phonograph.service.queue.internal.PLAYING_QUEUE_TYPE
import player.phonograph.service.queue.internal.QueueSongDao
import player.phonograph.service.queue.internal.QueueSongConverter
import player.phonograph.service.queue.internal.QueueSongEntity
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Database(
    entities = [QueueSongEntity::class],
    version = QueueDatabase.DATABASE_REVISION,
    exportSchema = true,
)
abstract class QueueDatabase : RoomDatabase() {

    abstract fun queueSongDao(): QueueSongDao

    suspend fun saveQueues(playingQueue: List<Song>, originalPlayingQueue: List<Song>) {
        queueSongDao().replaceQueues(
            playingQueue.mapIndexed { index, song ->
                QueueSongConverter.fromSong(song, PLAYING_QUEUE_TYPE, index)
            },
            originalPlayingQueue.mapIndexed { index, song ->
                QueueSongConverter.fromSong(song, ORIGINAL_QUEUE_TYPE, index)
            },
        )
    }

    suspend fun savedPlayingQueue(): List<Song> = savedQueue(PLAYING_QUEUE_TYPE)

    suspend fun savedOriginalPlayingQueue(): List<Song> = savedQueue(ORIGINAL_QUEUE_TYPE)

    private suspend fun savedQueue(queueType: Int): List<Song> =
        queueSongDao().queue(queueType).map(QueueSongConverter::toSong)

    companion object {
        const val DATABASE_NAME = "queue_database.db"
        const val DATABASE_REVISION = 1

        fun instance(context: Context): QueueDatabase {
            val applicationContext = context.applicationContext
            return Room.databaseBuilder(
                applicationContext,
                QueueDatabase::class.java,
                DATABASE_NAME,
            )
                .enableMultiInstanceInvalidation()
                .build()
                .also { database ->
                    runBlocking(Dispatchers.IO) {
                        LegacyQueueMigration.migrate(applicationContext, database)
                    }
                }
        }
    }
}
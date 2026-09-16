/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.queue.internal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class QueueSongDao {

    @Query(
        "SELECT * FROM $QUEUE_SONGS_TABLE " +
                "WHERE $QUEUE_TYPE_COLUMN = :queueType " +
                "ORDER BY $ORDER_IN_QUEUE_COLUMN ASC"
    )
    abstract suspend fun queue(queueType: Int): List<QueueSongEntity>

    @Query("DELETE FROM $QUEUE_SONGS_TABLE WHERE $QUEUE_TYPE_COLUMN = :queueType")
    abstract suspend fun deleteQueue(queueType: Int)

    @Insert
    abstract suspend fun insert(songs: List<QueueSongEntity>)

    @Transaction
    open suspend fun replaceQueues(
        playingQueue: List<QueueSongEntity>,
        originalPlayingQueue: List<QueueSongEntity>,
    ) {
        deleteQueue(PLAYING_QUEUE_TYPE)
        deleteQueue(ORIGINAL_QUEUE_TYPE)
        if (playingQueue.isNotEmpty()) insert(playingQueue)
        if (originalPlayingQueue.isNotEmpty()) insert(originalPlayingQueue)
    }
}
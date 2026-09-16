/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Buffer
import okio.Source
import okio.buffer
import okio.sink
import org.koin.core.context.GlobalContext
import player.phonograph.foundation.currentTimestamp
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.file.createOrOverride
import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.model.Song
import player.phonograph.model.backup.BackupItemExecutor
import player.phonograph.service.queue.QueueDatabase
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.internal.LegacyQueueMigration
import player.phonograph.service.queue.internal.ORDER_IN_QUEUE_COLUMN
import player.phonograph.service.queue.internal.ORIGINAL_QUEUE_TYPE
import player.phonograph.service.queue.internal.PLAYING_QUEUE_TYPE
import player.phonograph.service.queue.internal.QUEUE_SONG_PROJECTION
import player.phonograph.service.queue.internal.QUEUE_SONGS_TABLE
import player.phonograph.service.queue.internal.QUEUE_TYPE_COLUMN
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PlayingQueuesDatabaseBackupItemExecutor : BackupItemExecutor {

    override suspend fun export(context: Context): Buffer? = try {
        withContext(Dispatchers.IO) {
            GlobalContext.get()
                .get<QueueDatabase>()
                .openHelper
                .writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() }
        }
        RawDatabaseBackupItemExecutor(QueueDatabase.DATABASE_NAME).export(context)
    } catch (e: Exception) {
        warning(context, TAG, "Failed to export playback queue database", e)
        null
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val cacheDirectory = File(
            context.externalCacheDir ?: context.cacheDir!!,
            "QueueBackup_${currentTimestamp()}",
        )
        val tempFile = File(cacheDirectory, QueueDatabase.DATABASE_NAME)
        return try {
            cacheDirectory.mkdirs()
            tempFile.createOrOverride()
            tempFile.sink().buffer().use { it.writeAll(source) }

            val queues = withContext(Dispatchers.IO) { readQueues(context, tempFile) }
                ?: return false
            GlobalContext.get().get<QueueDatabase>().saveQueues(queues.playing, queues.original)
            GlobalContext.get().get<QueueManager>().reload()
            true
        } catch (e: Exception) {
            warning(context, TAG, "Failed to import playback queue database", e)
            false
        } finally {
            tempFile.delete()
            cacheDirectory.delete()
        }
    }

    private fun readQueues(context: Context, file: File): ImportedQueues? =
        SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            val tables = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null,
            ).use { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.moveToNext()) names.add(cursor.getString(0))
                names
            }

            when {
                QUEUE_SONGS_TABLE in tables -> ImportedQueues(
                    playing = readRoomQueue(database, PLAYING_QUEUE_TYPE),
                    original = readRoomQueue(database, ORIGINAL_QUEUE_TYPE),
                )

                LegacyQueueMigration.PLAYING_QUEUE_TABLE in tables &&
                        LegacyQueueMigration.ORIGINAL_PLAYING_QUEUE_TABLE in tables -> {
                    val (playing, original) = LegacyQueueMigration.readQueues(database)
                    ImportedQueues(playing, original)
                }

                else -> {
                    warning(context, TAG, "Unsupported playback queue database schema")
                    null
                }
            }
        }

    private fun readRoomQueue(database: SQLiteDatabase, queueType: Int): List<Song> =
        database.query(
            QUEUE_SONGS_TABLE,
            QUEUE_SONG_PROJECTION,
            "$QUEUE_TYPE_COLUMN = ?",
            arrayOf(queueType.toString()),
            null,
            null,
            "$ORDER_IN_QUEUE_COLUMN ASC",
        ).intoSongs()

    private data class ImportedQueues(
        val playing: List<Song>,
        val original: List<Song>,
    )

    private const val TAG = "PlayingQueuesBackup"
}
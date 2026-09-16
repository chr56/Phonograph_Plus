/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.queue.internal

import player.phonograph.foundation.error.warning
import player.phonograph.foundation.mediastore.BASE_SONG_PROJECTION
import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.model.Song
import player.phonograph.service.queue.QueueDatabase
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

internal object LegacyQueueMigration {

    const val LEGACY_DATABASE_NAME = "music_playback_state.db"

    private const val LEGACY_DATABASE_REVISION = 6
    internal const val PLAYING_QUEUE_TABLE = "playing_queue"
    internal const val ORIGINAL_PLAYING_QUEUE_TABLE = "original_playing_queue"
    private const val TAG = "LegacyQueueMigration"

    suspend fun migrate(context: Context, queueDatabase: QueueDatabase) {
        val legacyFile = context.getDatabasePath(LEGACY_DATABASE_NAME)
        if (!legacyFile.isFile) return

        try {
            val queues = SQLiteDatabase.openDatabase(
                legacyFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database ->
                check(database.version == LEGACY_DATABASE_REVISION) {
                    "Unsupported legacy queue database version ${database.version}"
                }
                readQueues(database)
            }

            queueDatabase.saveQueues(queues.first, queues.second)
            moveLegacyDatabase(context, legacyFile)
        } catch (e: Exception) {
            warning(
                context,
                TAG,
                "Failed to migrate the legacy playback queue database. It will be retried on the next launch.",
                e,
            )
        }
    }

    fun readQueues(database: SQLiteDatabase): Pair<List<Song>, List<Song>> =
        readQueue(database, PLAYING_QUEUE_TABLE) to
                readQueue(database, ORIGINAL_PLAYING_QUEUE_TABLE)

    private fun readQueue(database: SQLiteDatabase, tableName: String): List<Song> =
        database.query(
            tableName,
            BASE_SONG_PROJECTION,
            null,
            null,
            null,
            null,
            "rowid ASC",
        ).intoSongs()

    private fun moveLegacyDatabase(context: Context, legacyFile: File) {
        val backupDirectory = checkNotNull(context.getExternalFilesDir(null)) {
            "External files directory is unavailable"
        }
        val backupFile = File(
            backupDirectory,
            "$LEGACY_DATABASE_NAME.${System.currentTimeMillis()}.bkp",
        )
        check(legacyFile.renameTo(backupFile)) {
            "Failed to move ${legacyFile.absolutePath} to ${backupFile.absolutePath}"
        }
    }
}
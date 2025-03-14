/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.SortedLongCursor
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

/**
 * intermediate loader to help query database songs with mediastore
 */
abstract class DatabaseAgentLoader {

    /**
     * @return all songs the loader can be provided
     */
    fun tracks(context: Context): List<Song> = queryCursorAndClear(context).intoSongs()

    /**
     * query the database and return a Song cursor
     * @return Song cursor supporting (see [intoSongCursor])
     */
    protected abstract fun queryCursorImpl(context: Context): Cursor?

    /**
     * clean the database
     * @param existed ids that existed in MediaStore
     */
    protected abstract fun clean(context: Context, existed: List<Long>)

    /**
     * whether this loader needs to be cleaned frequently
     */
    protected abstract val cleanable: Boolean

    private fun queryCursorAndClear(context: Context): Cursor? {

        val songCursor = queryCursorImpl(context) ?: return null

        if (cleanable) {
            // clean up the databases with any ids not found
            clean(context, songIds(songCursor))
        }

        return songCursor
    }

    private fun songIds(songCursor: Cursor, idColumnName: String = BaseColumns._ID): List<Long> {
        val exists = mutableListOf<Long>()
        if (songCursor.moveToFirst()) {
            val index = songCursor.getColumnIndex(idColumnName)
            do {
                val id = songCursor.getLong(index)
                if (id > 0) exists.add(id)
            } while (songCursor.moveToNext())
        }
        return exists
    }

    /**
     * Convert Database cursor to Song cursor
     * @param idColumnName foreign key to MediaStore song id, in Database cursor
     */
    protected fun Cursor.intoSongCursor(context: Context, idColumnName: String): SortedLongCursor? =
        use { cursor -> generateSongCursor(context, cursor, idColumnName) }

    private fun generateSongCursor(context: Context, cursor: Cursor, idColumnName: String): SortedLongCursor? {
        val count = cursor.count
        val idColumnIndex = cursor.getColumnIndex(idColumnName)

        if (count < 0 || idColumnIndex < 0) return null

        cursor.moveToFirst()
        val selectionPlaceHolder = when {
            count > 1  -> "?" + ",?".repeat(count - 1)
            count == 1 -> "?"
            else       -> return null // empty cursor
        }

        val ids = LongArray(count) {
            cursor.getLong(idColumnIndex).also { cursor.moveToNext() }
        }

        val songCursor = querySongs(
            context,
            selection = "${BaseColumns._ID}  IN ( $selectionPlaceHolder )",
            selectionValues = ids.map { it.toString() }.toTypedArray()
        ) ?: return null

        return SortedLongCursor(songCursor, ids, BaseColumns._ID)
    }

}
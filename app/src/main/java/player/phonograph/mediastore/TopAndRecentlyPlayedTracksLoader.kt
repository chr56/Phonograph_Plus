/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.mediastore

import player.phonograph.mediastore.internal.SortedLongCursor
import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

object TopAndRecentlyPlayedTracksLoader {
    private const val NUMBER_OF_TOP_TRACKS = 150

    fun getRecentlyPlayedTracks(context: Context) = makeRecentTracksCursorAndClearUpDatabase(context).intoSongs()

    fun getTopTracks(context: Context) = makeTopTracksCursorAndClearUpDatabase(context).intoSongs()

    fun makeRecentTracksCursorAndClearUpDatabase(context: Context): Cursor? {
        val cursor = makeRecentTracksCursorImpl(context)

        // clean up the databases with any ids not found

        cursor?.let {
            val missingIds = cursor.missingIds
            if (missingIds.isNotEmpty()) {
                for (id in missingIds) {
                    HistoryStore.getInstance(context).removeSongId(id)
                }
            }
        }
        return cursor
    }

    fun makeTopTracksCursorAndClearUpDatabase(context: Context): Cursor? {
        val retCursor = makeTopTracksCursorImpl(context)

        // clean up the databases with any ids not found
        if (retCursor != null) {
            val missingIds = retCursor.missingIds
            if (missingIds.isNotEmpty()) {
                for (id in missingIds) {
                    SongPlayCountStore.getInstance(context).removeItem(id)
                }
            }
        }
        return retCursor
    }

    private fun makeRecentTracksCursorImpl(context: Context): SortedLongCursor? {
        // first get the top results ids from the internal database
        return HistoryStore.getInstance(context).queryRecentIds().use { cursor ->
            cursor.makeSortedCursor(
                context, cursor.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
            )
        }
    }

    private fun makeTopTracksCursorImpl(context: Context): SortedLongCursor? {
        // first get the top results ids from the internal database
        return SongPlayCountStore.getInstance(context).getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
            .use { cursor ->
                cursor.makeSortedCursor(
                    context, cursor.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)
                )
            }
    }

    private fun Cursor.makeSortedCursor(context: Context, idColumn: Int): SortedLongCursor? {
        moveToFirst()

        val selectionPlaceHolder = when {
            count > 1  -> "?" + ",?".repeat(count - 1)
            count == 1 -> "?"
            else       -> return null // empty cursor
        }

        val ids = LongArray(count) {
            getLong(idColumn).also { moveToNext() }
        }

        val songCursor = querySongs(
            context,
            selection = "${BaseColumns._ID}  IN ( $selectionPlaceHolder )",
            selectionValues = ids.map { it.toString() }.toTypedArray()
        ) ?: return null

        return SortedLongCursor(songCursor, ids, BaseColumns._ID)
    }
}

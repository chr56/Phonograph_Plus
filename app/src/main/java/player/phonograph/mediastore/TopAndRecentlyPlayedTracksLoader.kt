/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.mediastore

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import lib.phonograph.misc.SortedLongCursor
import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore

object TopAndRecentlyPlayedTracksLoader {
    const val NUMBER_OF_TOP_TRACKS = 150

    fun getRecentlyPlayedTracks(context: Context) =
        SongLoader.getSongs(makeRecentTracksCursorAndClearUpDatabase(context))

    fun getTopTracks(context: Context) =
        SongLoader.getSongs(makeTopTracksCursorAndClearUpDatabase(context))

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
        val cursor = HistoryStore.getInstance(context).queryRecentIds()
        return cursor.use { songs ->
            makeSortedCursor(
                context, songs,
                songs!!.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
            )
        }
    }

    private fun makeTopTracksCursorImpl(context: Context): SortedLongCursor? {
        // first get the top results ids from the internal database
        val cursor = SongPlayCountStore.getInstance(context).getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
        return cursor.use { songs ->
            makeSortedCursor(
                context, songs,
                songs!!.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)
            )
        }
    }

    private fun makeSortedCursor(context: Context, cursor: Cursor?, idColumn: Int): SortedLongCursor? {
        if (cursor != null && cursor.moveToFirst()) {
            // create the list of ids to select against
            val selection = StringBuilder()
            selection.append(BaseColumns._ID)
            selection.append(" IN (")

            // this tracks the order of the ids
            val order = LongArray(cursor.count)
            var id = cursor.getLong(idColumn)
            selection.append(id)
            order[cursor.position] = id
            while (cursor.moveToNext()) {
                selection.append(",")
                id = cursor.getLong(idColumn)
                order[cursor.position] = id
                selection.append(id.toString())
            }
            selection.append(")")

            // get a list of songs with the data given the selection statement
            val songCursor = SongLoader.makeSongCursor(context, selection.toString(), null)
            if (songCursor != null) {
                // now return the wrapped TopTracksCursor to handle sorting given order
                return SortedLongCursor(songCursor, order, BaseColumns._ID)
            }
        }
        return null
    }
}

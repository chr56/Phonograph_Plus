/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package player.phonograph.mediastore

import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

object TopAndRecentlyPlayedTracksLoader {
    private const val NUMBER_OF_TOP_TRACKS = 150

    fun getRecentlyPlayedTracks(context: Context) = makeRecentTracksCursorAndClearUpDatabase(context).intoSongs()

    fun getTopTracks(context: Context) = makeTopTracksCursorAndClearUpDatabase(context).intoSongs()

    private fun makeRecentTracksCursorAndClearUpDatabase(context: Context): Cursor? {

        val songCursor = recentTracksSongCursor(context) ?: return null

        // clean up the databases with any ids not found
        val exists = songIds(songCursor)
        HistoryStore.getInstance(context).gc(exists)

        return songCursor
    }

    private fun makeTopTracksCursorAndClearUpDatabase(context: Context): Cursor? {

        val songCursor = topTracksSongCursor(context) ?: return null

        // clean up the databases with any ids not found
        val exists = songIds(songCursor)
        SongPlayCountStore.getInstance(context).gc(exists)

        return songCursor
    }

    private fun recentTracksSongCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return HistoryStore.getInstance(context).queryRecentIds().use { cursor ->
            cursor.generateSongCursor(
                context, cursor.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
            )
        }
    }

    private fun topTracksSongCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return SongPlayCountStore.getInstance(context).getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
            .use { cursor ->
                cursor.generateSongCursor(
                    context, cursor.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)
                )
            }
    }

    private fun Cursor.generateSongCursor(context: Context, idColumn: Int): Cursor? {
        moveToFirst()

        val selectionPlaceHolder = when {
            count > 1  -> "?" + ",?".repeat(count - 1)
            count == 1 -> "?"
            else       -> return null // empty cursor
        }

        val ids = Array(count) {
            getLong(idColumn).toString().also { moveToNext() }
        }

        return querySongs(
            context,
            selection = "${BaseColumns._ID}  IN ( $selectionPlaceHolder )",
            selectionValues = ids
        )
    }

    private fun songIds(songCursor: Cursor): List<Long> {
        val exists = mutableListOf<Long>()
        if (songCursor.moveToFirst()) {
            val index = songCursor.getColumnIndex(BaseColumns._ID)
            do {
                val id = songCursor.getLong(index)
                if (id > 0) exists.add(id)
            } while (songCursor.moveToNext())
        }
        return exists
    }
}

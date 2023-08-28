/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.repo.mediastore.loaders.dynamics

import org.koin.core.context.GlobalContext
import player.phonograph.model.Song
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

object TopAndRecentlyPlayedTracksLoader {
    private const val NUMBER_OF_TOP_TRACKS = 150

    private val historyStore: HistoryStore by GlobalContext.get().inject()
    private val songPlayCountStore: SongPlayCountStore by GlobalContext.get().inject()

    fun recentlyPlayedTracks(context: Context): List<Song> =
        makeRecentTracksCursorAndClearUpDatabase(context).intoSongs()

    fun topTracks(context: Context): List<Song> =
        makeTopTracksCursorAndClearUpDatabase(context).intoSongs().take(NUMBER_OF_TOP_TRACKS)

    private fun makeRecentTracksCursorAndClearUpDatabase(context: Context): Cursor? {

        val songCursor = recentTracksSongCursor(context) ?: return null

        // clean up the databases with any ids not found
        val exists = songIds(songCursor)
        historyStore.gc(exists)

        return songCursor
    }

    private fun makeTopTracksCursorAndClearUpDatabase(context: Context): Cursor? {

        val songCursor = topTracksSongCursor(context) ?: return null

        // clean up the databases with any ids not found
        val exists = songIds(songCursor)
        songPlayCountStore.gc(exists)

        return songCursor
    }

    private fun recentTracksSongCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return historyStore.queryRecentIds().use { cursor ->
            cursor.generateSongCursor(
                context, cursor.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
            )
        }
    }

    private fun topTracksSongCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return songPlayCountStore.getTopPlayedResults(0)
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

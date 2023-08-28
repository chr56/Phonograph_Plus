/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.repo.mediastore.loaders

import org.koin.core.context.GlobalContext
import player.phonograph.model.Song
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.ShallowDatabase
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

class TopTracksLoader(private val songPlayCountStore: SongPlayCountStore) :
        DynamicDatabaseLoader(songPlayCountStore) {

    override fun tracks(context: Context): List<Song> =
        queryAndClearCursor(context).intoSongs().take(NUMBER_OF_TOP_TRACKS)

    override fun queryCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return songPlayCountStore.getTopPlayedResults(0)
            .use { cursor ->
                cursor.generateSongCursor(
                    context, cursor.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)
                )
            }
    }

    companion object {
        private const val NUMBER_OF_TOP_TRACKS = 150
        fun get() = GlobalContext.get().get<TopTracksLoader>()
    }
}

class RecentlyPlayedTracksLoader(private val historyStore: HistoryStore) :
        DynamicDatabaseLoader(historyStore) {

    override fun tracks(context: Context): List<Song> =
        queryAndClearCursor(context).intoSongs()

    override fun queryCursor(context: Context): Cursor? {
        // first get the top results ids from the internal database
        return historyStore.queryRecentIds().use { cursor ->
            cursor.generateSongCursor(
                context, cursor.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
            )
        }
    }

    companion object {
        fun get() = GlobalContext.get().get<RecentlyPlayedTracksLoader>()
    }
}

abstract class DynamicDatabaseLoader(private val db: Any) {

    abstract fun tracks(context: Context): List<Song>
    abstract fun queryCursor(context: Context): Cursor?

    protected fun queryAndClearCursor(context: Context): Cursor? {

        val songCursor = queryCursor(context) ?: return null

        if (db is ShallowDatabase) {
            // clean up the databases with any ids not found
            val exists = songIds(songCursor)
            db.gc(exists)
        }

        return songCursor
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

    protected fun Cursor.generateSongCursor(context: Context, idColumn: Int): Cursor? {
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

}
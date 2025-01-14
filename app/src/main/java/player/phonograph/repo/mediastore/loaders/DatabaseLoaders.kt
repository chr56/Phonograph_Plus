/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.repo.mediastore.loaders

import org.koin.core.context.GlobalContext
import player.phonograph.model.Song
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.ShallowDatabase
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.internal.SortedLongCursor
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

class TopTracksLoader(private val songPlayCountStore: SongPlayCountStore) :
        DynamicDatabaseLoader(songPlayCountStore) {

    override fun queryCursorImpl(context: Context): Cursor? =
        songPlayCountStore.getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
            .intoSongCursor(context, SongPlayCountStore.SongPlayCountColumns.ID)

    companion object {
        private const val NUMBER_OF_TOP_TRACKS = 150
        fun get() = GlobalContext.get().get<TopTracksLoader>()
    }
}

class RecentlyPlayedTracksLoader(private val historyStore: HistoryStore) :
        DynamicDatabaseLoader(historyStore) {

    override fun queryCursorImpl(context: Context): Cursor? =
        historyStore.queryRecentIds()
            .intoSongCursor(context, HistoryStore.RecentStoreColumns.ID)

    companion object {
        fun get() = GlobalContext.get().get<RecentlyPlayedTracksLoader>()
    }
}

abstract class DynamicDatabaseLoader(private val db: Any) {

    fun tracks(context: Context): List<Song> = queryCursorAndClear(context).intoSongs()

    protected abstract fun queryCursorImpl(context: Context): Cursor?

    protected fun queryCursorAndClear(context: Context): Cursor? {

        val songCursor = queryCursorImpl(context) ?: return null

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

    protected fun Cursor.intoSongCursor(context: Context, idColumnName: String): SortedLongCursor? =
        use { cursor -> generateSongCursor(context, cursor, idColumnName) }

    protected fun generateSongCursor(context: Context, cursor: Cursor, idColumnName: String): SortedLongCursor? {
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
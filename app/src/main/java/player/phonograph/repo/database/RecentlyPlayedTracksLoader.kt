/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database

import org.koin.core.context.GlobalContext
import player.phonograph.repo.mediastore.DatabaseAgentLoader
import android.content.Context
import android.database.Cursor

class RecentlyPlayedTracksLoader(private val historyStore: HistoryStore) : DatabaseAgentLoader() {

    override fun queryCursorImpl(context: Context): Cursor? =
        historyStore.queryRecentIds()
            .intoSongCursor(context, HistoryStore.RecentStoreColumns.ID)

    override val cleanable: Boolean = true

    override fun clean(context: Context, existed: List<Long>) {
        historyStore.gc(existed)
    }

    companion object {
        fun get() = GlobalContext.get().get<RecentlyPlayedTracksLoader>()
    }
}
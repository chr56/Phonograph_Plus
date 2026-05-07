/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import player.phonograph.repo.database.store.HistoryStore
import android.content.Context
import android.database.Cursor

object RecentlyPlayedTracksLoader : DatabaseAgentLoader() {

    override suspend fun queryCursorImpl(context: Context): Cursor? =
        HistoryStore.get().queryRecentIds()
            .intoSongCursor(context, HistoryStore.RecentStoreColumns.ID)

    override val cleanable: Boolean = true

    override suspend fun clean(context: Context, existed: List<Long>) {
        HistoryStore.get().gc(existed)
    }
}
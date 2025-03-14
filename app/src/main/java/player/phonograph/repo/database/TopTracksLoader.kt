/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database

import org.koin.core.context.GlobalContext
import player.phonograph.repo.mediastore.DatabaseAgentLoader
import android.content.Context
import android.database.Cursor

class TopTracksLoader(private val songPlayCountStore: SongPlayCountStore) : DatabaseAgentLoader() {

    override fun queryCursorImpl(context: Context): Cursor? =
        songPlayCountStore.getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
            .intoSongCursor(context, SongPlayCountStore.SongPlayCountColumns.ID)

    override val cleanable: Boolean = true

    override fun clean(context: Context, existed: List<Long>) {
        songPlayCountStore.gc(existed)
    }

    companion object {
        private const val NUMBER_OF_TOP_TRACKS = 150
        fun get() = GlobalContext.get().get<TopTracksLoader>()
    }
}
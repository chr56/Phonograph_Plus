/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import player.phonograph.repo.database.store.SongPlayCountStore
import android.content.Context
import android.database.Cursor

object TopTracksLoader : DatabaseAgentLoader() {

    override suspend fun queryCursorImpl(context: Context): Cursor? =
        SongPlayCountStore.get().getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
            .intoSongCursor(context, SongPlayCountStore.SongPlayCountColumns.ID)

    override val cleanable: Boolean = true

    override suspend fun clean(context: Context, existed: List<Long>) {
        SongPlayCountStore.get().gc(existed)
    }

    private const val NUMBER_OF_TOP_TRACKS = 150
}
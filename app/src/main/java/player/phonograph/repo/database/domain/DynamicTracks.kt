/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.database.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IRecentTracks
import player.phonograph.model.repo.loader.ITopTracks
import player.phonograph.repo.database.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.database.loaders.TopTracksLoader
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.database.store.SongPlayCountStore
import android.content.Context

object DynamicTracks {

    object RecentTracks : IRecentTracks {
        override suspend fun all(context: Context): List<Song> = RecentlyPlayedTracksLoader.tracks(context)
        override fun clear(): Boolean = HistoryStore.get().clear()

        override fun add(songId: Long) = HistoryStore.get().addSongId(songId)
    }

    object TopTracks : ITopTracks {
        override suspend fun all(context: Context): List<Song> = TopTracksLoader.tracks(context)
        override fun clear(): Boolean = SongPlayCountStore.get().clear()

        override fun bump(songId: Long) = SongPlayCountStore.get().bumpPlayCount(songId)
        override fun refresh(context: Context) = SongPlayCountStore.get().reCalculateScore(context)
    }

}
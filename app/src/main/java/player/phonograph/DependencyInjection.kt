/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.mechanism.FavoriteDatabaseImpl
import player.phonograph.mechanism.FavoritePlaylistImpl
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.PathFilterStore
import player.phonograph.repo.database.RecentlyPlayedTracksLoader
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.database.TopTracksLoader
import player.phonograph.service.queue.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting

val moduleStatus = module {
    single { QueueManager(get()) }
    single { MediaStoreTracker(get()) }
}

val moduleLoaders = module {
    single { PathFilterStore(get()) }
    single { HistoryStore(get()) }
    single { SongPlayCountStore(get()) }
    single { FavoritesStore(get()) }
    single { MusicPlaybackQueueStore(get()) }

    factory {
        val preference = Setting(get())[Keys.useLegacyFavoritePlaylistImpl]
        if (preference.data) FavoritePlaylistImpl() else FavoriteDatabaseImpl()
    }

    factory { TopTracksLoader(get()) }
    factory { RecentlyPlayedTracksLoader(get()) }

}
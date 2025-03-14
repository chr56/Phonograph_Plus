/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.repo.database.loaders.DatabaseFavoriteSongLoader
import player.phonograph.repo.database.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.database.loaders.TopTracksLoader
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.database.store.PathFilterStore
import player.phonograph.repo.database.store.SongPlayCountStore
import player.phonograph.repo.mediastore.loaders.PlaylistFavoriteSongLoader
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
        if (preference.data) PlaylistFavoriteSongLoader() else DatabaseFavoriteSongLoader()
    }

    factory { TopTracksLoader(get()) }
    factory { RecentlyPlayedTracksLoader(get()) }

}
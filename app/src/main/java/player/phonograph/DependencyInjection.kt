/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.PathFilterStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.service.queue.QueueManager

val moduleStatus = module {
    single { QueueManager(get()) }
    single { MediaStoreTracker(get()) }
}

val moduleLoaders = module {
    single { PathFilterStore(get()) }
    single { HistoryStore(get()) }
    single { SongPlayCountStore(get()) }
    single { FavoritesStore(get()) }
}
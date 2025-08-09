/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.repo.database.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.database.loaders.TopTracksLoader
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.database.store.SongPlayCountStore
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.service.queue.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueManager

val moduleStatus = module {
    single { QueueManager(get()) }
}

val moduleLoaders = module {
    single { HistoryStore(get()) }
    single { SongPlayCountStore(get()) }
    single { MusicPlaybackQueueStore(get()) }

    factory { TopTracksLoader(get()) }
    factory { RecentlyPlayedTracksLoader(get()) }


    single { MusicDatabase.instance(get()) }
}
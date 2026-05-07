/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo

import org.koin.dsl.module
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.database.store.SongPlayCountStore
import player.phonograph.repo.room.MusicDatabase

val moduleLoaders = module {
    single { HistoryStore(get()) }
    single { SongPlayCountStore(get()) }

    single { MusicDatabase.instance(get()) }
}
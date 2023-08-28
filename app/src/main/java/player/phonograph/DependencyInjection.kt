/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.service.queue.QueueManager

val module = module {
    single { QueueManager(get()) }
    single { MediaStoreTracker(get()) }
}
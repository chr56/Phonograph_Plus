/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service

import org.koin.dsl.module
import player.phonograph.service.queue.QueueDatabase
import player.phonograph.service.queue.QueueManager

val moduleQueue = module {
    single { QueueDatabase.instance(get()) }
    single { QueueManager(get()) }
}
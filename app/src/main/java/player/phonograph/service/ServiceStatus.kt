/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service

import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode

data class ServiceStatus(
    val isPlaying: Boolean,
    val shuffleMode: ShuffleMode,
    val repeatMode: RepeatMode
)
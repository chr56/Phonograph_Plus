/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song

interface QueueObserver {
    fun onQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {}
    fun onCurrentPositionChanged(newPosition: Int) {}
    fun onShuffleModeChanged(newMode: ShuffleMode) {}
    fun onRepeatModeChanged(newMode: RepeatMode) {}
}
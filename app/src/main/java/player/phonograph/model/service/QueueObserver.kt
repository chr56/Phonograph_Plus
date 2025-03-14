/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

import player.phonograph.model.Song

interface QueueObserver {
    fun onQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {}
    fun onCurrentPositionChanged(newPosition: Int) {}
    fun onShuffleModeChanged(newMode: ShuffleMode) {}
    fun onRepeatModeChanged(newMode: RepeatMode) {}
}
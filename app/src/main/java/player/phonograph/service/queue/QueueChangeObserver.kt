/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song

interface QueueChangeObserver {
    fun onStateRestored() {}
    fun onStateSaved() {}
    fun onQueueCursorChanged(newPosition: Int) {}
    fun onQueueChanged(
        shuffleChanged: ShuffleMode,
        newPlayingQueue: List<Song>,
        newOriginalQueue: List<Song>
    ) {}
    fun onShuffleModeChanged(newMode: ShuffleMode) {}
    fun onRepeatModeChanged(newMode: RepeatMode) {}
}

internal fun MutableList<QueueChangeObserver>.executeForEach(
    action: QueueChangeObserver.() -> Unit
) {
    for (observer in this) {
        action(observer)
    }
}

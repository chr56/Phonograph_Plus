/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.QueueObserver
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.SoftReference

@Suppress("ObjectPropertyName")
object QueueStateTracker {
    private const val TAG = "QueueStateTracker"

    private val _shuffleMode = MutableStateFlow(ShuffleMode.NONE)
    val shuffleMode get() = _shuffleMode.asStateFlow()

    private fun refreshShuffleMode(newMode: ShuffleMode) {
        _shuffleMode.update { newMode }
    }

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode get() = _repeatMode.asStateFlow()

    private fun refreshRepeatMode(newMode: RepeatMode) {
        _repeatMode.update { newMode }
    }

    private val _queue = MutableStateFlow(SoftReference(listOf<Song>()))
    val queue get() = _queue.asStateFlow()

    private fun refreshQueue(queue: List<Song>) {
        _queue.update { SoftReference(queue) }
    }

    private val _position = MutableStateFlow(-1)
    val position get() = _position.asStateFlow()

    private fun refreshPosition(newPosition: Int) {
        _position.update { newPosition }
    }

    private val _current = MutableStateFlow(Song.EMPTY_SONG)
    val currentSong get() = _current.asStateFlow()

    private fun refreshCurrentSong(song: Song) {
        _current.update { song }
    }

    private fun refreshAll(queueManager: QueueManager) {
        refreshQueue(queueManager.playingQueue)
        refreshPosition(queueManager.currentSongPosition)
        refreshShuffleMode(queueManager.shuffleMode)
        refreshRepeatMode(queueManager.repeatMode)
        refreshCurrentSong(queueManager.currentSong)
    }

    private object Observer : QueueObserver {
        override fun onQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {
            refreshQueue(newPlayingQueue)
            refreshCurrentSong(App.instance.queueManager.currentSong)
        }

        override fun onCurrentPositionChanged(newPosition: Int) {
            refreshPosition(newPosition)
            refreshCurrentSong(App.instance.queueManager.currentSong)
        }

        override fun onShuffleModeChanged(newMode: ShuffleMode) {
            refreshShuffleMode(newMode)
        }

        override fun onRepeatModeChanged(newMode: RepeatMode) {
            refreshRepeatMode(newMode)
        }
    }

    fun register(queueManager: QueueManager) {
        queueManager.addObserver(Observer)
    }

    fun unregister(queueManager: QueueManager) {
        queueManager.removeObserver(Observer)
    }

    fun init(context: App) {
        refreshAll(context.queueManager)
    }
}
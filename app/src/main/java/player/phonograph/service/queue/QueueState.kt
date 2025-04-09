/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import player.phonograph.model.service.QueueObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.SoftReference

object CurrentQueueState {
    private const val TAG = "QueueState"

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

    private val _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    val currentSong get() = _currentSong.asStateFlow()

    private fun refreshCurrentSong(song: Song?) {
        _currentSong.update { song }
    }

    private fun refreshAll(queueManager: QueueManager) {
        refreshQueue(queueManager.playingQueue)
        refreshPosition(queueManager.currentSongPosition)
        refreshShuffleMode(queueManager.shuffleMode)
        refreshRepeatMode(queueManager.repeatMode)
        refreshCurrentSong(queueManager.currentSong)
    }

    private object Observer : QueueObserver {
        override fun onQueueChanged(newPlayingQueue: List<Song>) {
            refreshQueue(newPlayingQueue)
        }

        override fun onCurrentPositionChanged(newPosition: Int) {
            refreshPosition(newPosition)
        }

        override fun onCurrentSongChanged(newSong: Song?) {
            refreshCurrentSong(newSong)
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

    fun init(queueManager: QueueManager) {
        refreshAll(queueManager)
    }
}
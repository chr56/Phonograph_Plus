/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.panel

import player.phonograph.model.Song
import player.phonograph.model.service.QueueObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.service.queue.QueueManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class QueueViewModel: ViewModel(), QueueObserver {

    //region States
    private val _queue = MutableStateFlow(listOf<Song>())
    val queue get() = _queue.asStateFlow()

    private val _position = MutableStateFlow(-1)
    val position get() = _position.asStateFlow()

    private val _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    val currentSong get() = _currentSong.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.NONE)
    val shuffleMode get() = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode get() = _repeatMode.asStateFlow()
    //endregion

    //region Callbacks
    override fun onQueueChanged(newPlayingQueue: List<Song>) {
        _queue.value = newPlayingQueue
    }

    override fun onCurrentPositionChanged(newPosition: Int) {
        _position.value = newPosition
    }

    override fun onCurrentSongChanged(newSong: Song?) {
        _currentSong.value = newSong
    }

    override fun onShuffleModeChanged(newMode: ShuffleMode) {
        _shuffleMode.value = newMode
    }

    override fun onRepeatModeChanged(newMode: RepeatMode) {
        _repeatMode.value = newMode
    }
    //endregion

    fun register(queueManager: QueueManager) {
        queueManager.addObserver(this)
    }

    fun unregister(queueManager: QueueManager) {
        queueManager.removeObserver(this)
    }

    fun refresh(queueManager: QueueManager) {
        _queue.value = queueManager.playingQueue
        _position.value = queueManager.currentSongPosition
        _currentSong.value = queueManager.currentSong
        _shuffleMode.value = queueManager.shuffleMode
        _repeatMode.value = queueManager.repeatMode
    }


    companion object {
        private const val TAG = "MusicServiceViewModel"
    }
}
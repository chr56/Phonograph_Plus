/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process.THREAD_PRIORITY_BACKGROUND

class QueueManager2(val context: Application) {

    private val handler: QueueManagerHandler
    private val thread = HandlerThread("QueueManagerHandler", THREAD_PRIORITY_BACKGROUND)
    private var queueHolder: QueueHolder
    private val observerManager: ObserverManager

    init {
        thread.start()
        handler = QueueManagerHandler(thread.looper)
        queueHolder = QueueHolder.fromPersistence(context)
        observerManager = ObserverManager()
    }

    /**
     * stop internal thread and release resource
     */
    fun release() {
        handler.looper.quitSafely()
        thread.quitSafely()
    }

    private inner class QueueManagerHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SAVE_QUEUE        -> saveQueue()
                MSG_SAVE_CFG          -> saveCfg()
                MSG_STATE_RESTORE_ALL -> restoreAllState()
            }
        }
    }

    val playingQueue: List<Song>
        @Synchronized get() = queueHolder.playingQueue
    val originalPlayingQueue: List<Song>
        @Synchronized get() = queueHolder.originalPlayingQueue
    val currentSongPosition: Int
        @Synchronized get() = queueHolder.currentSongPosition


    val shuffleMode: ShuffleMode
        @Synchronized get() = queueHolder.shuffleMode

    val repeatMode: RepeatMode
        @Synchronized get() = queueHolder.repeatMode

    val previousSongPosition: Int
        @Synchronized get() = queueHolder.previousSongPosition
    val nextSongPosition: Int
        @Synchronized get() = queueHolder.nextSongPosition

    val currentSong: Song get() = queueHolder.currentSong
    val previousSong: Song get() = queueHolder.previousSong
    val nextSong: Song get() = queueHolder.nextSong

    fun modifyQueue(
        async: Boolean = true,
        action: (MutableList<Song>, MutableList<Song>) -> Unit,
    ) = request(async) {
        queueHolder.modifyQueue(action)
        handler.post {
            with(queueHolder) {
                observerManager.notifyQueueChanged(playingQueue, originalPlayingQueue)
            }
        }
    }

    fun modifyPosition(
        async: Boolean = true,
        newPosition: Int,
    ) = request(async) {
        queueHolder.modifyPosition(newPosition)
        handler.post {
            observerManager.notifyCurrentPositionChanged(newPosition)
        }
    }

    fun modifyShuffleMode(
        async: Boolean = true,
        newShuffleMode: ShuffleMode,
    ) = request(async) {
        queueHolder.modifyShuffleMode(newShuffleMode)
        handler.post {
            observerManager.notifyShuffleModeChanged(newShuffleMode)
        }
    }

    fun modifyRepeatMode(
        async: Boolean = true,
        newRepeatMode: RepeatMode,
    ) = request(async) {
        queueHolder.modifyRepeatMode(newRepeatMode)
        handler.post {
            observerManager.notifyRepeatModeChanged(newRepeatMode)
        }
    }


    fun isLastTrack(): Boolean = currentSongPosition == playingQueue.size - 1

    fun isQueueEnded(): Boolean = nextSongPosition == -1 && isLastTrack()


    private fun saveQueue() = queueHolder.saveQueue(context)
    private fun saveCfg() = queueHolder.saveCfg(context)
    private fun restoreAllState() = queueHolder.saveAll(context)


    fun swapQueue(newQueue: List<Song>, newPosition: Int) =
        swapQueue(queueHolder, newQueue, newPosition)

    fun addSong(song: Song, position: Int = -1) =
        addSong(queueHolder, song, position)

    fun addSongs(songs: List<Song>, position: Int = -1) =
        addSongs(queueHolder, songs, position)

    fun removeSongAt(position: Int) =
        removeSongAt(queueHolder, position)

    fun removeSong(song: Song) =
        removeSong(queueHolder, song)

    fun moveSong(from: Int, to: Int) =
        moveSong(queueHolder, from, to)

    fun clearQueue() =
        clearQueue(queueHolder)


    fun post(what: Int) = handler.sendEmptyMessage(what)

    private inline fun request(async: Boolean, crossinline block: () -> Unit) {
        if (async) {
            handler.post {
                block()
            }
        } else {
            block()
        }
    }


    /**
     * synchronized
     */
    fun cycleRepeatMode() = queueHolder.cycleRepeatMode()

    /**
     * synchronized
     */
    fun toggleShuffle() = queueHolder.toggleShuffle()

    fun addObserver(observer: QueueObserver) = observerManager.addObserver(observer)
    fun removeObserver(observer: QueueObserver) = observerManager.removeObserver(observer)

    private inner class ObserverManager {
        private val observers: MutableList<QueueObserver> = ArrayList()
        fun addObserver(observer: QueueObserver) = observers.add(observer)
        fun removeObserver(observer: QueueObserver): Boolean = observers.remove(observer)

        fun notifyQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) =
            notifyAllObservers {
                onQueueChanged(newPlayingQueue, newOriginalQueue)
            }

        fun notifyCurrentPositionChanged(newPosition: Int) =
            notifyAllObservers {
                onCurrentPositionChanged(newPosition)
            }

        fun notifyShuffleModeChanged(newMode: ShuffleMode) =
            notifyAllObservers {
                onShuffleModeChanged(newMode)
            }

        fun notifyRepeatModeChanged(newMode: RepeatMode) =
            notifyAllObservers {
                onRepeatModeChanged(newMode)
            }

        private inline fun notifyAllObservers(block: QueueObserver.() -> Unit) {
            for (observer in observers) {
                block(observer)
            }
        }
    }

    companion object {
        const val MSG_STATE_RESTORE_ALL = 1
        const val MSG_SAVE_QUEUE = 2
        const val MSG_SAVE_CFG = 4
    }
}
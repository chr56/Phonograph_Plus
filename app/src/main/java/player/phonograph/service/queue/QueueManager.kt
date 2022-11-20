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

class QueueManager(val context: Application) {

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

    val previousListPosition: Int
        @Synchronized get() = queueHolder.previousListPosition
    val nextListPosition: Int
        @Synchronized get() = queueHolder.nextListPosition

    val currentSong: Song get() = queueHolder.currentSong
    val previousSong: Song get() = queueHolder.previousSong
    val nextSong: Song get() = queueHolder.nextSong

    fun modifyPosition(
        newPosition: Int,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyPosition(newPosition)
        handler.post {
            observerManager.notifyCurrentPositionChanged(newPosition)
        }
    }

    fun modifyShuffleMode(
        newShuffleMode: ShuffleMode,
        alongWithQueue: Boolean = true,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyShuffleMode(newShuffleMode)
        if (alongWithQueue) shuffleQueue()
        handler.post {
            observerManager.notifyShuffleModeChanged(newShuffleMode)
        }
    }

    fun modifyRepeatMode(
        newRepeatMode: RepeatMode,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyRepeatMode(newRepeatMode)
        handler.post {
            observerManager.notifyRepeatModeChanged(newRepeatMode)
        }
    }


    fun isLastTrack(): Boolean = currentSongPosition == playingQueue.size - 1

    fun isQueueEnded(): Boolean = nextSongPosition == -1 && isLastTrack()

    fun getRestSongsDuration(position: Int): Long = queueHolder.getRestSongsDuration(position)


    private fun saveQueue() = queueHolder.saveQueue(context)
    private fun saveCfg() = queueHolder.saveCfg(context)
    private fun restoreAllState() = queueHolder.saveAll(context)


    fun swapQueue(newQueue: List<Song>, newPosition: Int, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            swapQueue(queueHolder, newQueue, newPosition)
        }
    }

    fun addSong(song: Song, position: Int = -1, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            addSong(queueHolder, song, position)
        }
    }

    fun addSongs(songs: List<Song>, position: Int = -1, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            addSongs(queueHolder, songs, position)
        }
    }

    fun removeSongAt(position: Int, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            removeSongAt(queueHolder, position)
        }
    }

    fun removeSong(song: Song, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            removeSong(queueHolder, song)
        }
    }

    fun moveSong(from: Int, to: Int, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            moveSong(queueHolder, from, to)
        }
    }

    fun clearQueue(async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder) {
            clearQueue(queueHolder)
        }
    }

    private fun shuffleQueue() {
        shuffle(queueHolder, queueHolder.shuffleMode)
    }

    fun moveToNextSong(async: Boolean = true) = async(async) {
        queueHolder.modifyPosition(nextSongPosition)
        handler.post {
            observerManager.notifyCurrentPositionChanged(queueHolder.currentSongPosition)
        }
    }

    fun post(what: Int) = handler.sendEmptyMessage(what)

    private fun async(async: Boolean, block: () -> Unit) {
        if (async) {
            handler.post {
                block()
            }
        } else {
            block()
        }
    }

    /**
     * for queue operations
     * detect queue changes and notify observers effectually
     */
    private inline fun snapshotAndNotify(queueHolder: QueueHolder, block: () -> Unit) {
        val oldPosition = queueHolder.currentSongPosition
        block()
        with(observerManager) {
            with(queueHolder) {
                notifyQueueChanged(playingQueue, originalPlayingQueue) // always changes
                if (oldPosition != currentSongPosition)
                    notifyCurrentPositionChanged(currentSongPosition)
            }
        }
    }


    fun cycleRepeatMode(async: Boolean = true) = async(async) {
        queueHolder.cycleRepeatMode()
        handler.post {
            observerManager.notifyRepeatModeChanged(queueHolder.repeatMode)
        }
    }

    fun toggleShuffle(
        alongWithQueue: Boolean = true,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.toggleShuffle()
        if (alongWithQueue) shuffleQueue()
        handler.post {
            observerManager.notifyShuffleModeChanged(queueHolder.shuffleMode)
        }
    }

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
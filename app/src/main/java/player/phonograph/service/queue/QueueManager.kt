/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import player.phonograph.model.service.QueueObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.recordThrowable
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import kotlin.concurrent.thread

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


        thread(name = "queue_validation", priority = THREAD_PRIORITY_BACKGROUND) {
            if (validQueueChanges(context, queueHolder)) {
                observerManager.notifyQueueChanged(queueHolder.playingQueue, queueHolder.originalPlayingQueue)
            }
        }
    }

    private fun validQueueChanges(context: Context, queueHolder: QueueHolder): Boolean = try {
        queueHolder.valid(context)
    } catch (e: Throwable) {
        recordThrowable(context, TAG, e)  // validation is optional
        false
    }

    private var snapShotsItemCount: Long = 0
    private val queueHolderSnapshots: MutableList<QueueHolder> = ArrayList()
    fun getQueueSnapShots(): List<QueueHolder> = queueHolderSnapshots.toList()

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
                MSG_SAVE_QUEUE -> queueHolder.saveQueue(context)
                MSG_SAVE_CFG   -> queueHolder.saveConfig(context)
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

    /**
     * get previous song position in CURRENT Repeat Mode behavior
     */
    val previousSongPosition: Int
        @Synchronized get() {
            val result = currentSongPosition - 1
            return when (repeatMode) {
                RepeatMode.NONE               -> if (result < 0) 0 else result
                RepeatMode.REPEAT_QUEUE       -> if (result < 0) playingQueue.size - 1 else result
                RepeatMode.REPEAT_SINGLE_SONG -> currentSongPosition
            }
        }

    /**
     * get next song position in CURRENT Repeat mode behavior.
     * returns -1 if ended
     */
    val nextSongPosition: Int
        @Synchronized get() {
            val result = currentSongPosition + 1
            return when (repeatMode) {
                RepeatMode.NONE               -> if (result >= playingQueue.size) -1 else result
                RepeatMode.REPEAT_QUEUE       -> if (result >= playingQueue.size) 0 else result
                RepeatMode.REPEAT_SINGLE_SONG -> currentSongPosition
            }
        }

    /**
     * get previous song position in order of list as a loop
     */
    val previousLoopPosition: Int
        @Synchronized get() = (currentSongPosition - 1 + playingQueue.size) % playingQueue.size
    /**
     * get next song position in order of list as a loop
     */
    val nextLoopPosition: Int
        @Synchronized get() = (currentSongPosition + 1) % playingQueue.size


    /**
     * get previous song position in order of list no mater what Repeat Mode is.
     * returns -1 if out of index
     */
    val previousListPosition: Int
        @Synchronized get() = currentSongPosition - 1
    /**
     * get next song position in order of list no mater what Repeat Mode is.
     * returns -1 if out of index
     */
    val nextListPosition: Int
        @Synchronized get() {
            val result = currentSongPosition + 1
            return if (result >= playingQueue.size) -1 else result
        }

    val currentSong: Song? get() = queueHolder.getSongAt(currentSongPosition)
    val previousSong: Song? get() = queueHolder.getSongAt(previousSongPosition)
    val nextSong: Song? get() = queueHolder.getSongAt(nextSongPosition)

    fun modifyPosition(
        newPosition: Int,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyPosition(newPosition)
        handler.post {
            observerManager.notifyCurrentPositionChanged(newPosition)
            queueHolder.saveConfig(context)
        }
    }

    /**
     * @param newShuffleMode null for cycling mode
     * @param alongWithQueue shuffle queue along as well
     */
    fun modifyShuffleMode(
        newShuffleMode: ShuffleMode? = null,
        alongWithQueue: Boolean = true,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyShuffleMode(newShuffleMode)
        if (alongWithQueue) shuffle(queueHolder, queueHolder.shuffleMode)
        handler.post {
            observerManager.notifyShuffleModeChanged(newShuffleMode ?: queueHolder.shuffleMode)
            queueHolder.saveConfig(context)
            if (alongWithQueue) queueHolder.saveQueue(context)
        }
    }

    /**
     * @param newRepeatMode null for cycling mode
     */
    fun modifyRepeatMode(
        newRepeatMode: RepeatMode?,
        async: Boolean = true,
    ) = async(async) {
        queueHolder.modifyRepeatMode(newRepeatMode)
        handler.post {
            observerManager.notifyRepeatModeChanged(newRepeatMode ?: queueHolder.repeatMode)
            queueHolder.saveConfig(context)
        }
    }


    fun isLastTrack(): Boolean = currentSongPosition == playingQueue.size - 1

    fun isQueueEnded(): Boolean = nextSongPosition == -1 && isLastTrack()

    fun getRestSongsDuration(position: Int): Long = queueHolder.getRestSongsDuration(position)


    fun swapQueue(newQueue: List<Song>, newPosition: Int, async: Boolean = true) = async(async) {
        snapshotAndNotify(queueHolder, createSnapshot = true) {
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
        snapshotAndNotify(queueHolder, true) {
            clearQueue(queueHolder)
        }
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
     * remove missing songs
     */
    fun clean() {
        handler.post {
            val changed = queueHolder.clean(context)
            if (changed) {
                observerManager.notifyQueueChanged(queueHolder.playingQueue, queueHolder.originalPlayingQueue)
                observerManager.notifyCurrentPositionChanged(queueHolder.currentSongPosition)
            }
        }
    }

    /**
     * for queue operations
     * detect queue changes and notify observers effectually
     */
    private inline fun snapshotAndNotify(
        queueHolder: QueueHolder,
        createSnapshot: Boolean = false,
        block: () -> Unit,
    ) {
        if (createSnapshot) createSnapshot()
        val oldPosition = queueHolder.currentSongPosition
        block()
        handler.post {
            with(observerManager) {
                with(queueHolder) {
                    notifyQueueChanged(playingQueue, originalPlayingQueue) // always changes
                    if (oldPosition != currentSongPosition || createSnapshot)
                        notifyCurrentPositionChanged(currentSongPosition)
                }
            }
            queueHolder.saveQueue(context)
        }
    }

    fun createSnapshot() {
        if (queueHolder.playingQueue.size <= 0) return
        synchronized(queueHolderSnapshots) {
            snapShotsItemCount += queueHolder.playingQueue.size
            queueHolderSnapshots.add(0, queueHolder.clone())
            if (queueHolderSnapshots.size > 10 || snapShotsItemCount >= 150_000) {
                val removed = queueHolderSnapshots.removeAt(queueHolderSnapshots.lastIndex)
                snapShotsItemCount -= removed.playingQueue.size
            }
        }
    }

    fun recoverSnapshot(
        newQueueHolder: QueueHolder,
        createSnapshot: Boolean = false,
        async: Boolean = true,
    ) = async(async) {
        snapshotAndNotify(queueHolder, createSnapshot) {
            queueHolder = newQueueHolder.clone()
        }
        MusicPlayerRemote.playSongAt(queueHolder.currentSongPosition)
    }


    fun addObserver(observer: QueueObserver) = observerManager.addObserver(observer)
    fun removeObserver(observer: QueueObserver) = observerManager.removeObserver(observer)

    private inner class ObserverManager {
        private val observers: MutableList<QueueObserver> = ArrayList()
        fun addObserver(observer: QueueObserver) = synchronized(observers) { observers.add(observer) }
        fun removeObserver(observer: QueueObserver): Boolean = synchronized(observers) { observers.remove(observer) }

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
            synchronized(observers) {
                for (observer in observers) {
                    block(observer)
                }
            }
        }
    }

    companion object {
        const val MSG_STATE_RESTORE_ALL = 1
        const val MSG_SAVE_QUEUE = 2
        const val MSG_SAVE_CFG = 4

        private const val TAG = "QueueManager"
    }
}
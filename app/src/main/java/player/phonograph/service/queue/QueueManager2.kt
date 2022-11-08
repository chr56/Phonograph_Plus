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

    init {
        thread.start()
        handler = QueueManagerHandler(thread.looper)
        queueHolder = QueueHolder.fromPersistence(context)
    }

    /**
     * stop internal thread and release resource
     */
    fun release() {
        handler.looper.quitSafely()
        thread.quitSafely()
        // observers.clear() //todo
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
    }

    fun modifyPosition(
        async: Boolean = true,
        newPosition: Int,
    ) = request(async) {
        queueHolder.modifyPosition(newPosition)
    }

    fun modifyShuffleMode(
        async: Boolean = true,
        newShuffleMode: ShuffleMode,
    ) = request(async) {
        queueHolder.modifyShuffleMode(newShuffleMode)
    }

    fun modifyRepeatMode(
        async: Boolean = true,
        newRepeatMode: RepeatMode,
    ) = request(async) {
        queueHolder.modifyRepeatMode(newRepeatMode)
    }


    fun isLastTrack(): Boolean = currentSongPosition == playingQueue.size - 1

    fun isQueueEnded(): Boolean = nextSongPosition == -1 && isLastTrack()


    private fun saveQueue() = queueHolder.saveQueue(context)
    private fun saveCfg() = queueHolder.saveCfg(context)
    private fun restoreAllState() = queueHolder.saveAll(context)


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

    companion object {
        const val MSG_STATE_RESTORE_ALL = 1
        const val MSG_SAVE_QUEUE = 2
        const val MSG_SAVE_CFG = 4
    }
}
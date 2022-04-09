/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.music_service

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.provider.MusicPlaybackQueueStore

class QueueManager {
    private var _context: Context?
    private val context: Context get() = _context!!

    constructor(context: Context) {
        _context = context
    }
    constructor() {
        _context = App.instance
    }

    private val handler: Handler
    private val thread: HandlerThread = HandlerThread("QueueManagerHandler", Process.THREAD_PRIORITY_BACKGROUND)
    init {
        thread.start()
        handler = Handler(thread.looper) { handleMessage(it) }
        restoreState()
    }

    /**
     * stop internal thread and release resource
     */
    fun release() {
        handler.sendMessage(Message.obtain().apply { what = MSG_STOP })
        handler.looper.quit()
        thread.quitSafely()
        _context = null
    }

    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_STOP -> {
                return true
            }
        }
        return false
    }

    fun postMessage(msg: Message) {
        handler.sendMessage(msg)
    }

    val playingQueue: List<Song> = ArrayList()
    val originalPlayingQueue: List<Song> = ArrayList()
    var currentQueueCursor = -1
        private set
    var originalQueueCursor = -1
        private set
    var mode: QueueMode = QueueMode.ORIGINAL
        private set

    fun restoreState() {
    }

    fun saveQueue() {
        MusicPlaybackQueueStore.getInstance(context).saveQueues(playingQueue, originalPlayingQueue)
    }
    fun saveCursor() {}
    fun saveCurrentTime() {}
    fun saveMode() {}

    fun saveAll() {
        saveQueue()
        saveCursor()
        saveCurrentTime()
        saveMode()
    }

    companion object {
        private const val MSG_STOP = -1
    }
}
enum class QueueMode {
    RANDOM, ORIGINAL
}

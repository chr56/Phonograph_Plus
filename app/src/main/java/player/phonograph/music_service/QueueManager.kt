/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.music_service

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import androidx.preference.PreferenceManager
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
        observers.clear()
    }

    var playingQueue: MutableList<Song> = ArrayList()
    var originalPlayingQueue: MutableList<Song> = ArrayList()
    var currentQueueCursor = -1
        @Synchronized
        private set(value) {
            field = value
            observers.executeForEach {
                onQueueCursorChanged(value)
            }
        }
    val previousSongCursor: Int get() {
        val result = currentQueueCursor - 1
        return if (result < 0) 0 else result
    }
    val nextSongCursor: Int get() {
        val result = currentQueueCursor + 1
        return if (result < 0) 0 else result
    }
    var shuffleMode: ShuffleMode = ShuffleMode.NONE
        @Synchronized
        private set(value) {
            field = value
            observers.executeForEach {
                onShuffleModeChanged(value)
            }
        }
    var repeatMode: RepeatMode = RepeatMode.NONE
        @Synchronized
        private set(value) {
            field = value
            observers.executeForEach {
                onRepeatModeChanged(value)
            }
        }

    private fun getSongAt(position: Int): Song =
        if (position >= 0 && position < playingQueue.size) {
            playingQueue[position]
        } else {
            Song.EMPTY_SONG
        }

    fun getCurrentSong(): Song = getSongAt(currentQueueCursor)
    fun getNextSong(): Song = getSongAt(nextSongCursor)
    fun getPreviousSong(): Song = getSongAt(previousSongCursor)

    fun modifyQueue(modifyWhat: ShuffleMode, action: (MutableList<Song>) -> Unit) {
        handler.post {
            modifyQueueIml(modifyWhat, action)
        }
    }

    private fun modifyQueueIml(modifyWhat: ShuffleMode, action: (MutableList<Song>) -> Unit) {
        when (modifyWhat) {
            ShuffleMode.SHUFFLE -> {
                synchronized(playingQueue) {
                    action(playingQueue)
                }
            }
            ShuffleMode.NONE -> {
                synchronized(originalPlayingQueue) {
                    action(originalPlayingQueue)
                }
            }
        }
        observers.executeForEach {
            onQueueChanged(modifyWhat, playingQueue, originalPlayingQueue)
        }
    }

    fun setQueueCursor(position: Int) {
        handler.post {
            currentQueueCursor = position
        }
    }
    fun switchShuffleMode(shuffleMode: ShuffleMode) {
        handler.post {
            this.shuffleMode = shuffleMode
        }
    }
    fun switchRepeatMode(repeatMode: RepeatMode) {
        handler.post {
            this.repeatMode = repeatMode
        }
    }

    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_STOP -> {
                return true
            }
            MSG_STATE_SAVE_ALL -> {
                saveAll()
            }
            MSG_SAVE_QUEUE -> {
                saveQueue()
            }
            MSG_SAVE_MODE -> {
                saveMode()
            }
            MSG_SAVE_CURSOR -> {
                saveCursor()
            }
            MSG_STATE_RESTORE -> {
                restoreState()
            }
        }
        return false
    }

    fun postMessage(msg: Message) {
        if (msg.what == MSG_STOP)return // stop via [this#destroy()]
        handler.sendMessage(msg)
    }

    private fun restoreState() {
        val restoredQueue = MusicPlaybackQueueStore.getInstance(context).savedPlayingQueue
        val restoredOriginalQueue = MusicPlaybackQueueStore.getInstance(context).savedOriginalPlayingQueue
        val restoredPosition = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_POSITION, -1)
        if (restoredQueue.size > 0 && restoredQueue.size == restoredOriginalQueue.size && restoredPosition != -1) {
            originalPlayingQueue = restoredOriginalQueue.toMutableList()
            playingQueue = restoredQueue.toMutableList()
            currentQueueCursor = restoredPosition
        }
        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_SHUFFLE_MODE, 0).let {
            shuffleMode = when (it) {
                SHUFFLE_MODE_SHUFFLE -> ShuffleMode.SHUFFLE
                SHUFFLE_MODE_NONE -> ShuffleMode.NONE
                else -> throw Exception("invalid shuffle mode")
            }
        }
        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_REPEAT_MODE, 0).let {
            repeatMode = when (it) {
                REPEAT_MODE_NONE -> RepeatMode.NONE
                REPEAT_MODE_ALL -> RepeatMode.REPEAT_QUEUE
                REPEAT_MODE_THIS -> RepeatMode.REPEAT_SINGLE_SONG
                else -> throw Exception("invalid repeat mode")
            }
        }
        observers.executeForEach {
            onStateRestored()
        }
    }

    private fun saveQueue() {
        MusicPlaybackQueueStore.getInstance(context).saveQueues(playingQueue, originalPlayingQueue)
    }
    private fun saveCursor() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_POSITION, currentQueueCursor).apply()
    }
    private fun saveMode() {
        val value = when (shuffleMode) {
            ShuffleMode.SHUFFLE -> SHUFFLE_MODE_SHUFFLE
            ShuffleMode.NONE -> SHUFFLE_MODE_NONE
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_SHUFFLE_MODE, value).apply()
    }

    private fun saveAll() {
        saveQueue()
        saveCursor()
        saveMode()
        observers.executeForEach {
            onStateSaved()
        }
    }

    private val observers: MutableList<QueueChangeObserver> = ArrayList()
    fun addObserver(observer: QueueChangeObserver) = observers.add(observer)
    fun removeObserver(observer: QueueChangeObserver): Boolean = observers.remove(observer)

    companion object {
        private const val MSG_STOP = -1
        const val MSG_STATE_RESTORE = 1
        const val MSG_SAVE_QUEUE = 2
        const val MSG_SAVE_CURSOR = 4
        const val MSG_SAVE_MODE = 8
        const val MSG_STATE_SAVE_ALL = 32

        const val PREF_POSITION = "POSITION"
        const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val PREF_REPEAT_MODE = "REPEAT_MODE"

        const val SHUFFLE_MODE_NONE = 0
        const val SHUFFLE_MODE_SHUFFLE = 1

        const val REPEAT_MODE_NONE = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_THIS = 2
    }
}
enum class ShuffleMode {
    SHUFFLE, NONE
}
enum class RepeatMode {
    NONE, REPEAT_QUEUE, REPEAT_SINGLE_SONG
}
interface QueueChangeObserver {
    fun onStateRestored() {}
    fun onStateSaved() {}
    fun onQueueCursorChanged(newPosition: Int) {}
    fun onQueueChanged(shuffleChanged: ShuffleMode, newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {}
    fun onShuffleModeChanged(newMode: ShuffleMode) {}
    fun onRepeatModeChanged(newMode: RepeatMode) {}
}

private fun MutableList<QueueChangeObserver>.executeForEach(
    action: QueueChangeObserver.() -> Unit
) {
    for (observer in this) {
        action(observer)
    }
}

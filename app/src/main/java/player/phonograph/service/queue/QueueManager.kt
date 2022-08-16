/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import android.app.Application
import android.os.*
import android.util.ArrayMap
import androidx.preference.PreferenceManager
import java.util.concurrent.CopyOnWriteArrayList
import player.phonograph.helper.ShuffleHelper.shuffleAt
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.provider.MusicPlaybackQueueStore

class QueueManager(val context: Application) {

    private val handler: QueueManagerHandler
    private val thread: HandlerThread = HandlerThread(
        "QueueManagerHandler",
        Process.THREAD_PRIORITY_BACKGROUND
    )

    init {
        thread.start()
        handler = QueueManagerHandler(thread.looper)
    }

    /**
     * stop internal thread and release resource
     */
    fun release() {
        handler.looper.quitSafely()
        thread.quitSafely()
        handler.looper.quit()
        thread.quitSafely()
        observers.clear()
    }

    inner class QueueManagerHandler(looper: Looper) : Handler(looper) {
        private var requestIdCumulator = 0
        private val requestList: ArrayMap<Int, Runnable> = ArrayMap(1)

        /**
         * Request running in the handler thread
         * @param request RunnableRequest: (PlayerController) -> Unit
         */
        fun request(request: Runnable) {
            val requestId = requestIdCumulator++
            synchronized(requestList) {
                requestList[requestId] = request
                sendMessage(
                    Message.obtain(this, MSG_EXECUTE_REQUEST, requestId, 0)
                )
            }
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_EXECUTE_REQUEST -> {
                    val requestId = msg.arg1
                    requestList[requestId]?.let { runnableRequest ->
                        synchronized(this) {
                            runnableRequest.run()
                        }
                        synchronized(requestList) {
                            requestList.remove(requestId)
                        }
                    }
                }
                MSG_SAVE_QUEUE -> {
                    saveQueue()
                }
                MSG_SAVE_MODE -> {
                    saveShuffleMode()
                }
                MSG_SAVE_CURSOR -> {
                    saveCursor()
                }
                MSG_STATE_RESTORE_ALL -> {
                    restoreAllState()
                }
            }
        }
    }

    private inline fun request(async: Boolean, crossinline r: () -> Unit) {
        if (async) {
            handler.request {
                r()
            }
        } else {
            r()
        }
    }

    private var _playingQueue: MutableList<Song> = CopyOnWriteArrayList()
    private var _originalPlayingQueue: MutableList<Song> = CopyOnWriteArrayList()

    val playingQueue: List<Song> get() = _playingQueue
    val originalPlayingQueue: List<Song> get() = _originalPlayingQueue

    var currentSongPosition = -1
        @Synchronized
        private set(value) {
            field = value
            observers.executeForEach {
                onQueueCursorChanged(value)
            }
        }

    @Synchronized fun modifyQueue(
        async: Boolean = true,
        action: (MutableList<Song>, MutableList<Song>) -> Unit
    ) = request(async) {
        modifyQueueImp(action)
    }

    private fun modifyQueueImp(
        action: (MutableList<Song>, MutableList<Song>) -> Unit
    ) {
        synchronized(this::class.java) {
            action(_playingQueue, _originalPlayingQueue)
        }
        observers.executeForEach {
            onQueueChanged(_playingQueue, _originalPlayingQueue)
        }
        saveQueue()
        saveCursor()
    }

    fun swapQueue(newQueue: List<Song>, newPosition: Int, async: Boolean = true) {
        if (newQueue.isNotEmpty() && newPosition in newQueue.indices) {
            modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
                _originalPlayingQueue.clear()
                _originalPlayingQueue.addAll(newQueue)
                _playingQueue.clear()
                _playingQueue.addAll(newQueue)
                if (shuffleMode == ShuffleMode.SHUFFLE) {
                    _playingQueue.shuffleAt(newPosition)
                    currentSongPosition = 0
                } else {
                    currentSongPosition = newPosition
                }
            }
        }
    }

    /**
     * get previous song position in CURRENT Repeat Mode behavior
     */
    val previousSongPosition: Int
        get() {
            val result = currentSongPosition - 1
            return when (repeatMode) {
                RepeatMode.NONE -> {
                    if (result < 0) 0 else result
                }
                RepeatMode.REPEAT_QUEUE -> {
                    if (result <= 0) _playingQueue.size - 1 else result
                }
                RepeatMode.REPEAT_SINGLE_SONG -> {
                    currentSongPosition
                }
            }
        }

    /**
     * get previous song position in order of list no mater what Repeat Mode is
     */
    val previousSongPositionInList: Int
        get() {
            val result = currentSongPosition - 1
            return if (result < 0) 0 else result
        }

    /**
     * get next song position in CURRENT Repeat mode behavior
     */
    val nextSongPosition: Int
        get() {
            val result = currentSongPosition + 1
            return when (repeatMode) {
                RepeatMode.NONE -> {
                    if (result >= _playingQueue.size) {
                        -1
                    } else {
                        result
                    }
                }
                RepeatMode.REPEAT_QUEUE -> {
                    if (result >= _playingQueue.size) 0 else result
                }
                RepeatMode.REPEAT_SINGLE_SONG -> {
                    currentSongPosition
                }
            }
        }

    /**
     * get next song position in order of list no mater what Repeat Mode is
     */
    val nextSongPositionInList: Int
        get() {
            val result = currentSongPosition + 1
            return if (result >= _playingQueue.size) -1 else result
        }

    var shuffleMode: ShuffleMode = ShuffleMode.NONE
        @Synchronized
        private set(value) {
            field = value
            applyNewShuffleMode(value)
            observers.executeForEach {
                onShuffleModeChanged(value)
            }
            handler.post {
                saveShuffleMode()
            }
        }
    var repeatMode: RepeatMode = RepeatMode.NONE
        @Synchronized
        private set(value) {
            field = value
            observers.executeForEach {
                onRepeatModeChanged(value)
            }
            handler.post {
                saveRepeatMode()
            }
        }

    private fun applyNewShuffleMode(newShuffleMode: ShuffleMode) {
        synchronized(shuffleMode) {
            when (newShuffleMode) {
                ShuffleMode.SHUFFLE -> {
                    _playingQueue.shuffleAt(currentSongPosition)
                    currentSongPosition = 0
                }
                ShuffleMode.NONE -> {
                    val currentSongId = currentSong.id
                    _playingQueue.clear()
                    _playingQueue.addAll(_originalPlayingQueue)
                    for (song in _playingQueue) {
                        if (song.id == currentSongId) {
                            currentSongPosition = _playingQueue.indexOf(song)
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * Get a song safely in current queue
     */
    @Synchronized fun getSongAt(position: Int): Song =
        if (position >= 0 && position < _playingQueue.size) {
            playingQueue[position]
        } else {
            Song.EMPTY_SONG
        }

    val currentSong: Song get() = getSongAt(currentSongPosition)
    val nextSong: Song get() = getSongAt(nextSongPosition)
    val previousSong: Song get() = getSongAt(previousSongPosition)

    @JvmOverloads
    fun addSong(song: Song, position: Int = -1, async: Boolean = true) {
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            if (position < 0) {
                _playingQueue.add(song)
                _originalPlayingQueue.add(song)
            } else {
                _playingQueue.add(position, song)
                _originalPlayingQueue.add(position, song)
            }
        }
    }

    @JvmOverloads
    fun addSongs(songs: List<Song>, position: Int = -1, async: Boolean = true) {
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            if (position < 0) {
                _playingQueue.addAll(songs)
                _originalPlayingQueue.addAll(songs)
            } else {
                _playingQueue.addAll(position, songs)
                _originalPlayingQueue.addAll(position, songs)
            }
        }
    }

    fun removeSongAt(position: Int, async: Boolean = true) {
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            if (position in _originalPlayingQueue.indices) {
                if (shuffleMode == ShuffleMode.NONE) {
                    _playingQueue.removeAt(position)
                    _originalPlayingQueue.removeAt(position)
                } else {
                    _originalPlayingQueue.remove(_playingQueue.removeAt(position))
                }
                rePosition(position)
            } else {
                ErrorNotification.postErrorNotification(
                    "Warning: removing song at position$position,but we only have ${_originalPlayingQueue.size} songs"
                )
            }
        }
    }
    fun removeSong(song: Song, async: Boolean = true) {
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            for (i in _playingQueue.indices) {
                if (_playingQueue[i].id == song.id) {
                    _playingQueue.removeAt(i)
                    rePosition(i)
                }
            }
            for (i in playingQueue.indices) {
                if (_originalPlayingQueue[i].id == song.id) {
                    _originalPlayingQueue.removeAt(i)
                }
            }
            _playingQueue.remove(song)
            _originalPlayingQueue.remove(song)
        }
    }

    /**
     * Change current position after deletion
     */
    private fun rePosition(deletedSongPosition: Int) {
        if (deletedSongPosition < currentSongPosition) {
            currentSongPosition -= 1
        } else if (deletedSongPosition == currentSongPosition) {
            if (playingQueue.size > deletedSongPosition) {
                currentSongPosition = currentSongPosition
            } else {
                currentSongPosition -= 1
            }
        }
    }

    fun moveSong(from: Int, to: Int, async: Boolean = true) {
        if (from == to) return
        if (from !in _originalPlayingQueue.indices || to !in _originalPlayingQueue.indices) {
            ErrorNotification.postErrorNotification("Warning: from $from to $to is outrage ")
            return
        }
        // start moving
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            val oldPosition: Int = currentSongPosition
            val songToMove: Song = _playingQueue.removeAt(from)
            _playingQueue.add(to, songToMove)
            if (shuffleMode == ShuffleMode.NONE) {
                val tmpSong: Song = _originalPlayingQueue.removeAt(from)
                _originalPlayingQueue.add(to, tmpSong)
            }
            currentSongPosition =
                when {
                    from == oldPosition -> to
                    oldPosition in to until from -> oldPosition + 1
                    oldPosition in (from + 1)..to -> oldPosition - 1
                    else -> oldPosition
                }
        }
    }

    fun clearQueue(async: Boolean = true) {
        modifyQueue(async) { _playingQueue, _originalPlayingQueue ->
            _playingQueue.clear()
            _originalPlayingQueue.clear()
            rePosition(-1)
        }
    }

    fun setSongPosition(position: Int, async: Boolean = false) =
        request(async) {
            currentSongPosition = position
        }

    fun moveToNextSong(async: Boolean = false) =
        request(async) {
            currentSongPosition = nextSongPosition
        }

    fun switchShuffleMode(shuffleMode: ShuffleMode, async: Boolean = false) =
        request(async) {
            this.shuffleMode = shuffleMode
        }

    fun switchRepeatMode(repeatMode: RepeatMode, async: Boolean = false) =
        request(async) {
            this.repeatMode = repeatMode
        }

    /**
     * send empty message to Handler
     */
    fun postMessage(what: Int): Boolean =
        handler.sendEmptyMessage(what)

    /**
     * synchronized
     */
    private fun restoreAllState() {
        val restoredQueue = MusicPlaybackQueueStore.getInstance(context).savedPlayingQueue
        val restoredOriginalQueue = MusicPlaybackQueueStore.getInstance(context).savedOriginalPlayingQueue
        val restoredPosition = PreferenceManager
            .getDefaultSharedPreferences(context).getInt(PREF_POSITION, -1)

        if (restoredQueue.isNotEmpty() && restoredQueue.size == restoredOriginalQueue.size && restoredPosition != -1) {
            _originalPlayingQueue = restoredOriginalQueue.toMutableList()
            _playingQueue = restoredQueue.toMutableList()
            currentSongPosition = restoredPosition
        }

        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_SHUFFLE_MODE, 0).let {
            shuffleMode = ShuffleMode.deserialize(it)
        }
        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_REPEAT_MODE, 0).let {
            repeatMode = RepeatMode.deserialize(it)
        }

        observers.executeForEach {
            onStateRestored()
        }
    }

    /**
     * synchronized
     */
    private fun saveQueue() {
        MusicPlaybackQueueStore.getInstance(context).saveQueues(
            _playingQueue,
            _originalPlayingQueue
        )
    }

    /**
     * synchronized
     */
    private fun saveCursor() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(
            PREF_POSITION,
            currentSongPosition
        ).apply()
    }

    /**
     * synchronized
     */
    private fun saveShuffleMode() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(
            PREF_SHUFFLE_MODE,
            shuffleMode.serialize()
        ).apply()
    }

    /**
     * synchronized
     */
    private fun saveRepeatMode() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(
            PREF_REPEAT_MODE,
            repeatMode.serialize()
        ).apply()
    }

    /**
     * synchronized
     */
    private fun saveAll() {
        saveQueue()
        saveCursor()
        saveShuffleMode()
        saveRepeatMode()
        observers.executeForEach {
            onStateSaved()
        }
    }

    fun isLastTrack(): Boolean = currentSongPosition == playingQueue.size - 1

    fun isQueueEnded(): Boolean = nextSongPosition == -1 && isLastTrack()

    fun getAllSongsDuration(): Long =
        _originalPlayingQueue.fold(0L) { acc, song: Song -> acc + song.duration }

    fun getRestSongsDuration(position: Int): Long =
        _playingQueue.takeLast(getRestSongsCount(position))
            .fold(0L) { acc, song -> acc + song.duration }
    private fun getRestSongsCount(currentPosition: Int): Int =
        if (_playingQueue.isEmpty() || _playingQueue.size - currentPosition < 0) 0
        else _playingQueue.size - currentPosition

    /**
     * synchronized
     */
    fun cycleRepeatMode() {
        switchRepeatMode(
            when (repeatMode) {
                RepeatMode.NONE -> RepeatMode.REPEAT_QUEUE
                RepeatMode.REPEAT_QUEUE -> RepeatMode.REPEAT_SINGLE_SONG
                RepeatMode.REPEAT_SINGLE_SONG -> RepeatMode.NONE
            }
        )
    }

    /**
     * synchronized
     */
    fun toggleShuffle() {
        switchShuffleMode(
            when (shuffleMode) {
                ShuffleMode.NONE -> ShuffleMode.SHUFFLE
                ShuffleMode.SHUFFLE -> ShuffleMode.NONE
            }
        )
    }

    private val observers: MutableList<QueueChangeObserver> = ArrayList()
    fun addObserver(observer: QueueChangeObserver) = observers.add(observer)
    fun removeObserver(observer: QueueChangeObserver): Boolean = observers.remove(observer)

    companion object {
        const val MSG_STATE_RESTORE_ALL = 1
        const val MSG_SAVE_QUEUE = 2
        const val MSG_SAVE_CURSOR = 4
        const val MSG_SAVE_MODE = 8

        const val PREF_POSITION = "POSITION"
        const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val PREF_REPEAT_MODE = "REPEAT_MODE"

        const val MSG_EXECUTE_REQUEST = 10
    }
}

/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import androidx.preference.PreferenceManager
import java.util.concurrent.CopyOnWriteArrayList
import player.phonograph.helper.ShuffleHelper.shuffleAt
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.provider.MusicPlaybackQueueStore

class QueueManager(val context: Application) {

    private val handler: Handler
    private val thread: HandlerThread = HandlerThread("QueueManagerHandler", Process.THREAD_PRIORITY_BACKGROUND)

    init {
        thread.start()
        handler = Handler(thread.looper, this::handleMessage)
    }

    /**
     * stop internal thread and release resource
     */
    fun release() {
        handler.sendMessage(Message.obtain().apply { what = MSG_STOP })
        handler.looper.quit()
        thread.quitSafely()
        observers.clear()
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

    /**
     * get previous position song in CURRENT Repeat mode behavior
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
     * get next song position in CURRENT Repeat mode behavior
     */
    val nextSongPosition: Int
        get() {
            val result = currentSongPosition + 1
            return when (repeatMode) {
                RepeatMode.NONE -> {
                    if (result >= _playingQueue.size) {
                        // todo
//                        service.queueSaveHandlerThread sendMessage(
//                            Message.obtain().apply { what = MusicService.TRACK_ENDED }
//                            )
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
            // todo notify player
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
    fun getSongAt(position: Int): Song =
        if (position >= 0 && position < _playingQueue.size) {
            playingQueue[position]
        } else {
            Song.EMPTY_SONG
        }

    val currentSong: Song get() = getSongAt(currentSongPosition)
    val nextSong: Song get() = getSongAt(nextSongPosition)
    val previousSong: Song get() = getSongAt(previousSongPosition)

    val lastTrack: Boolean = currentSongPosition == _playingQueue.size - 1

    @JvmOverloads
    fun addSong(song: Song, position: Int = -1) {
        modifyQueue { _playingQueue, _originalPlayingQueue ->
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
    fun addSongs(songs: List<Song>, position: Int = -1) {
        modifyQueue { _playingQueue, _originalPlayingQueue ->
            if (position < 0) {
                _playingQueue.addAll(songs)
                _originalPlayingQueue.addAll(songs)
            } else {
                _playingQueue.addAll(position, songs)
                _originalPlayingQueue.addAll(position, songs)
            }
        }
    }

    fun removeSongAt(position: Int) {
        modifyQueue { _playingQueue, _originalPlayingQueue ->
            if (position in _originalPlayingQueue.indices) {
                if (shuffleMode == ShuffleMode.NONE) {
                    _playingQueue.removeAt(position)
                    _originalPlayingQueue.removeAt(position)
                } else {
                    _originalPlayingQueue.remove(_playingQueue.removeAt(position))
                }
                rePosition(position)
            } else {
                ErrorNotification.postErrorNotification("Warning: removing song at position$position,but we only have ${_originalPlayingQueue.size} songs")
            }
        }
    }
    fun removeSong(song: Song) {
        modifyQueue { _playingQueue, _originalPlayingQueue ->
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
            // todo
            if (playingQueue.size > deletedSongPosition) {
                currentSongPosition = currentSongPosition
            } else {
                currentSongPosition -= 1
            }
        }
    }

    fun moveSong(from: Int, to: Int) {
        if (from == to) return
        if (from !in _originalPlayingQueue.indices || to !in _originalPlayingQueue.indices) {
            ErrorNotification.postErrorNotification("Warning: from $from to $to is outrage ")
            return
        }
        // start moving
        modifyQueue { _playingQueue, _originalPlayingQueue ->
            val currentPosition: Int = currentSongPosition
            val songToMove: Song = _playingQueue.removeAt(from)
            _playingQueue.add(to, songToMove)
            if (shuffleMode == ShuffleMode.NONE) {
                val tmpSong: Song = _originalPlayingQueue.removeAt(from)
                _originalPlayingQueue.add(to, tmpSong)
            }
            currentSongPosition =
                when {
                    currentPosition in to until from -> currentPosition + 1
                    currentPosition in (from + 1)..to -> currentPosition - 1
                    from == currentPosition -> to
                    else -> from
                }
        }
    }

    fun clearQueue() {
        modifyQueue { _playingQueue, _originalPlayingQueue ->
            _playingQueue.clear()
            _originalPlayingQueue.clear()
            rePosition(-1)
        }
    }

    fun swapQueue(newQueue: List<Song>, newPosition: Int) {
        if (newQueue.isNotEmpty() && newPosition in newQueue.indices) {
            modifyQueue { _playingQueue, _originalPlayingQueue ->
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
                // todo notify player
            }
        }
    }

    fun modifyQueue(action: (MutableList<Song>, MutableList<Song>) -> Unit) {
        handler.post {
            synchronized(_playingQueue) {
                action(_playingQueue, _originalPlayingQueue)
            }
            observers.executeForEach {
                onQueueChanged(_playingQueue, _originalPlayingQueue)
            }
            updateMeta()
            saveQueue()
            saveCursor()
        }
    }

    private fun updateMeta() {
        // todo
    }

    fun setQueueCursor(position: Int) {
        handler.post {
            currentSongPosition = position
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

    /**
     * [Handler.Callback] for QueueManager's [handler]
     */
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
                saveShuffleMode()
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

    fun postMessage(what: Int) {
        postMessage(Message.obtain(handler, what))
    }

    fun postMessage(msg: Message) {
        if (msg.what == MSG_STOP) return /** stop via [release]**/
        handler.sendMessage(msg)
    }

    private fun restoreState() {
        val restoredQueue = MusicPlaybackQueueStore.getInstance(context).savedPlayingQueue
        val restoredOriginalQueue = MusicPlaybackQueueStore.getInstance(context).savedOriginalPlayingQueue
        val restoredPosition = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_POSITION, -1)
        if (restoredQueue.isNotEmpty() && restoredQueue.size == restoredOriginalQueue.size && restoredPosition != -1) {
            _originalPlayingQueue = restoredOriginalQueue.toMutableList()
            _playingQueue = restoredQueue.toMutableList()
            currentSongPosition = restoredPosition
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
        MusicPlaybackQueueStore.getInstance(context).saveQueues(_playingQueue, _originalPlayingQueue)
    }

    private fun saveCursor() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_POSITION, currentSongPosition).apply()
    }

    private fun saveShuffleMode() {
        val value = when (shuffleMode) {
            ShuffleMode.SHUFFLE -> SHUFFLE_MODE_SHUFFLE
            ShuffleMode.NONE -> SHUFFLE_MODE_NONE
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_SHUFFLE_MODE, value).apply()
    }

    private fun saveRepeatMode() {
        val value = when (repeatMode) {
            RepeatMode.NONE -> REPEAT_MODE_NONE
            RepeatMode.REPEAT_QUEUE -> REPEAT_MODE_ALL
            RepeatMode.REPEAT_SINGLE_SONG -> REPEAT_MODE_THIS
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_REPEAT_MODE, value).apply()
    }

    private fun saveAll() {
        saveQueue()
        saveCursor()
        saveShuffleMode()
        saveRepeatMode()
        observers.executeForEach {
            onStateSaved()
        }
    }

    fun isLastTrack(): Boolean {
        return currentSongPosition == playingQueue.size - 1
    }

    fun getAllSongsDuration(): Long =
        _originalPlayingQueue.fold(0L) { acc, song: Song -> acc + song.duration }

    fun cycleRepeatMode() {
        switchRepeatMode(
            when (repeatMode) {
                RepeatMode.NONE -> RepeatMode.REPEAT_QUEUE
                RepeatMode.REPEAT_QUEUE -> RepeatMode.REPEAT_SINGLE_SONG
                RepeatMode.REPEAT_SINGLE_SONG -> RepeatMode.NONE
            }
        )
    }

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

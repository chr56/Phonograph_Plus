/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import org.koin.core.context.GlobalContext
import player.phonograph.model.PlayRequest.SongsRequest
import player.phonograph.model.Song
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.text.totalDuration
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList

class QueueHolder private constructor(
    playing: List<Song>,
    original: List<Song>,
    position: Int,
    shuffle: ShuffleMode,
    repeat: RepeatMode,
) {
    var playingQueue: MutableList<Song> = CopyOnWriteArrayList(playing)
        private set
    var originalPlayingQueue: MutableList<Song> = CopyOnWriteArrayList(original)
        private set
    private val queueLock = Any()

    var currentSongPosition: Int = position
        private set
    private val positionLock = Any()

    var shuffleMode: ShuffleMode = shuffle
        private set
    var repeatMode: RepeatMode = repeat
        private set

    /**
     * synchronized
     */
    fun modifyQueue(
        action: (MutableList<Song>, MutableList<Song>) -> Unit,
    ) = synchronized(queueLock) {
        action(playingQueue, originalPlayingQueue)
    }

    /**
     * synchronized
     */
    fun modifyPosition(newPosition: Int) = synchronized(positionLock) {
        currentSongPosition = newPosition
    }

    /**
     * synchronized
     */
    fun modifyShuffleMode(newShuffleMode: ShuffleMode) = synchronized(shuffleMode) {
        shuffleMode = newShuffleMode
    }

    /**
     * synchronized
     */
    fun modifyRepeatMode(newRepeatMode: RepeatMode) = synchronized(repeatMode) {
        repeatMode = newRepeatMode
    }


    fun getSongAt(position: Int): Song? =
        if (position >= 0 && position < playingQueue.size) playingQueue[position] else null

    /**
     * synchronized
     */
    fun cycleRepeatMode() {
        modifyRepeatMode(
            when (repeatMode) {
                RepeatMode.NONE               -> RepeatMode.REPEAT_QUEUE
                RepeatMode.REPEAT_QUEUE       -> RepeatMode.REPEAT_SINGLE_SONG
                RepeatMode.REPEAT_SINGLE_SONG -> RepeatMode.NONE
            }
        )
    }

    /**
     * synchronized
     */
    fun toggleShuffle() {
        modifyShuffleMode(
            when (shuffleMode) {
                ShuffleMode.NONE    -> ShuffleMode.SHUFFLE
                ShuffleMode.SHUFFLE -> ShuffleMode.NONE
            }
        )
    }

    fun getRestSongsDuration(position: Int): Long =
        totalDuration(playingQueue.takeLast(getRestSongsCount(position)))

    private fun getRestSongsCount(currentPosition: Int): Int =
        if (playingQueue.isEmpty() || playingQueue.size - currentPosition < 0) 0
        else playingQueue.size - currentPosition


    fun saveAll(context: Context) {
        saveQueue(context)
        saveCfg(context)
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveQueue(context: Context) = synchronized(persistenceLock) {
        GlobalContext.get().get<MusicPlaybackQueueStore>().saveQueues(playingQueue, originalPlayingQueue)
    }

    fun saveCfg(context: Context) = synchronized(persistenceLock) {
        val queuePreferenceManager = QueuePreferenceManager(context)
        queuePreferenceManager.currentPosition = currentSongPosition
        queuePreferenceManager.repeatMode = repeatMode
        queuePreferenceManager.shuffleMode = shuffleMode
    }

    @Suppress("UNCHECKED_CAST")
    fun valid(context: Context): Boolean {
        val previousPlayingQueue =
            (playingQueue as CopyOnWriteArrayList<Song>).clone() as List<Song>
        val previousOriginalPlayingQueue =
            (originalPlayingQueue as CopyOnWriteArrayList<Song>).clone() as List<Song>
        return runBlocking {
            val validatedQueue = QueueValidator.markInvalidSongs(context, previousPlayingQueue)
            val validatedOriginalQueue = QueueValidator.markInvalidSongs(context, previousOriginalPlayingQueue)
            val changed =
                validatedQueue != previousPlayingQueue || validatedOriginalQueue != previousOriginalPlayingQueue
            synchronized(queueLock) {
                if (
                    previousPlayingQueue == playingQueue && previousOriginalPlayingQueue == originalPlayingQueue // avoid data race
                ) {
                    if (changed) {
                        playingQueue = CopyOnWriteArrayList(validatedQueue)
                        originalPlayingQueue = CopyOnWriteArrayList(validatedOriginalQueue)
                    }
                } // cancel if user changes queue before validation
            }
            changed
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun clean(context: Context): Boolean {
        val previousPlayingQueue = (playingQueue as CopyOnWriteArrayList<Song>).clone() as List<Song>
        val previousOriginalPlayingQueue = (originalPlayingQueue as CopyOnWriteArrayList<Song>).clone() as List<Song>
        val position = currentSongPosition
        return runBlocking {
            val queue = QueueValidator.removeMissingSongs(context, SongsRequest(previousPlayingQueue, position))
            val origin = QueueValidator.removeMissingSongs(context, SongsRequest(previousOriginalPlayingQueue, 0))
            synchronized(queueLock) {
                playingQueue = CopyOnWriteArrayList(queue.songs)
                originalPlayingQueue = CopyOnWriteArrayList(origin.songs)
                modifyPosition(queue.position)
            }
            previousPlayingQueue.size != queue.songs.size
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun clone(): QueueHolder = QueueHolder(
        (playingQueue as CopyOnWriteArrayList).clone() as List<Song>,
        (originalPlayingQueue as CopyOnWriteArrayList).clone() as List<Song>,
        currentSongPosition,
        shuffleMode,
        repeatMode
    )

    val snapshotTime: Long = currentTimestamp()

    companion object {
        private val persistenceLock = Any()
        fun fromPersistence(context: Context): QueueHolder {
            synchronized(persistenceLock) {

                val queueStore = GlobalContext.get().get<MusicPlaybackQueueStore>()
                val restoredQueue: List<Song> = queueStore.savedPlayingQueue
                val restoredOriginalQueue: List<Song> = queueStore.savedOriginalPlayingQueue

                val preferenceManager = QueuePreferenceManager(context)

                return QueueHolder(
                    restoredQueue,
                    restoredOriginalQueue,
                    preferenceManager.currentPosition,
                    preferenceManager.shuffleMode,
                    preferenceManager.repeatMode
                )
            }
        }

        private var _coroutineScope: CoroutineScope? = null
        private val coroutineScope: CoroutineScope
            get() {
                val scope = _coroutineScope
                return if (scope != null && scope.isActive) {
                    scope
                } else {
                    CoroutineScope(Dispatchers.IO).also { _coroutineScope = it }
                }
            }
    }
}
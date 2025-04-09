/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import org.koin.core.context.GlobalContext
import player.phonograph.model.PlayRequest.SongsRequest
import player.phonograph.model.Song
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.util.text.currentTimestamp
import android.content.Context
import kotlinx.coroutines.runBlocking

class QueueHolder private constructor(
    playing: List<Song>,
    original: List<Song>,
    position: Int,
    shuffle: ShuffleMode,
    repeat: RepeatMode,
) {
    var playingQueue: List<Song> = playing
        private set
    var originalPlayingQueue: List<Song> = original
        private set
    private val queueLock = Any()

    var currentSongPosition: Int = position
        private set
    private val positionLock = Any()

    var shuffleMode: ShuffleMode = shuffle
        private set
    var repeatMode: RepeatMode = repeat
        private set

    data class QueuesAndPosition(
        val playingQueue: List<Song>,
        val originalPlayingQueue: List<Song>,
        val currentSongPosition: Int,
    )

    /**
     * modify queues and position
     * (synchronized)
     */
    fun modify(
        action: (QueuesAndPosition) -> QueuesAndPosition,
    ) = synchronized(queueLock) {
        val result = action(QueuesAndPosition(playingQueue, originalPlayingQueue, currentSongPosition))
        playingQueue = result.playingQueue.toMutableList()
        originalPlayingQueue = result.originalPlayingQueue.toMutableList()
        synchronized(positionLock) { currentSongPosition = result.currentSongPosition }
    }

    /**
     * modify position only
     * (synchronized)
     */
    fun modifyPosition(newPosition: Int) = synchronized(positionLock) {
        currentSongPosition = newPosition
    }

    /**
     * synchronized
     * @param newShuffleMode null for cycling mode
     */
    fun modifyShuffleMode(newShuffleMode: ShuffleMode?) = synchronized(shuffleMode) {
        shuffleMode = newShuffleMode ?: when (shuffleMode) {
            ShuffleMode.NONE    -> ShuffleMode.SHUFFLE
            ShuffleMode.SHUFFLE -> ShuffleMode.NONE
        }
    }

    /**
     * synchronized
     * @param newRepeatMode null for cycling mode
     */
    fun modifyRepeatMode(newRepeatMode: RepeatMode?) = synchronized(repeatMode) {
        repeatMode = newRepeatMode ?: when (repeatMode) {
            RepeatMode.NONE               -> RepeatMode.REPEAT_QUEUE
            RepeatMode.REPEAT_QUEUE       -> RepeatMode.REPEAT_SINGLE_SONG
            RepeatMode.REPEAT_SINGLE_SONG -> RepeatMode.NONE
        }
    }

    fun getSongAt(position: Int): Song = playingQueue.getOrElse(position) { Song.EMPTY_SONG }
    val currentSong: Song = getSongAt(currentSongPosition)

    fun getRestSongsDuration(position: Int): Long =
        playingQueue.takeLast(getRestSongsCount(position))
            .fold(0L) { acc, song -> acc + song.duration }

    private fun getRestSongsCount(currentPosition: Int): Int =
        if (playingQueue.isEmpty() || playingQueue.size - currentPosition < 0) 0
        else playingQueue.size - currentPosition


    fun saveAll(context: Context) {
        saveQueue(context)
        saveConfig(context)
    }

    fun saveQueue(context: Context) = synchronized(persistenceLock) {
        GlobalContext.get().get<MusicPlaybackQueueStore>().saveQueues(playingQueue, originalPlayingQueue)
    }

    fun saveConfig(context: Context) = synchronized(persistenceLock) {
        val queuePreferenceManager = QueuePreferenceManager(context)
        queuePreferenceManager.currentPosition = currentSongPosition
        queuePreferenceManager.repeatMode = repeatMode
        queuePreferenceManager.shuffleMode = shuffleMode
    }

    @Suppress("UNCHECKED_CAST")
    fun valid(context: Context): Boolean {
        val previousPlayingQueue = playingQueue.toList()
        val previousOriginalPlayingQueue = originalPlayingQueue.toList()
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
                        playingQueue = validatedQueue
                        originalPlayingQueue = validatedOriginalQueue
                    }
                } // cancel if user changes queue before validation
            }
            changed
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun clean(context: Context): Boolean {
        val previousPlayingQueue = playingQueue.toList()
        val previousOriginalPlayingQueue = originalPlayingQueue.toList()
        val position = currentSongPosition
        return runBlocking {
            val queue = QueueValidator.removeMissingSongs(context, SongsRequest(previousPlayingQueue, position))
            val origin = QueueValidator.removeMissingSongs(context, SongsRequest(previousOriginalPlayingQueue, 0))
            synchronized(queueLock) {
                playingQueue = queue.songs
                originalPlayingQueue = origin.songs
                modifyPosition(queue.position)
            }
            previousPlayingQueue.size != queue.songs.size
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun clone(): QueueHolder = QueueHolder(
        playingQueue.toList(),
        originalPlayingQueue.toList(),
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

    }
}
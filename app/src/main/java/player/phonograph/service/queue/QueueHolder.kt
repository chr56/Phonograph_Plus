/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import player.phonograph.provider.MusicPlaybackQueueStore
import androidx.preference.PreferenceManager
import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

class QueueHolder private constructor(
    playing: List<Song>,
    original: List<Song>,
    position: Int,
    shuffle: ShuffleMode,
    repeat: RepeatMode,
) {
    var playingQueue: MutableList<Song> = CopyOnWriteArrayList(playing)
    var originalPlayingQueue: MutableList<Song> = CopyOnWriteArrayList(original)
    private val queueLock = Any()

    var currentSongPosition: Int = position
    private val positionLock = Any()

    var shuffleMode: ShuffleMode = shuffle
    var repeatMode: RepeatMode = repeat


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
                    if (result <= 0) playingQueue.size - 1 else result
                }
                RepeatMode.REPEAT_SINGLE_SONG -> {
                    currentSongPosition
                }
            }
        }

    /**
     * get previous song position in order of list no mater what Repeat Mode is
     */
    val previousListPosition: Int
        get() = currentSongPosition - 1

    /**
     * get next song position in CURRENT Repeat mode behavior
     */
    val nextSongPosition: Int
        get() {
            val result = currentSongPosition + 1
            return when (repeatMode) {
                RepeatMode.NONE -> {
                    if (result >= playingQueue.size) {
                        -1
                    } else {
                        result
                    }
                }
                RepeatMode.REPEAT_QUEUE -> {
                    if (result >= playingQueue.size) 0 else result
                }
                RepeatMode.REPEAT_SINGLE_SONG -> {
                    currentSongPosition
                }
            }
        }

    /**
     * get next song position in order of list no mater what Repeat Mode is
     */
    val nextListPosition: Int
        get() {
            val result = currentSongPosition + 1
            return if (result >= playingQueue.size) -1 else result
        }

    fun getSongAt(position: Int): Song =
        if (position >= 0 && position < playingQueue.size) {
            playingQueue[position]
        } else {
            Song.EMPTY_SONG
        }

    val currentSong: Song get() = getSongAt(currentSongPosition)
    val nextSong: Song get() = getSongAt(nextSongPosition)
    val previousSong: Song get() = getSongAt(previousSongPosition)

    /**
     * synchronized
     */
    fun cycleRepeatMode() {
        modifyRepeatMode(
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
        modifyShuffleMode(
            when (shuffleMode) {
                ShuffleMode.NONE -> ShuffleMode.SHUFFLE
                ShuffleMode.SHUFFLE -> ShuffleMode.NONE
            }
        )
    }

    fun getRestSongsDuration(position: Int): Long =
        playingQueue.takeLast(getRestSongsCount(position))
            .fold(0L) { acc, song -> acc + song.duration }
    private fun getRestSongsCount(currentPosition: Int): Int =
        if (playingQueue.isEmpty() || playingQueue.size - currentPosition < 0) 0
        else playingQueue.size - currentPosition


    fun saveAll(context: Context) {
        saveQueue(context)
        saveCfg(context)
    }

    fun saveQueue(context: Context) = synchronized(persistenceLock) {
        MusicPlaybackQueueStore.getInstance(context)
            .saveQueues(playingQueue, originalPlayingQueue)
    }

    fun saveCfg(context: Context) = synchronized(persistenceLock) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
            putInt(PREF_POSITION, currentSongPosition)
            putInt(PREF_SHUFFLE_MODE, shuffleMode.serialize())
            putInt(PREF_REPEAT_MODE, repeatMode.serialize())
        }.apply()
    }

    @Suppress("UNCHECKED_CAST")
    fun clone(): QueueHolder = QueueHolder(
        (playingQueue as CopyOnWriteArrayList).clone() as List<Song>,
        (originalPlayingQueue as CopyOnWriteArrayList).clone() as List<Song>,
        currentSongPosition,
        shuffleMode,
        repeatMode
    )

    companion object {
        private val persistenceLock = Any()
        fun fromPersistence(context: Context): QueueHolder {
            synchronized(persistenceLock) {

                val restoredQueue: List<Song> =
                    MusicPlaybackQueueStore.getInstance(context).savedPlayingQueue
                val restoredOriginalQueue: List<Song> =
                    MusicPlaybackQueueStore.getInstance(context).savedOriginalPlayingQueue


                val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)

                val restoredPosition: Int = preferenceManager.getInt(PREF_POSITION, -1)

                val shuffle: ShuffleMode =
                    preferenceManager.getInt(PREF_SHUFFLE_MODE, 0).let {
                        ShuffleMode.deserialize(it)
                    }

                val repeat: RepeatMode =
                    preferenceManager.getInt(PREF_REPEAT_MODE, 0).let {
                        RepeatMode.deserialize(it)
                    }

                return QueueHolder(restoredQueue,
                                   restoredOriginalQueue,
                                   restoredPosition,
                                   shuffle,
                                   repeat)
            }
        }

        const val PREF_POSITION = "POSITION"
        const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val PREF_REPEAT_MODE = "REPEAT_MODE"
    }
}
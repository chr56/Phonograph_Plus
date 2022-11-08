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
    var currentSongPosition: Int = position
    var shuffleMode: ShuffleMode = shuffle
    var repeatMode: RepeatMode = repeat

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
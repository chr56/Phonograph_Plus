/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.warning
import kotlin.random.Random



fun swapQueue(queueHolder: QueueHolder, newQueue: List<Song>, newPosition: Int) {
    if (newQueue.isNotEmpty() && newPosition in newQueue.indices) {
        queueHolder.modify {
            if (queueHolder.shuffleMode == ShuffleMode.SHUFFLE) {
                QueueHolder.QueuesAndPosition(
                    playingQueue = shuffle(newQueue, newPosition),
                    originalPlayingQueue = newQueue,
                    currentSongPosition = 0
                )
            } else {
                QueueHolder.QueuesAndPosition(
                    playingQueue = newQueue,
                    originalPlayingQueue = newQueue,
                    currentSongPosition = newPosition
                )
            }
        }
    } else {
        warning(TAG, "Illegal queue or position")
    }
}

fun addSong(queueHolder: QueueHolder, song: Song, position: Int = -1) {
    queueHolder.modify { original ->
        if (position !in original.originalPlayingQueue.indices) {
            QueueHolder.QueuesAndPosition(
                playingQueue = original.playingQueue + song,
                originalPlayingQueue = original.originalPlayingQueue + song,
                currentSongPosition = original.currentSongPosition
            )
        } else {
            val playingQueue = original.playingQueue.toMutableList().apply { add(position, song) }
            val originalPlayingQueue = original.originalPlayingQueue.toMutableList().apply { add(position, song) }
            val newPosition = if (position < original.currentSongPosition) {
                original.currentSongPosition + 1
            } else {
                original.currentSongPosition
            }
            QueueHolder.QueuesAndPosition(
                playingQueue = playingQueue,
                originalPlayingQueue = originalPlayingQueue,
                currentSongPosition = newPosition
            )
        }
    }
}

fun addSongs(queueHolder: QueueHolder, songs: List<Song>, position: Int = -1) {
    queueHolder.modify { original ->
        if (position !in original.originalPlayingQueue.indices) {
            QueueHolder.QueuesAndPosition(
                playingQueue = original.playingQueue + songs,
                originalPlayingQueue = original.originalPlayingQueue + songs,
                currentSongPosition = original.currentSongPosition
            )
        } else {
            val playingQueue = original.playingQueue.toMutableList().apply { addAll(position, songs) }
            val originalPlayingQueue = original.originalPlayingQueue.toMutableList().apply { addAll(position, songs) }
            val newPosition = if (position < original.currentSongPosition) {
                original.currentSongPosition + songs.size
            } else {
                original.currentSongPosition
            }
            QueueHolder.QueuesAndPosition(
                playingQueue = playingQueue,
                originalPlayingQueue = originalPlayingQueue,
                currentSongPosition = newPosition
            )
        }
    }
}

fun removeSongAt(queueHolder: QueueHolder, position: Int) {
    queueHolder.modify { original ->
        if (position in original.originalPlayingQueue.indices) {
            val playingQueue = original.playingQueue.toMutableList()
            val removedSong = playingQueue.removeAt(position)

            val originalPlayingQueue = original.originalPlayingQueue.toMutableList()
            if (queueHolder.shuffleMode == ShuffleMode.SHUFFLE) {
                originalPlayingQueue.remove(removedSong)
            } else {
                originalPlayingQueue.removeAt(position)
            }

            val position =
                if (position < original.currentSongPosition || (queueHolder.playingQueue.size - 1 == position)) {
                    original.currentSongPosition - 1
                } else {
                    original.currentSongPosition
                }
            QueueHolder.QueuesAndPosition(
                playingQueue = playingQueue,
                originalPlayingQueue = originalPlayingQueue,
                currentSongPosition = position
            )
        } else {
            warning(
                TAG,
                "Removing a song at position $position, but out-ranged (${original.originalPlayingQueue.size})"
            )
            original
        }
    }
}

fun removeSong(queueHolder: QueueHolder, song: Song) {
    queueHolder.modify { original ->
        val playingQueue = original.playingQueue.toMutableList()
        val originalPlayingQueue = original.originalPlayingQueue.toMutableList()

        var deletedPosition = -1
        for ((i, item) in playingQueue.withIndex()) {
            if (item == song) deletedPosition = i
        }

        if (deletedPosition > -1) {
            playingQueue.removeAt(deletedPosition)
            originalPlayingQueue.remove(song)
        }

        val newPosition =
            if (deletedPosition < original.currentSongPosition || (queueHolder.playingQueue.size - 1 == deletedPosition)) {
                original.currentSongPosition - 1
            } else {
                original.currentSongPosition
            }

        QueueHolder.QueuesAndPosition(
            playingQueue = playingQueue,
            originalPlayingQueue = originalPlayingQueue,
            currentSongPosition = newPosition
        )
    }
}

fun moveSong(queueHolder: QueueHolder, from: Int, to: Int) {
    if (from == to) return
    val range = queueHolder.originalPlayingQueue.indices
    if (from !in range || to !in range) {
        warning(TAG, "Warning: from $from to $to is outrage ")
        return
    }
    // start moving
    queueHolder.modify { original ->
        val playingQueue = original.playingQueue.toMutableList()
        val originalPlayingQueue = original.originalPlayingQueue.toMutableList()

        val oldPosition: Int = queueHolder.currentSongPosition
        val songToMove: Song = playingQueue.removeAt(from)
        playingQueue.add(to, songToMove)
        if (queueHolder.shuffleMode == ShuffleMode.NONE) {
            val tmpSong: Song = originalPlayingQueue.removeAt(from)
            originalPlayingQueue.add(to, tmpSong)
        }
        val newPosition =
            when {
                from == oldPosition           -> to
                oldPosition in to until from  -> oldPosition + 1
                oldPosition in (from + 1)..to -> oldPosition - 1
                else                          -> oldPosition
            }
        QueueHolder.QueuesAndPosition(
            playingQueue = playingQueue,
            originalPlayingQueue = originalPlayingQueue,
            currentSongPosition = newPosition
        )
    }
}

fun clearQueue(queueHolder: QueueHolder) {
    queueHolder.modify { original ->
        QueueHolder.QueuesAndPosition(
            playingQueue = emptyList(),
            originalPlayingQueue = emptyList(),
            currentSongPosition = -1
        )
    }
}

fun shuffle(queueHolder: QueueHolder, newShuffleMode: ShuffleMode) {
    queueHolder.modify { original ->
        when (newShuffleMode) {
            ShuffleMode.SHUFFLE -> {
                val playingQueue = shuffle(original.playingQueue, queueHolder.currentSongPosition)
                QueueHolder.QueuesAndPosition(
                    playingQueue = playingQueue,
                    originalPlayingQueue = original.originalPlayingQueue,
                    currentSongPosition = 0
                )
            }

            ShuffleMode.NONE    -> {
                val current = original.playingQueue[original.currentSongPosition]
                val recovered = original.originalPlayingQueue.indexOf(current)
                QueueHolder.QueuesAndPosition(
                    playingQueue = original.originalPlayingQueue,
                    originalPlayingQueue = original.originalPlayingQueue,
                    currentSongPosition = recovered.coerceAtLeast(0)
                )
            }
        }
    }
}

private val random by lazy { Random(42) }
private fun shuffle(songs: List<Song>, current: Int): List<Song> {
    if (songs.isEmpty()) return emptyList()

    val result = songs.toMutableList()

    if (current in result.indices) {
        val currentSong = result.removeAt(current)
        result.shuffle(random)
        result.add(0, currentSong)
    } else {
        result.shuffle(random)
    }

    return result
}

fun executePlayRequest(queueManager: QueueManager, request: PlayRequest, mode: Int) {
    when (request) {
        is PlayRequest.SongRequest  -> executePlayRequest(queueManager, request, mode)
        is PlayRequest.SongsRequest -> executePlayRequest(queueManager, request, mode)
        else                        -> {}
    }
    if (mode in SongClickMode.modesRequiringInstantlyChangingState) {
        MusicPlayerRemote.requireResumeInstantlyIfReady()
    }
}

private fun executePlayRequest(queueManager: QueueManager, request: PlayRequest.SongsRequest, mode: Int) {
    val currentPosition = queueManager.currentSongPosition
    val songs = request.songs
    val position = request.position
    when (mode) {
        SongClickMode.SONG_PLAY_NEXT            -> queueManager.addSong(songs[position], currentPosition + 1)
        SongClickMode.SONG_PLAY_NOW             -> queueManager.addSong(songs[position], currentPosition)
        SongClickMode.SONG_APPEND_QUEUE         -> queueManager.addSong(songs[position])
        SongClickMode.SONG_SINGLE_PLAY          -> queueManager.swapQueue(listOf(songs[position]), 0, false)
        SongClickMode.QUEUE_PLAY_NOW            -> queueManager.addSongs(songs, currentPosition)
        SongClickMode.QUEUE_PLAY_NEXT           -> queueManager.addSongs(songs, currentPosition + 1)
        SongClickMode.QUEUE_APPEND_QUEUE        -> queueManager.addSongs(songs)
        SongClickMode.QUEUE_SWITCH_TO_BEGINNING -> queueManager.swapQueue(songs, 0, false)
        SongClickMode.QUEUE_SWITCH_TO_POSITION  -> queueManager.swapQueue(songs, position, false)
        SongClickMode.QUEUE_SHUFFLE             -> {
            queueManager.swapQueue(songs, 0, false)
            queueManager.modifyShuffleMode(ShuffleMode.SHUFFLE, false)
        }

        else  /* invalided */                   -> {}
    }
}

private fun executePlayRequest(queueManager: QueueManager, request: PlayRequest.SongRequest, mode: Int) {
    val currentPosition = queueManager.currentSongPosition
    val song = request.song
    when (mode) {
        SongClickMode.SONG_PLAY_NEXT    -> queueManager.addSong(song, currentPosition + 1)
        SongClickMode.SONG_PLAY_NOW     -> queueManager.addSong(song, currentPosition)
        SongClickMode.SONG_APPEND_QUEUE -> queueManager.addSong(song)
        SongClickMode.SONG_SINGLE_PLAY  -> queueManager.swapQueue(listOf(song), 0, false)
        else  /* invalided */           -> {}
    }
}

private const val TAG = "QueueOperations"
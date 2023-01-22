/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.misc.ShuffleHelper.shuffleAt
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification



fun swapQueue(queueHolder: QueueHolder, newQueue: List<Song>, newPosition: Int) {
    require(newQueue.isNotEmpty() && newPosition in newQueue.indices) {
        "illegal queue or position"
    }
    if (newQueue.isNotEmpty() && newPosition in newQueue.indices) {
        queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
            _originalPlayingQueue.clear()
            _originalPlayingQueue.addAll(newQueue)
            _playingQueue.clear()
            _playingQueue.addAll(newQueue)
            if (queueHolder.shuffleMode == ShuffleMode.SHUFFLE) {
                _playingQueue.shuffleAt(newPosition)
                queueHolder.modifyPosition(0)
            } else {
                queueHolder.modifyPosition(newPosition)
            }
        }
    }
}

fun addSong(queueHolder: QueueHolder, song: Song, position: Int = -1) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        if (position < 0) {
            _playingQueue.add(song)
            _originalPlayingQueue.add(song)
        } else {
            _playingQueue.add(position, song)
            _originalPlayingQueue.add(position, song)
        }
    }
}

fun addSongs(queueHolder: QueueHolder, songs: List<Song>, position: Int = -1) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        if (position < 0) {
            _playingQueue.addAll(songs)
            _originalPlayingQueue.addAll(songs)
        } else {
            _playingQueue.addAll(position, songs)
            _originalPlayingQueue.addAll(position, songs)
        }
    }
}

/**
 * Change current position after deletion
 */
private fun rePosition(queueHolder: QueueHolder, deletedSongPosition: Int) {
    val currentSongPosition = queueHolder.currentSongPosition
    val newPosition: Int =
        if (deletedSongPosition < currentSongPosition) {
            currentSongPosition - 1
        } else if (deletedSongPosition == currentSongPosition) {
            if (queueHolder.playingQueue.size > deletedSongPosition) {
                currentSongPosition
            } else {
                currentSongPosition - 1
            }
        } else return
    queueHolder.modifyPosition(newPosition)

}

fun removeSongAt(queueHolder: QueueHolder, position: Int) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        if (position in _originalPlayingQueue.indices) {
            if (queueHolder.shuffleMode == ShuffleMode.NONE) {
                _playingQueue.removeAt(position)
                _originalPlayingQueue.removeAt(position)
            } else {
                _originalPlayingQueue.remove(_playingQueue.removeAt(position))
            }
            rePosition(queueHolder, position)
        } else {
            ErrorNotification.postErrorNotification(
                "Warning: removing song at position$position,but we only have ${_originalPlayingQueue.size} songs"
            )
        }
    }
}

fun removeSong(queueHolder: QueueHolder, song: Song) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        for (i in _playingQueue.indices) {
            if (_playingQueue[i].id == song.id) {
                _playingQueue.removeAt(i)
                rePosition(queueHolder, i)
            }
        }
        for (i in queueHolder.playingQueue.indices) {
            if (_originalPlayingQueue[i].id == song.id) {
                _originalPlayingQueue.removeAt(i)
            }
        }
        _playingQueue.remove(song)
        _originalPlayingQueue.remove(song)
    }
}

fun moveSong(queueHolder: QueueHolder, from: Int, to: Int) {
    if (from == to) return
    val range = queueHolder.originalPlayingQueue.indices
    if (from !in range || to !in range) {
        ErrorNotification.postErrorNotification("Warning: from $from to $to is outrage ")
        return
    }
    // start moving
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        val oldPosition: Int = queueHolder.currentSongPosition
        val songToMove: Song = _playingQueue.removeAt(from)
        _playingQueue.add(to, songToMove)
        if (queueHolder.shuffleMode == ShuffleMode.NONE) {
            val tmpSong: Song = _originalPlayingQueue.removeAt(from)
            _originalPlayingQueue.add(to, tmpSong)
        }
        val newPosition =
            when {
                from == oldPosition           -> to
                oldPosition in to until from  -> oldPosition + 1
                oldPosition in (from + 1)..to -> oldPosition - 1
                else                          -> oldPosition
            }
        queueHolder.modifyPosition(newPosition)
    }
}

fun clearQueue(queueHolder: QueueHolder) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        _playingQueue.clear()
        _originalPlayingQueue.clear()
        queueHolder.modifyPosition(-1)
    }
}

fun shuffle(queueHolder: QueueHolder, newShuffleMode: ShuffleMode) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        when (newShuffleMode) {
            ShuffleMode.SHUFFLE -> {
                _playingQueue.shuffleAt(queueHolder.currentSongPosition)
                queueHolder.modifyPosition(0)
            }
            ShuffleMode.NONE    -> {
                val currentSongId = queueHolder.currentSong.id
                _playingQueue.clear()
                _playingQueue.addAll(_originalPlayingQueue)
                for (song in _playingQueue) {
                    if (song.id == currentSongId) {
                        queueHolder.modifyPosition(_playingQueue.indexOf(song))
                        break
                    }
                }
            }
        }
        Unit
    }
}
/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.warning



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
                shuffle(_playingQueue, newPosition)
                queueHolder.modifyPosition(0)
            } else {
                queueHolder.modifyPosition(newPosition)
            }
        }
    }
}

fun addSong(queueHolder: QueueHolder, song: Song, position: Int = -1) {
    queueHolder.modifyQueue { _playingQueue, _originalPlayingQueue ->
        if (position < 0 || position >= _playingQueue.size || position >= _originalPlayingQueue.size) {
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
        if (position < 0 || position >= _playingQueue.size || position >= _originalPlayingQueue.size) {
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
            warning(
                TAG,
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
        warning(TAG, "Warning: from $from to $to is outrage ")
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
                shuffle(_playingQueue, queueHolder.currentSongPosition)
                queueHolder.modifyPosition(0)
            }
            ShuffleMode.NONE    -> {
                _playingQueue.clear()
                _playingQueue.addAll(_originalPlayingQueue)
                val currentSong = queueHolder.getSongAt(queueHolder.currentSongPosition)
                if (currentSong == null){
                    queueHolder.modifyPosition(_playingQueue.size)
                } else {
                    for (song in _playingQueue) {
                        if (song.id == currentSong.id) {
                            queueHolder.modifyPosition(_playingQueue.indexOf(song))
                            break
                        }
                    }
                }
            }
        }
    }
}

private fun shuffle(songs: MutableList<Song>, current: Int) {
    if (songs.isEmpty()) return
    if (current in 0 until songs.size) {
        val song: Song = songs.removeAt(current)
        songs.shuffle()
        songs.add(0, song)
    } else {
        songs.shuffle()
    }
}


fun executePlayRequest(queueManager: QueueManager, request: PlayRequest, mode: Int) {
    when(request) {
        is PlayRequest.SongRequest   -> executePlayRequest(queueManager, request, mode)
        is PlayRequest.SongsRequest  -> executePlayRequest(queueManager, request, mode)
        else                         -> {}
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

        else  /* invalided */     -> {}
    }
}

private fun executePlayRequest(queueManager: QueueManager, request: PlayRequest.SongRequest, mode: Int) {
    val currentPosition = queueManager.currentSongPosition
    val song = request.song
    when (mode) {
        SongClickMode.SONG_PLAY_NEXT            -> queueManager.addSong(song, currentPosition + 1)
        SongClickMode.SONG_PLAY_NOW             -> queueManager.addSong(song, currentPosition)
        SongClickMode.SONG_APPEND_QUEUE         -> queueManager.addSong(song)
        SongClickMode.SONG_SINGLE_PLAY          -> queueManager.swapQueue(listOf(song), 0, false)
        else  /* invalided */     -> {}
    }
}

private const val TAG = "QueueOperations"
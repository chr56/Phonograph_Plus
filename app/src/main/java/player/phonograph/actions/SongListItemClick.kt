/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("ClassName")

package player.phonograph.actions

import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import android.content.res.Resources

// 1 : Single song

const val SONG_PLAY_NEXT = 101
const val SONG_PLAY_NOW = 102
const val SONG_APPEND_QUEUE = 103
const val SONG_SINGLE_PLAY = 110

// 2 : Queue

const val QUEUE_PLAY_NEXT = 201
const val QUEUE_PLAY_NOW = 202
const val QUEUE_APPEND_QUEUE = 203
const val QUEUE_SWITCH_TO_BEGINNING = 211
const val QUEUE_SWITCH_TO_POSITION = 213
const val QUEUE_SHUFFLE = 219

// extras

const val PRE_MASK_GOTO_POSITION_FIRST = 1 shl 3
const val PRE_MASK_PLAY_QUEUE_IF_EMPTY = 1 shl 4

val baseModes by lazy {
    intArrayOf(
        SONG_PLAY_NEXT,
        SONG_PLAY_NOW,
        SONG_APPEND_QUEUE,
        SONG_SINGLE_PLAY,
        QUEUE_PLAY_NEXT,
        QUEUE_PLAY_NOW,
        QUEUE_APPEND_QUEUE,
        QUEUE_SWITCH_TO_BEGINNING,
        QUEUE_SWITCH_TO_POSITION,
        QUEUE_SHUFFLE,
    )
}

fun modeName(resources: Resources, id: Int): String {
    return when (id) {
        SONG_PLAY_NEXT -> "SONG_PLAY_NEXT"
        SONG_PLAY_NOW -> "SONG_PLAY_NOW"
        SONG_APPEND_QUEUE -> "SONG_APPEND_QUEUE"
        SONG_SINGLE_PLAY -> "SONG_SINGLE_PLAY"
        QUEUE_PLAY_NEXT -> "QUEUE_PLAY_NEXT"
        QUEUE_PLAY_NOW -> "QUEUE_PLAY_NOW"
        QUEUE_APPEND_QUEUE -> "QUEUE_APPEND_QUEUE"
        QUEUE_SWITCH_TO_BEGINNING -> "QUEUE_SWITCH_TO_BEGINNING"
        QUEUE_SWITCH_TO_POSITION -> "QUEUE_SWITCH_TO_POSITION"
        QUEUE_SHUFFLE -> "QUEUE_SHUFFLE"
        else -> "MODE $id"
    }
}

fun songClick(
    list: List<Song>,
    position: Int,
    startPlaying: Boolean,
): Boolean {
    val extra = Setting.instance.defaultSongItemClickExtraMode
    var base = Setting.instance.defaultSongItemClickBaseMode

    // pre-process extra mode
    if (MusicPlayerRemote.playingQueue.isEmpty() && extra.bitTest(PRE_MASK_PLAY_QUEUE_IF_EMPTY)) {
        if (base in 100..109) {
            base += 100
        } else {
            base = QUEUE_SWITCH_TO_POSITION
        }
    }

    if (list == MusicPlayerRemote.playingQueue && extra.bitTest(PRE_MASK_GOTO_POSITION_FIRST)) {
        // same queue, jump
        MusicPlayerRemote.playSongAt(position)
        return true
    }


    // base mode
    when (base) {
        SONG_PLAY_NEXT            -> list[position].actionPlayNext()
        SONG_PLAY_NOW             -> list[position].actionPlayNow()
        SONG_APPEND_QUEUE         -> list[position].actionEnqueue()
        SONG_SINGLE_PLAY          -> {
            MusicPlayerRemote.playQueue(
                listOf(list[position]),
                0,
                startPlaying,
                null
            )
        }
        QUEUE_PLAY_NOW            -> list.actionPlayNow()
        QUEUE_PLAY_NEXT           -> list.actionPlayNext()
        QUEUE_APPEND_QUEUE        -> list.actionEnqueue()
        QUEUE_SWITCH_TO_BEGINNING -> {
            MusicPlayerRemote.playQueue(
                list,
                0,
                startPlaying,
                ShuffleMode.NONE
            )
        }
        QUEUE_SWITCH_TO_POSITION  -> {
            MusicPlayerRemote.playQueue(
                list,
                position,
                startPlaying,
                ShuffleMode.NONE
            )
        }
        QUEUE_SHUFFLE             -> {
            MusicPlayerRemote.playQueue(
                list,
                0,
                startPlaying,
                ShuffleMode.SHUFFLE
            )
        }
        else  /* invalided */     -> {
            resetBaseMode()
            return false
        }
    }
    return true
}

fun resetBaseMode() {
    Setting.instance.defaultSongItemClickBaseMode = SONG_PLAY_NOW
}

fun resetExtraMode() {
    Setting.instance.defaultSongItemClickExtraMode = 0
}


private fun Int.bitTest(mask: Int): Boolean = (this and mask) != 0
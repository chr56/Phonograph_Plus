/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("ClassName")

package player.phonograph.actions

import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode


@JvmInline
value class ClickMode(val id: Int)

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

private val baseMode: ClickMode get() =  TODO()
private val extraMode: ClickMode get() =  TODO()

fun tapClick(
    list: List<Song>,
    position: Int,
    startPlaying: Boolean,
): Boolean {
    val extra = extraMode.id
    var base = baseMode.id

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
        0                         -> return false //todo
        else                      -> return false
    }
    return true
}


private fun Int.bitTest(mask: Int): Boolean = (this and mask) != 0
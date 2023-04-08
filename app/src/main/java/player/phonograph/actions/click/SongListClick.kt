/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("ClassName")

package player.phonograph.actions.click

import player.phonograph.actions.actionEnqueue
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.actions.actionPlayNow
import player.phonograph.actions.click.mode.SongClickMode.FLAG_MASK_GOTO_POSITION_FIRST
import player.phonograph.actions.click.mode.SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_APPEND_QUEUE
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_PLAY_NEXT
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_PLAY_NOW
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SHUFFLE
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SWITCH_TO_BEGINNING
import player.phonograph.actions.click.mode.SongClickMode.QUEUE_SWITCH_TO_POSITION
import player.phonograph.actions.click.mode.SongClickMode.SONG_APPEND_QUEUE
import player.phonograph.actions.click.mode.SongClickMode.SONG_PLAY_NEXT
import player.phonograph.actions.click.mode.SongClickMode.SONG_PLAY_NOW
import player.phonograph.actions.click.mode.SongClickMode.SONG_SINGLE_PLAY
import player.phonograph.actions.click.mode.SongClickMode.resetBaseMode
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode.NONE
import player.phonograph.service.queue.ShuffleMode.SHUFFLE
import player.phonograph.util.testBit
import kotlin.random.Random

fun songClick(
    list: List<Song>,
    position: Int,
    baseMode: Int,
    extraFlag: Int,
): Boolean {
    var base = baseMode

    // pre-process extra mode
    if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
        if (base in 100..109) {
            base += 100
        } else {
            base = QUEUE_SWITCH_TO_POSITION
        }
    }

    if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST) && list == MusicPlayerRemote.playingQueue) {
        // same queue, jump
        MusicPlayerRemote.playSongAt(position)
        return true
    }


    // base mode
    when (base) {
        SONG_PLAY_NEXT            -> list[position].actionPlayNext()
        SONG_PLAY_NOW             -> list[position].actionPlayNow()
        SONG_APPEND_QUEUE         -> list[position].actionEnqueue()
        SONG_SINGLE_PLAY          -> listOf(list[position]).actionPlay(null, 0)
        QUEUE_PLAY_NOW            -> list.actionPlayNow()
        QUEUE_PLAY_NEXT           -> list.actionPlayNext()
        QUEUE_APPEND_QUEUE        -> list.actionEnqueue()
        QUEUE_SWITCH_TO_BEGINNING -> list.actionPlay(NONE, 0)
        QUEUE_SWITCH_TO_POSITION  -> list.actionPlay(NONE, position)
        QUEUE_SHUFFLE             -> list.actionPlay(SHUFFLE, Random.nextInt(list.size))
        else  /* invalided */     -> {
            resetBaseMode()
            return false
        }
    }
    return true
}


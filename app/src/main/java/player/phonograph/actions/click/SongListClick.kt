/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("ClassName")

package player.phonograph.actions.click

import player.phonograph.actions.actionEnqueue
import player.phonograph.actions.actionPlayNext
import player.phonograph.actions.actionPlayNow
import player.phonograph.actions.click.mode.*
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import player.phonograph.util.Util.testBit

fun songClick(
    list: List<Song>,
    position: Int,
    startPlaying: Boolean,
): Boolean {
    val extra = Setting.instance.defaultSongItemClickExtraMode
    var base = Setting.instance.defaultSongItemClickBaseMode

    // pre-process extra mode
    if (MusicPlayerRemote.playingQueue.isEmpty() && extra.testBit(PRE_MASK_PLAY_QUEUE_IF_EMPTY)) {
        if (base in 100..109) {
            base += 100
        } else {
            base = QUEUE_SWITCH_TO_POSITION
        }
    }

    if (list == MusicPlayerRemote.playingQueue && extra.testBit(PRE_MASK_GOTO_POSITION_FIRST)) {
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


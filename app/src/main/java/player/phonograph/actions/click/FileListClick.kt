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
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.linkedSong
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode.NONE
import player.phonograph.service.queue.ShuffleMode.SHUFFLE
import player.phonograph.util.testBit
import android.content.Context
import kotlin.random.Random
import kotlin.LazyThreadSafetyMode.NONE as LazyNone

/**
 * @param list entire list including folder
 * @param position in-list position
 */
fun fileClick(
    list: List<FileEntity>,
    position: Int,
    baseMode: Int,
    extraFlag: Int,
    context: Context,
): Boolean {
    var base = baseMode
    val songRequest by lazy(LazyNone) { filter(list, position, context) }

    // pre-process extra mode
    if (MusicPlayerRemote.playingQueue.isEmpty() && extraFlag.testBit(FLAG_MASK_PLAY_QUEUE_IF_EMPTY)) {
        if (base in 100..109) {
            base += 100
        } else {
            base = QUEUE_SWITCH_TO_POSITION
        }
    }

    if (extraFlag.testBit(FLAG_MASK_GOTO_POSITION_FIRST) && songRequest.songs == MusicPlayerRemote.playingQueue) {
        // same queue, jump
        MusicPlayerRemote.playSongAt(songRequest.position)
        return true
    }



    when (base) {
        SONG_PLAY_NEXT,
        SONG_PLAY_NOW,
        SONG_APPEND_QUEUE,
        SONG_SINGLE_PLAY,
             -> {
            val fileEntity = list[position] as? FileEntity.File ?: return false
            val song = fileEntity.linkedSong(context)
            when (base) {
                SONG_PLAY_NEXT    -> song.actionPlayNext()
                SONG_PLAY_NOW     -> song.actionPlayNow()
                SONG_APPEND_QUEUE -> song.actionEnqueue()
                SONG_SINGLE_PLAY  -> listOf(song).actionPlay(null, 0)
            }
        }
        QUEUE_PLAY_NOW,
        QUEUE_PLAY_NEXT,
        QUEUE_APPEND_QUEUE,
        QUEUE_SWITCH_TO_BEGINNING,
        QUEUE_SWITCH_TO_POSITION,
        QUEUE_SHUFFLE,
             -> {
            val songs = songRequest.songs
            val actualPosition = songRequest.position

            when (base) {
                QUEUE_PLAY_NOW            -> songs.actionPlayNow()
                QUEUE_PLAY_NEXT           -> songs.actionPlayNext()
                QUEUE_APPEND_QUEUE        -> songs.actionEnqueue()
                QUEUE_SWITCH_TO_BEGINNING -> songs.actionPlay(NONE, 0)
                QUEUE_SWITCH_TO_POSITION  -> songs.actionPlay(NONE, actualPosition)
                QUEUE_SHUFFLE             ->
                    songs.actionPlay(SHUFFLE, Random.nextInt(songs.size))
            }
        }
        else -> {
            resetBaseMode()
            return false
        }
    }
    return true
}

/**
 * filter folders and relocate position
 */
private fun filter(list: List<FileEntity>, position: Int, context: Context): PlayRequest {
    var actualPosition: Int = position
    val actualFileList = ArrayList<Song>(position)
    for ((index, item) in list.withIndex()) {
        if (item is FileEntity.File) {
            actualFileList.add(item.linkedSong(context))
        } else {
            if (index < position) actualPosition--
        }
    }
    return PlayRequest(actualFileList, actualPosition)
}
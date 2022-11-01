/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.click.mode

import player.phonograph.R
import player.phonograph.settings.Setting
import android.content.res.Resources

@Suppress("MemberVisibilityCanBePrivate")
object SongClickMode {

    const val SONG_PLAY_NEXT = 101
    const val SONG_PLAY_NOW = 102
    const val SONG_APPEND_QUEUE = 103
    const val SONG_SINGLE_PLAY = 110

    const val QUEUE_PLAY_NEXT = 201
    const val QUEUE_PLAY_NOW = 202
    const val QUEUE_APPEND_QUEUE = 203
    const val QUEUE_SWITCH_TO_BEGINNING = 211
    const val QUEUE_SWITCH_TO_POSITION = 213
    const val QUEUE_SHUFFLE = 219

    const val FLAG_MASK_GOTO_POSITION_FIRST = 1 shl 3
    const val FLAG_MASK_PLAY_QUEUE_IF_EMPTY = 1 shl 4

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
            SONG_PLAY_NEXT            -> resources.getString(R.string.mode_song_play_next)
            SONG_PLAY_NOW             -> resources.getString(R.string.mode_song_play_now)
            SONG_APPEND_QUEUE         -> resources.getString(R.string.mode_song_append_queue)
            SONG_SINGLE_PLAY          -> resources.getString(R.string.mode_song_single_play)
            QUEUE_PLAY_NEXT           -> resources.getString(R.string.mode_queue_play_next)
            QUEUE_PLAY_NOW            -> resources.getString(R.string.mode_queue_play_now)
            QUEUE_APPEND_QUEUE        -> resources.getString(R.string.mode_queue_append_queue)
            QUEUE_SWITCH_TO_BEGINNING -> resources.getString(R.string.mode_queue_switch_to_beginning)
            QUEUE_SWITCH_TO_POSITION  -> resources.getString(R.string.mode_queue_switch_to_position)
            QUEUE_SHUFFLE             -> resources.getString(R.string.mode_queue_shuffle)
            else                      -> "UNKNOWN MODE $id"
        }
    }


    fun resetBaseMode() {
        Setting.instance.songItemClickMode = SONG_PLAY_NOW
    }

    fun resetExtraMode() {
        Setting.instance.songItemClickExtraFlag = FLAG_MASK_PLAY_QUEUE_IF_EMPTY
    }
}
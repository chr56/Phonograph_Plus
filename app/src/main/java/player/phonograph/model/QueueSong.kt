/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QueueSong(val song: Song, val index: Int) : Displayable by song, Parcelable {
    companion object {
        fun fromQueue(songs: List<Song>): List<QueueSong> =
            songs.mapIndexed { index, song -> QueueSong(song, index) }
    }
}
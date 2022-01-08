/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.helper

import player.phonograph.database.mediastore.SongConverter
import player.phonograph.database.mediastore.Song as NewSong
import player.phonograph.model.Song as OldSong

object SongModelConverterHelper {

    @JvmStatic
    fun convert(songs: List<NewSong>): List<OldSong> {
        return List(songs.size) { index ->
            SongConverter.toSongModel(songs[index])
        }
    }

    @JvmStatic
    fun convertBack(songs: List<OldSong>): List<NewSong> {
        return List(songs.size) { index ->
            SongConverter.fromSongModel(songs[index])
        }
    }
}

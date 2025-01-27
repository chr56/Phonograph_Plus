/*
 * Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.lyrics

import android.os.Parcelable

sealed interface AbsLyrics : Parcelable {
    @LyricsType
    val type: Int

    val title: String
    val source: LyricsSource

    val raw: String
    val length: Int
    val lyricsLineArray: Array<String>
    val lyricsTimeArray: IntArray

}




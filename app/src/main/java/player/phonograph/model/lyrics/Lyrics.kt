/*
 * Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.lyrics

import androidx.annotation.IntDef
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

    companion object {
        const val DEFAULT_TITLE = "Lyrics"
    }
}

interface TextLyrics : AbsLyrics {
    override val type: Int get() = LyricsType.TXT
}

interface LrcLyrics : AbsLyrics {

    override val type: Int get() = LyricsType.LRC

    /**
     * total duration in millisecond
     */
    val totalTime: Long

    /**
     * time offset in millisecond
     */
    val offset: Long


    /**
     * get a line of lyrics by timestamp
     * @param timestamp target timestamp in millisecond
     * @return pair of content and duration in millisecond
     */
    fun getLine(timestamp: Int): Pair<String, Long>

    /**
     * get line number by timeStamp
     * @param timestamp target timestamp in millisecond
     */
    fun getLineNumber(timestamp: Int): Int

}

@IntDef(LyricsType.TXT, LyricsType.LRC)
annotation class LyricsType {
    companion object {
        const val LRC: Int = 2
        const val TXT: Int = 1
    }
}
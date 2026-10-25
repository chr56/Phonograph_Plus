/*
 * Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.lyrics

import androidx.annotation.IntDef
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

    /**
     * get timed words of an enhanced LRC line
     * @param lineIndex line index in current lyrics
     * @return timed words; empty for ordinary LRC lines
     */
    fun getLineWords(lineIndex: Int): List<LrcWord> = emptyList()

    /**
     * get index of the last word that should be highlighted at [timestamp]
     * @param lineIndex line index in current lyrics
     * @param timestamp target timestamp in millisecond
     * @return word index, or -1 when the line has no word timings or no word has started yet
     */
    fun getWordIndex(lineIndex: Int, timestamp: Int): Int = -1

}

@Parcelize
data class LrcWord(
    val text: String,
    val startTime: Int,
    val endTime: Int,
) : Parcelable

@Parcelize
data class LrcLine(
    val text: String,
    val words: List<LrcWord> = emptyList(),
) : Parcelable

@IntDef(LyricsType.TXT, LyricsType.LRC)
annotation class LyricsType {
    companion object {
        const val LRC: Int = 2
        const val TXT: Int = 1
    }
}
/*
 *  Copyright (c) 2022~2025 chr_56
 */

@file:Suppress("RegExpRedundantEscape")

package player.phonograph.mechanism.lyrics

import player.phonograph.foundation.warning
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsSource
import androidx.core.util.forEach
import android.util.SparseArray
import kotlinx.parcelize.Parcelize
import java.util.Locale
import java.util.regex.Pattern

@Parcelize
data class ActualLrcLyrics(
    private val lyrics: SparseArray<String>,
    override val source: LyricsSource,
    override var title: String = AbsLyrics.DEFAULT_TITLE,
    override val offset: Long = 0,
    override val totalTime: Long = -1,
) : LrcLyrics {

    override val raw: String
        get() {
            val stringBuilder = StringBuilder()
            lyrics.forEach { _, line ->
                stringBuilder.append(line).append("\r\n")
            }
            return stringBuilder.toString().trim()
        }

    override val length: Int get() = lyrics.size()
    override val lyricsLineArray: Array<String> get() = Array(lyrics.size()) { lyrics.valueAt(it) }
    override val lyricsTimeArray: IntArray get() = IntArray(lyrics.size()) { lyrics.keyAt(it) }


    override fun getLine(timestamp: Int): Pair<String, Long> {
        val index = getLineNumber(timestamp)
        if (index in 0..lyrics.size()) {
            val currentLine = lyrics.valueAt(index)
                .replace(Regex("""\\[nNrR]"""), "\n")
            val length: Long =
                if (index + 1 != lyrics.size()) {
                    // not last line
                    lyrics.keyAt(index + 1).toLong() - lyrics.keyAt(index)
                } else {
                    // last line
                    totalTime - lyrics.keyAt(index)
                }
            return Pair(
                currentLine, if (length in 1..totalTime) length else -1
            )
        }
        return Pair("", -1)
    }

    override fun getLineNumber(timestamp: Int): Int {
        var index = -1
        if (totalTime != -1L) { // -1 means " no length info in lyrics"
            if (timestamp >= totalTime) {
                warning(
                    "LrcLyrics",
                    "TimeStamp is over the total lyrics length: lyrics might be mismatched, please check up."
                )
                return index
            }
        }

        val ms = timestamp + offset + TIME_OFFSET_MS
        index = binSearch(ms, 0, lyrics.size())
        return index
    }

    private tailrec fun binSearch(time: Long, down: Int, up: Int): Int {
        val mid = (down + up) / 2
        when {
            time < lyrics.keyAt(mid)           -> {
                return if (mid <= 0) {
                    // first line
                    -1
                } else {
                    // not first line
                    binSearch(time, down, mid)
                }
            }

            time == lyrics.keyAt(mid).toLong() -> return mid
            time > lyrics.keyAt(mid)           -> {
                if (mid < lyrics.size() - 1) {
                    // not last line
                    return if (time < lyrics.keyAt(mid + 1)) mid
                    else binSearch(time, mid, up)
                } else {
                    // last line
                    return lyrics.size() - 1
                }
            }

            else                               -> return -1 // what!?
        }
    }

    companion object {
        private const val TAG = "LrcLyrics"
        fun from(raw: String, source: LyricsSource = LyricsSource.Unknown): ActualLrcLyrics { // raw data:
            //
            val rawLines: List<String> = raw.split(Pattern.compile("\r?\n"))
            var offset: Long = 0
            var totalTime: Long = -1
            var title: String = AbsLyrics.DEFAULT_TITLE
            // data to Return:
            //
            val lyrics: SparseArray<String> = SparseArray()

            // Parse start
            for (line in rawLines) {
                // blank line ?
                if (line.isBlank()) {
                    continue
                }
                // parse metadata of lyric
                val attrMatcher = LRC_ATTRIBUTE_PATTERN.matcher(line)
                if (attrMatcher.find()) {
                    try { // todo [lyric parse] fix null safe in attr parse
                        val attr = attrMatcher.group(1)!!.lowercase(Locale.ENGLISH).trim { it <= ' ' }
                        val value = attrMatcher.group(2)!!.lowercase(Locale.ENGLISH).trim { it <= ' ' }
                        when (attr) {
                            "offset" -> offset = value.toLong()
                            "length" -> totalTime = value.toLong()
                            "ti"     -> title = value
                            // todo [lyric parse] more attr
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                // parse lyric lines
                val matcher = LRC_LINE_PATTERN.matcher(line)
                if (matcher.find()) {
                    val time: String? = matcher.group(1)
                    val text: String? = matcher.group(2)
                    var timestamp = 0

                    // Time
                    try { // todo: null safe?
                        val timeMatcher = LRC_TIME_PATTERN.matcher(time!!)
                        while (timeMatcher.find()) {
                            var m = 0
                            var s = 0f
                            try { // todo [lyric parse] fix null safe in line parse
                                m = timeMatcher.group(1)!!.toInt()
                                s = timeMatcher.group(2)!!.toFloat()
                            } catch (ex: NumberFormatException) {
                                ex.printStackTrace()
                            }
                            timestamp = ((s * LRC_SECONDS_TO_MS_MULTIPLIER).toInt() + m * LRC_MINUTES_TO_MS_MULTIPLIER)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Write to var
                    val existed = lyrics.get(timestamp)
                    if (existed == null) {
                        lyrics.put(timestamp, text.orEmpty())
                    } else {
                        lyrics.put(timestamp, existed + '\n' + text)
                    }
                }
            } // loop_end

            // create lyrics
            return ActualLrcLyrics(lyrics, source, title, offset, totalTime)
        }

        private val LRC_LINE_PATTERN = Pattern.compile("((?:\\[.*?\\])+)(.*)")
        private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2}(?:\\.\\d+)?)\\]")
        private val LRC_ATTRIBUTE_PATTERN = Pattern.compile("\\[(\\D+):(.+)\\]")
        private const val LRC_SECONDS_TO_MS_MULTIPLIER = 1000f
        private const val LRC_MINUTES_TO_MS_MULTIPLIER = 60000
        private const val TIME_OFFSET_MS = 400
    }

}


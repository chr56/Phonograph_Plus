/*
 *  Copyright (c) 2022~2025 chr_56
 */

@file:Suppress("RegExpRedundantEscape")

package player.phonograph.mechanism.lyrics

import player.phonograph.App
import player.phonograph.foundation.error.warning
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLine
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LrcWord
import player.phonograph.model.lyrics.LyricsSource
import androidx.core.util.forEach
import android.util.SparseArray
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.parcelize.Parcelize
import java.util.Locale
import java.util.regex.Pattern

@Parcelize
data class ActualLrcLyrics(
    private val lyrics: SparseArray<LrcLine>,
    override val source: LyricsSource,
    override var title: String = AbsLyrics.DEFAULT_TITLE,
    override val offset: Long = 0,
    override val totalTime: Long = -1,
) : LrcLyrics {

    override val raw: String
        get() {
            val stringBuilder = StringBuilder()
            lyrics.forEach { _, line ->
                stringBuilder.append(line.text).append("\r\n")
            }
            return stringBuilder.toString().trim()
        }

    override val length: Int get() = lyrics.size()
    override val lyricsLineArray: Array<String> get() = Array(lyrics.size()) { lyrics.valueAt(it).text }
    override val lyricsTimeArray: IntArray get() = IntArray(lyrics.size()) { lyrics.keyAt(it) }


    override fun getLine(timestamp: Int): Pair<String, Long> {
        val index = getLineNumber(timestamp)
        if (index in 0 until lyrics.size()) {
            val currentLine = lyrics.valueAt(index).text.replace(LINE_FEED_PATTERN, "\n")
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
        if (lyrics.size() == 0) return -1

        var index = -1
        if (totalTime != -1L) { // -1 means " no length info in lyrics"
            if (timestamp >= totalTime) {
                warning(
                    App.instance,
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

    override fun getLineWords(lineIndex: Int): List<LrcWord> =
        if (lineIndex in 0 until lyrics.size()) lyrics.valueAt(lineIndex).words else emptyList()

    override fun getWordIndex(lineIndex: Int, timestamp: Int): Int {
        val words = getLineWords(lineIndex)
        if (words.isEmpty()) return -1

        val ms = timestamp + offset + TIME_OFFSET_MS
        var index = -1
        for (i in words.indices) {
            if (words[i].startTime <= ms) index = i else break
        }
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
            val rawLines: List<String> = raw.split(Pattern.compile("\r\n|\n|\r"))
            var offset: Long = 0
            var totalTime: Long = -1
            var title: String = AbsLyrics.DEFAULT_TITLE
            val parsedLines = mutableListOf<RawLrcLine>()

            for (rawLine in rawLines) {
                // blank line ?
                if (rawLine.isBlank()) {
                    continue
                }
                val line = rawLine.removePrefix("\uFEFF")

                // parse metadata of lyric
                val attrMatcher = LRC_ATTRIBUTE_PATTERN.matcher(line)
                if (attrMatcher.matches()) {
                    try {
                        val attr = attrMatcher.group(1)!!.lowercase(Locale.ENGLISH)
                        val value = attrMatcher.group(2)!!.trim()
                        when (attr) {
                            "offset" -> value.toLongOrNull()?.let { offset = it }
                            "length" -> parseTimeValue(value)?.let { totalTime = it }
                            "ti"     -> if (value.isNotEmpty()) title = value
                            // todo [lyric parse] more attr
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    continue
                }

                // parse lyric lines
                val matcher = LRC_LINE_PATTERN.matcher(line)
                if (matcher.matches()) {
                    val lineTimes = mutableListOf<Int>()
                    val timeMatcher = LRC_TIME_PATTERN.matcher(matcher.group(1).orEmpty())
                    while (timeMatcher.find()) {
                        parseTimestamp(timeMatcher.group(1), timeMatcher.group(2))?.let { lineTimes.add(it) }
                    }

                    val (text, words) = parseEnhancedBody(matcher.group(2).orEmpty(), lineTimes.firstOrNull())
                    val firstLineTime = lineTimes.firstOrNull() ?: words.firstOrNull()?.startTime
                    if (firstLineTime != null) {
                        if (words.isEmpty()) {
                            for (lineTime in lineTimes) {
                                parsedLines.add(RawLrcLine(lineTime, text, emptyList()))
                            }
                        } else {
                            parsedLines.add(RawLrcLine(firstLineTime, text, words))
                        }
                    }
                } else {
                    // Enhanced LRC without a leading line timestamp
                    val (text, words) = parseEnhancedBody(line, null)
                    val firstLineTime = words.firstOrNull()?.startTime
                    if (firstLineTime != null) {
                        parsedLines.add(RawLrcLine(firstLineTime, text, words))
                    }
                }
            } // loop_end

            // Calculate end times and build the final line list
            val sortedLines = parsedLines.sortedBy { it.startTime }
            val lyrics = SparseArray<LrcLine>(sortedLines.size)
            for (index in sortedLines.indices) {
                val rawLine = sortedLines[index]
                val nextLineStart = sortedLines.getOrNull(index + 1)?.startTime
                val lineEnd = when {
                    nextLineStart != null                 -> nextLineStart
                    totalTime > rawLine.startTime         -> totalTime.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    else                                  -> rawLine.words.lastOrNull()?.startTime ?: rawLine.startTime
                }
                val words = if (rawLine.words.isEmpty()) {
                    emptyList()
                } else {
                    rawLine.words.mapIndexed { wordIndex, word ->
                        val end = rawLine.words.getOrNull(wordIndex + 1)?.startTime ?: lineEnd
                        LrcWord(word.text, word.startTime, max(end, word.startTime))
                    }
                }

                val existed = lyrics.get(rawLine.startTime)
                if (existed == null) {
                    lyrics.put(rawLine.startTime, LrcLine(rawLine.text, words))
                } else {
                    val mergedWords = if (existed.words.isEmpty() && words.isEmpty()) {
                        emptyList()
                    } else {
                        existed.words + words
                    }
                    lyrics.put(rawLine.startTime, LrcLine(existed.text + '\n' + rawLine.text, mergedWords))
                }
            }

            // create lyrics
            return ActualLrcLyrics(lyrics, source, title, offset, totalTime)
        }

        /**
         * Parse enhanced LRC word timings (&lt;mm:ss.xx&gt;) and strip their tags from the text.
         */
        private fun parseEnhancedBody(body: String, lineStart: Int?): Pair<String, List<RawLrcWord>> {
            val matcher = ENHANCED_TIME_PATTERN.matcher(body)
            val tags = mutableListOf<EnhancedTag>()
            while (matcher.find()) {
                val time = parseTimestamp(matcher.group(1), matcher.group(2)) ?: continue
                tags.add(EnhancedTag(matcher.start(), matcher.end(), time))
            }
            if (tags.isEmpty()) return body to emptyList()

            val textBuilder = StringBuilder()
            val words = mutableListOf<RawLrcWord>()
            val firstTag = tags.first()

            if (firstTag.start > 0) {
                val prefix = body.substring(0, firstTag.start)
                // Enhanced LRC may prefix lines with a vocal agent, e.g. "v1:" or "v2:".
                // It is metadata, not lyric content, so do not include it as a word.
                if (!VOICE_PREFIX_PATTERN.matcher(prefix).matches()) {
                    textBuilder.append(prefix)
                    words.add(RawLrcWord(prefix, lineStart ?: firstTag.time))
                }
            }

            tags.forEachIndexed { index, tag ->
                val end = tags.getOrNull(index + 1)?.start ?: body.length
                if (tag.end < end) {
                    val text = body.substring(tag.end, end)
                    textBuilder.append(text)
                    if (text.isNotEmpty()) {
                        words.add(RawLrcWord(text, tag.time))
                    }
                }
            }

            return textBuilder.toString() to words
        }

        private fun parseTimestamp(minutes: String?, seconds: String?): Int? {
            if (minutes == null || seconds == null) return null
            val m = minutes.toLongOrNull() ?: return null
            val s = seconds.toDoubleOrNull() ?: return null
            return (m * LRC_MINUTES_TO_MS + (s * LRC_SECONDS_TO_MS).roundToInt())
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()
        }

        private fun parseTimeValue(value: String): Long? {
            value.toLongOrNull()?.let { return it }
            val matcher = LRC_TIMESTAMP_PATTERN.matcher(value)
            if (matcher.matches()) {
                return parseTimestamp(matcher.group(1), matcher.group(2))?.toLong()
            }
            return null
        }

        private data class RawLrcLine(
            val startTime: Int,
            val text: String,
            val words: List<RawLrcWord>,
        )

        private data class RawLrcWord(
            val text: String,
            val startTime: Int,
        )

        private data class EnhancedTag(
            val start: Int,
            val end: Int,
            val time: Int,
        )

        private val LRC_LINE_PATTERN = Pattern.compile("^\\s*((?:\\[\\d+:\\d{2}(?:\\.\\d+)?\\])+)(.*)$")
        private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2}(?:\\.\\d+)?)\\]")
        private val LRC_TIMESTAMP_PATTERN = Pattern.compile("(\\d+):(\\d{2}(?:\\.\\d+)?)")
        private val LRC_ATTRIBUTE_PATTERN = Pattern.compile("^\\[([A-Za-z]+):(.+)]\\s*$")
        private val ENHANCED_TIME_PATTERN = Pattern.compile("<(\\d+):(\\d{2}(?:\\.\\d+)?)>")
        private val VOICE_PREFIX_PATTERN =
            Pattern.compile("^\\s*(?:v\\d+|bg|ch|h\\d+):\\s*$", Pattern.CASE_INSENSITIVE)
        private val LINE_FEED_PATTERN = Regex("""\\[nNrR]""")
        private const val LRC_SECONDS_TO_MS = 1000.0
        private const val LRC_MINUTES_TO_MS = 60_000L
        private const val TIME_OFFSET_MS = 400
    }

}

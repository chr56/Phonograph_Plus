/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("RegExpRedundantEscape")

package player.phonograph.model.lyrics

import player.phonograph.util.sparseArray
import player.phonograph.util.warning
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import java.util.Locale
import java.util.regex.Pattern

class LrcLyrics : AbsLyrics, Parcelable {
    override val type: Int = LyricsType.LRC

    private constructor(lyrics: SparseArray<String>, source: LyricsSource) {
        this.lyrics = lyrics.also {
            if (it.isEmpty()) Log.w(TAG, "$this has no lyrics")
        }
        this.title = DEFAULT_TITLE
        this.offset = 0
        this.totalTime = -1
        this.source = source
    }

    private constructor(
        lyrics: SparseArray<String>,
        title: String?,
        offset: Long = 0,
        totalTime: Long = -1,
        source: LyricsSource,
    ) :
            this(lyrics, source) {
        this.title = title ?: DEFAULT_TITLE
        this.offset = offset
        this.totalTime = totalTime
    }

    override var title: String

    private var lyrics: SparseArray<String>
    private var offset: Long
    private var totalTime: Long


    override val raw: String
        get() {
            val stringBuilder = StringBuilder()
            lyrics.forEach { _, line ->
                stringBuilder.append(line).append("\r\n")
            }
            return stringBuilder.toString().trim()
        }

    override val length: Int get() = lyrics.size()

    override val lyricsLineArray: Array<String>
        get() = Array(lyrics.size()) { lyrics.valueAt(it) }
    override val lyricsTimeArray: IntArray
        get() = IntArray(lyrics.size()) { lyrics.keyAt(it) }

    val rawLyrics: SparseArray<String> get() = lyrics

    /**
     * get a line of lyrics
     * @param timeStamp current rime in millisecond
     * @return Pair<LyricsLine:[String],Length:[Long]>
     */
    fun getLine(timeStamp: Int): Pair<String, Long> {
        val index = getPosition(timeStamp)
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

    fun getPosition(timeStamp: Int): Int {
        var index = -1
        if (totalTime != -1L) { // -1 means " no length info in lyrics"
            if (timeStamp >= totalTime) {
                warning(
                    "LrcLyrics",
                    "TimeStamp is over the total lyrics length: lyrics might be mismatched, please check up."
                )
                return index
            }
        }

        val ms = timeStamp + offset + TIME_OFFSET_MS
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

    override val source: LyricsSource

    companion object {
        private const val TAG = "LrcLyrics"
        fun from(raw: String, source: LyricsSource = LyricsSource.Unknown()): LrcLyrics { // raw data:
            //
            val rawLines: List<String> = raw.split(Pattern.compile("\r?\n"))
            var offset: Long = 0
            var totalTime: Long = -1
            var title: String? = null
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
            return LrcLyrics(lyrics, title, offset, totalTime, source)
        }

        private val LRC_LINE_PATTERN = Pattern.compile("((?:\\[.*?\\])+)(.*)")
        private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2}(?:\\.\\d+)?)\\]")
        private val LRC_ATTRIBUTE_PATTERN = Pattern.compile("\\[(\\D+):(.+)\\]")
        private const val LRC_SECONDS_TO_MS_MULTIPLIER = 1000f
        private const val LRC_MINUTES_TO_MS_MULTIPLIER = 60000
        private const val TIME_OFFSET_MS = 400
        // Parcelable
        @JvmField
        val CREATOR = object : Parcelable.Creator<LrcLyrics> {
            override fun createFromParcel(parcel: Parcel): LrcLyrics {
                return LrcLyrics(parcel)
            }

            override fun newArray(size: Int): Array<LrcLyrics?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeSparseArray(lyrics)
        parcel.writeString(title)
        parcel.writeLong(offset)
        parcel.writeLong(totalTime)
        parcel.writeInt(source.type)
    }

    constructor(parcel: Parcel) {
        parcel.readInt().let { if (it != LyricsType.LRC) throw IllegalStateException("incorrect parcel received") }
        this.lyrics = parcel.sparseArray(null) ?: SparseArray()
        this.title = parcel.readString() ?: DEFAULT_TITLE
        this.offset = parcel.readLong()
        this.totalTime = parcel.readLong()
        this.source = LyricsSource(parcel.readInt())
    }

    override fun describeContents(): Int = 0
}


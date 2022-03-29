/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import java.util.*
import java.util.regex.Pattern

class LrcLyrics : AbsLyrics, Parcelable {
    override val type: Int = LRC

    private constructor(lyrics: SparseArray<String>) {
        this.lyrics = lyrics.also {
            if (it.isEmpty()) Log.w(TAG, "$this has no lyrics")
        }
        this.title = DEFAULT_TITLE
        this.offset = 0
        this.totalTime = -1
    }
    private constructor(lyrics: SparseArray<String>, title: String?, offset: Long = 0, totalTime: Long = -1) :
        this(lyrics) {
        this.title = title ?: DEFAULT_TITLE
        this.offset = offset
        this.totalTime = totalTime
    }

    private var lyrics: SparseArray<String>
    private var title: String
    private var offset: Long
    private var totalTime: Long

    override fun getTitle(): String = title

    override fun getRaw(): String {
        val stringBuilder = StringBuilder()
        lyrics.forEach { _, line ->
            stringBuilder.append(line).append("\r\n")
        }
        return stringBuilder.toString().trim()
    }

    override fun getLyricsLineArray(): Array<String> {
        return Array(lyrics.size()) {
            lyrics.valueAt(it)
        }
    }
    override fun getLyricsTimeArray(): IntArray {
        return IntArray(lyrics.size()) {
            lyrics.keyAt(it)
        }
    }

    val rawLyrics: SparseArray<String>
        get() = lyrics

    fun getLine(timeStamp: Int): String {
        if (totalTime != -1L) { // -1 means " no length info in lyrics"
            if (timeStamp >= totalTime) throw Exception("TimeStamp is over the total lyrics length: lyrics might be mismatched")
        }

        val ms = timeStamp + offset + TIME_OFFSET_MS
        var index = 0
        // Todo performance improve
        for (i in 0 until lyrics.size()) {
            if (ms >= lyrics.keyAt(i)) {
                index = i
            } else {
                break
            }
        }

        // Todo '\n' detect
        return lyrics.valueAt(index) as String
    }

    companion object {
        private const val TAG = "LrcLyrics"
        fun from(raw: String): LrcLyrics { // raw data:
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
                            "ti" -> title = value
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

                    var ms = 0

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
                            ms = ((s * LRC_SECONDS_TO_MS_MULTIPLIER).toInt() + m * LRC_MINUTES_TO_MS_MULTIPLIER)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Write to var
                    lyrics.put(ms, text.orEmpty())
                }
            } // loop_end

            // create lyrics
            return LrcLyrics(lyrics, title, offset, totalTime)
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
        parcel.writeInt(LRC)
        parcel.writeSparseArray(lyrics)
        parcel.writeString(title)
        parcel.writeLong(offset)
        parcel.writeLong(totalTime)
    }
    constructor(parcel: Parcel) {
        parcel.readInt().let { if (it != LRC) throw IllegalStateException("incorrect parcel received") }
        this.lyrics = parcel.readSparseArray(String::class.java.classLoader) ?: SparseArray()
        this.title = parcel.readString() ?: DEFAULT_TITLE
        this.offset = parcel.readLong()
        this.totalTime = parcel.readLong()
    }
    override fun describeContents(): Int = 0
}

class LyricsCursor(val l: LrcLyrics) {

    private var index: Int = 0
    fun setIndex(i: Int) {
        index = i
    }

    fun locate(index: Int): String {
        return l.rawLyrics.valueAt(index) as String
    }

    fun next(): String {
        index++
        return l.rawLyrics.valueAt(index) as String
    }

    fun previous(): String {
        index--
        return l.rawLyrics.valueAt(index) as String
    }

    fun first(): String {
        return l.rawLyrics[0] as String
    }

    fun moveToFirst() {
        index = 0
    }

    fun last(): String {
        return l.rawLyrics[l.rawLyrics.size()] as String
    }

    fun moveToLast() {
        index = l.rawLyrics.size()
    }
}

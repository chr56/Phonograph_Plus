/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

import player.phonograph.util.array
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import java.util.regex.Pattern

class TextLyrics : AbsLyrics, Parcelable {
    override val type: Int = LyricsType.TXT

    private constructor(parsedLines: MutableList<String>, source: LyricsSource) {
        this.lines = parsedLines
            .ifEmpty {
                Log.w(TAG, "$this has no lyrics")
                ArrayList()
            }
        this.title = DEFAULT_TITLE
        this.source = source
    }

    private constructor(lines: MutableList<String>, title: String, source: LyricsSource) {
        this.lines = lines
            .ifEmpty {
                Log.w(TAG, "$this has no lyrics")
                ArrayList()
            }
        this.title = title
        this.source = source
    }

    private val lines: MutableList<String>

    override var title: String
    override val raw: String get() = lines.joinToString(separator = "\r\n") { it.trim() }
    override val length: Int get() = lines.size
    override val lyricsLineArray: Array<String> get() = Array(lines.size) { lines[it] }
    override val lyricsTimeArray: IntArray get() = IntArray(lines.size) { -1 }

    override val source: LyricsSource

    companion object {
        private const val TAG = "TextLyrics"
        fun from(raw: String, source: LyricsSource = LyricsSource.Unknown()): TextLyrics {
            val result = raw.split(Pattern.compile("(\r?\n)|(\\\\[nNrR])"))
            return TextLyrics(result.toMutableList(), source)
        }

        // Parcelable
        @JvmField
        val CREATOR = object : Parcelable.Creator<TextLyrics> {
            override fun createFromParcel(parcel: Parcel): TextLyrics {
                return TextLyrics(parcel)
            }

            override fun newArray(size: Int): Array<TextLyrics?> {
                return arrayOfNulls(size)
            }
        }
    }

    // Parcelable
    constructor(parcel: Parcel) {
        parcel.readInt().let { if (it != LyricsType.TXT) throw IllegalStateException("incorrect parcel received") }
        lines = parcel.array<String>(null)?.toMutableList()!!
        title = parcel.readString() ?: DEFAULT_TITLE
        source = LyricsSource(parcel.readInt())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeArray(lyricsLineArray)
        parcel.writeString(title)
        parcel.writeInt(source.type)
    }

    override fun describeContents(): Int = 0
}

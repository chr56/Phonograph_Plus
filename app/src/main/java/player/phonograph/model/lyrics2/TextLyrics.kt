/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import java.util.regex.Pattern

class TextLyrics : AbsLyrics, Parcelable {
    override val type: Int = TXT

    private constructor(parsedLines: MutableList<String>) {
        this.lines = parsedLines
            .ifEmpty {
                Log.w(TAG, "$this has no lyrics")
                ArrayList()
            }
        this.title = DEFAULT_TITLE
    }
    private constructor(lines: MutableList<String>, title: String) {
        this.lines = lines
            .ifEmpty {
                Log.w(TAG, "$this has no lyrics")
                ArrayList()
            }
        this.title = title
    }

    private val lines: MutableList<String>
    private var title: String

    override fun getTitle(): String = title

    override fun getRaw(): String =
        lines.joinToString(separator = "\r\n") { it.trim() }

    override fun getLyricsLineArray(): Array<String> = Array(lines.size) { lines[it] }

    override fun getLyricsTimeArray(): IntArray = IntArray(lines.size) { -1 }

    companion object {
        private const val TAG = "TextLyrics"
        fun from(raw: String): TextLyrics {
            val result = raw.split(Pattern.compile("(\r?\n)|(\\\\[nNrR])"))
            return TextLyrics(result.toMutableList())
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
        parcel.readInt().let { if (it != TXT) throw IllegalStateException("incorrect parcel received") }
        lines = parcel.readArray(String::class.java.classLoader).castToStringMutableList()
        title = parcel.readString() ?: DEFAULT_TITLE
    }
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeArray(getLyricsLineArray())
        parcel.writeString(title)
    }
    override fun describeContents(): Int = 0
}

private fun Array<*>?.castToStringMutableList(): MutableList<String> {
    return this?.let { it.toMutableList() as MutableList<String> } ?: ArrayList<String>() as MutableList<String>
}

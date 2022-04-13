/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable

sealed class AbsLyrics : Parcelable {
    abstract val type: Int

    abstract fun getTitle(): String
    abstract fun getRaw(): String
    abstract fun getLyricsLineArray(): Array<String>
    abstract fun getLyricsTimeArray(): IntArray

    abstract val source: LyricsSource

    // Parcelable
    abstract override fun writeToParcel(parcel: Parcel, flags: Int)
    abstract override fun describeContents(): Int
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<AbsLyrics> {
            override fun createFromParcel(parcel: Parcel): AbsLyrics {
                parcel.readInt().let {
                    return when (it) {
                        TXT -> TextLyrics(parcel)
                        LRC -> LrcLyrics(parcel)
                        else -> throw IllegalStateException("Unknown type of lyrics!")
                    }
                }
            }

            override fun newArray(size: Int): Array<AbsLyrics?> {
                return arrayOfNulls(size)
            }
        }
    }
}

const val LRC: Int = 2
const val TXT: Int = 1
const val DEFAULT_TITLE = "Lyrics"

@JvmInline
value class LyricsSource(val type: Int = UNKNOWN_SOURCE) {
    @Suppress("FunctionName")
    companion object {
        fun Embedded() = LyricsSource(EMBEDDED)
        fun ExternalPrecise() = LyricsSource(EXTERNAL_PRECISE)
        fun ExternalDecorated() = LyricsSource(EXTERNAL_DECORATED)

        const val EMBEDDED = 0
        const val EXTERNAL_PRECISE = 1
        const val EXTERNAL_DECORATED = 2

        fun Unknown() = LyricsSource(UNKNOWN_SOURCE)
        const val UNKNOWN_SOURCE = -1
    }
}

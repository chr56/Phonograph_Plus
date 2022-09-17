/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

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




/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable

data class LyricsPack(val embedded: AbsLyrics?, val external: AbsLyrics?, val externalWithSuffix: AbsLyrics?) : Parcelable {

    fun isEmpty(): Boolean = embedded == null && external == null && externalWithSuffix == null

    fun getLyrics(): AbsLyrics? {
        embedded?.let {
            return it
        }
        external?.let {
            return it
        }
        externalWithSuffix?.let {
            return it
        }
        return null
    }
    fun getLrcLyrics(): LrcLyrics? {
        embedded?.let {
            if (it is LrcLyrics) return it
        }
        external?.let {
            if (it is LrcLyrics) return it
        }
        externalWithSuffix?.let {
            if (it is LrcLyrics) return it
        }
        return null
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(AbsLyrics::class.java.classLoader),
        parcel.readParcelable(AbsLyrics::class.java.classLoader),
        parcel.readParcelable(AbsLyrics::class.java.classLoader)
    ) override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(embedded, flags)
        parcel.writeParcelable(external, flags)
        parcel.writeParcelable(externalWithSuffix, flags)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LyricsPack> {
            override fun createFromParcel(parcel: Parcel): LyricsPack {
                return LyricsPack(parcel)
            }
            override fun newArray(size: Int): Array<LyricsPack?> {
                return arrayOfNulls(size)
            }
        }
    }
}

/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable

data class LyricsPack(val embedded: AbsLyrics?, val external: AbsLyrics?, val externalWithSuffix: AbsLyrics?) : Parcelable {

    fun isEmpty(): Boolean = embedded == null && external == null && externalWithSuffix == null

    /**
     * @return Pair of (lyrics,lyrics_source)
     */
    fun getAvailableLyrics(): Pair<AbsLyrics?, Int> {
        embedded?.let {
            return Pair(it, EMBEDDED)
        }
        external?.let {
            return Pair(it, EXTERNAL)
        }
        externalWithSuffix?.let {
            return Pair(it, EXTERNAL_WITH_SUFFIX)
        }
        return Pair(null, UNKNOWN_SOURCE)
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
    fun getByType(type: Int): AbsLyrics? {
        return when (type) {
            EMBEDDED -> embedded
            EXTERNAL -> external
            EXTERNAL_WITH_SUFFIX -> externalWithSuffix
            else -> null
        }
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
        const val EMBEDDED = 0
        const val EXTERNAL = 1
        const val EXTERNAL_WITH_SUFFIX = 2

        const val UNKNOWN_SOURCE = -1
    }
}

/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import player.phonograph.model.lyrics2.LyricsSource.Companion.EMBEDDED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_DECORATED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_PRECISE

data class LyricsPack(val embedded: LyricsWithSource? = null, val external: List<LyricsWithSource>? = null) : Parcelable {

    fun isEmpty(): Boolean =
        embedded == null && external.isNullOrEmpty()

    fun getAvailableLyrics(): LyricsWithSource? {
        embedded?.let {
            return embedded
        }
        external?.let {
            if (external.isNotEmpty()) return external[0]
        }
        return null
    }

    fun getLrcLyrics(): LrcLyrics? {
        embedded?.let {
            if (it.lyrics is LrcLyrics) return it.lyrics
        }
        if (!external.isNullOrEmpty()) {
            for (l in external) {
                if (l.lyrics is LrcLyrics) return l.lyrics
            }
        }
        return null
    }
    fun getByType(type: LyricsSource): AbsLyrics? {
        return when (type.type) {
            EMBEDDED -> embedded?.lyrics
            EXTERNAL_PRECISE, EXTERNAL_DECORATED -> { // todo
                if (external.isNullOrEmpty()) null else external[0].lyrics
            }
            else -> null
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LyricsWithSource::class.java.classLoader),
        parcel.createTypedArrayList(LyricsWithSource.CREATOR)
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(embedded, flags)
        parcel.writeTypedList(external)
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

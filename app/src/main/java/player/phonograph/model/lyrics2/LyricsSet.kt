/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import player.phonograph.model.lyrics2.LyricsSource.Companion.EMBEDDED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_DECORATED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_PRECISE

data class LyricsSet(val embedded: Lyrics? = null, val external: List<Lyrics>? = null) : Parcelable {

    fun isEmpty(): Boolean =
        embedded == null && external.isNullOrEmpty()

    fun getAvailableLyrics(): Lyrics? {
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
            if (it.content is LrcLyrics) return it.content
        }
        if (!external.isNullOrEmpty()) {
            for (l in external) {
                if (l.content is LrcLyrics) return l.content
            }
        }
        return null
    }
    fun getByType(type: LyricsSource): AbsLyrics? {
        return when (type.type) {
            EMBEDDED -> embedded?.content
            EXTERNAL_PRECISE, EXTERNAL_DECORATED -> { // todo
                if (external.isNullOrEmpty()) null else external[0].content
            }
            else -> null
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Lyrics::class.java.classLoader),
        parcel.createTypedArrayList(Lyrics.CREATOR)
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(embedded, flags)
        parcel.writeTypedList(external)
    }
    override fun describeContents(): Int = 0
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LyricsSet> {
            override fun createFromParcel(parcel: Parcel): LyricsSet {
                return LyricsSet(parcel)
            }

            override fun newArray(size: Int): Array<LyricsSet?> {
                return arrayOfNulls(size)
            }
        }
    }
}

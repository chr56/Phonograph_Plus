/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LyricsSource.Companion.EMBEDDED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_DECORATED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_PRECISE

data class LyricsList(val list: ArrayList<AbsLyrics> = ArrayList(), val song: Song = Song.EMPTY_SONG) : Parcelable {

    fun isEmpty(): Boolean = list.isEmpty()

    fun getAvailableLyrics(): AbsLyrics? {
        if (list.isNotEmpty()) return list[0]
        return null
    }

    fun getLrcLyrics(): LrcLyrics? {
        if (!list.isNullOrEmpty()) {
            for (l in list) {
                if (l is LrcLyrics) return l
            }
        }
        return null
    }
    fun getByType(type: LyricsSource): AbsLyrics? {
        return when (type.type) {
            EMBEDDED -> {
                return list.firstOrNull {
                    it.source.type == EMBEDDED
                }
            }
            EXTERNAL_PRECISE -> {
                return list.firstOrNull {
                    it.source.type == EXTERNAL_PRECISE
                }
            }
            EXTERNAL_DECORATED -> {
                return list.firstOrNull {
                    it.source.type == EXTERNAL_DECORATED
                }
            }
            else -> null
        }
    }

    fun getAvailableTypes(): Set<LyricsSource>? {
        val all = list.map { it.source }
        return if (all.isEmpty()) null else all.toSet()
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LyricsList> {
            override fun createFromParcel(parcel: Parcel): LyricsList {
                return LyricsList(parcel)
            }

            override fun newArray(size: Int): Array<LyricsList?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(AbsLyrics.CREATOR) as ArrayList<AbsLyrics>,
        parcel.readParcelable(Song::class.java.classLoader)!!
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(list)
        parcel.writeParcelable(song, flags)
    }
    override fun describeContents(): Int = 0
}

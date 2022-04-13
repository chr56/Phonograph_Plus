/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.os.Parcel
import android.os.Parcelable
import player.phonograph.model.lyrics2.LyricsSource.Companion.EMBEDDED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_DECORATED
import player.phonograph.model.lyrics2.LyricsSource.Companion.EXTERNAL_PRECISE

data class LyricsList(val list: ArrayList<Lyrics> = ArrayList()) : Parcelable {

    fun isEmpty(): Boolean = list.isEmpty()

    fun getAvailableLyrics(): Lyrics? {
        if (list.isNotEmpty()) return list[0]
        return null
    }

    fun getLrcLyrics(): LrcLyrics? {
        if (!list.isNullOrEmpty()) {
            for (l in list) {
                if (l.content is LrcLyrics) return l.content
            }
        }
        return null
    }
    fun getByType(type: LyricsSource): AbsLyrics? {
        return when (type.type) {
            EMBEDDED -> {
                return list.firstOrNull {
                    it.source.type == EMBEDDED
                }?.content
            }
            EXTERNAL_PRECISE -> {
                return list.firstOrNull {
                    it.source.type == EXTERNAL_PRECISE
                }?.content
            }
            EXTERNAL_DECORATED -> { // todo
                return list.firstOrNull {
                    it.source.type == EXTERNAL_DECORATED
                }?.content
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
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(Lyrics.CREATOR) as ArrayList<Lyrics>)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(list)
    }
    override fun describeContents(): Int = 0
}

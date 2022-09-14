/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep

open class PlaylistSong : Song {
    val playlistId: Long
    val idInPlayList: Long

    constructor(
        id: Long,
        title: String,
        trackNumber: Int,
        year: Int,
        duration: Long,
        data: String,
        dateAdded: Long,
        dateModified: Long,
        albumId: Long,
        albumName: String?,
        artistId: Long,
        artistName: String?,
        playlistId: Long,
        idInPlayList: Long
    ) : super(id, title, trackNumber, year, duration, data, dateAdded, dateModified, albumId, albumName, artistId, artistName) {
        this.playlistId = playlistId
        this.idInPlayList = idInPlayList
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        if (!super.equals(o)) return false
        val that = o as PlaylistSong
        return if (playlistId != that.playlistId) false else idInPlayList == that.idInPlayList
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + playlistId.toInt()
        result = 31 * result + idInPlayList.toInt()
        return result
    }

    override fun toString(): String {
        return "${super.toString()}==:PlaylistSong{playlistId=$playlistId, idInPlayList=$idInPlayList}"
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(playlistId)
        dest.writeLong(idInPlayList)
    }

    protected constructor(parcel: Parcel) : super(parcel) {
        playlistId = parcel.readLong()
        idInPlayList = parcel.readLong()
    }

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<PlaylistSong> = object : Parcelable.Creator<PlaylistSong> {
            override fun createFromParcel(source: Parcel): PlaylistSong {
                return PlaylistSong(source)
            }

            override fun newArray(size: Int): Array<PlaylistSong?> {
                return arrayOfNulls(size)
            }
        }
        val EMPTY_PLAYLIST_SONG = PlaylistSong(
            id = -1,
            title = "",
            trackNumber = -1,
            year = -1,
            duration = -1,
            data = "",
            dateAdded = -1,
            dateModified = -1,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = "",
            playlistId = -1,
            idInPlayList = -1
        )
    }
}

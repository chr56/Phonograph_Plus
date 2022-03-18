/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class Playlist : Parcelable {
    @JvmField
    val id: Long
    @JvmField
    val name: String

    constructor(id: Long, name: String?) {
        this.id = id
        this.name = name ?: ""
    }

    constructor() {
        id = -1
        name = ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val playlist = other as Playlist
        return if (id != playlist.id) false else name == playlist.name
    }

    override fun hashCode(): Int {
        return 31 * id.toInt() + name.hashCode()
    }

    override fun toString(): String = "Playlist{id=$id, name='$name'}"

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
    }

    protected constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString() ?: ""
    }

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Playlist?> = object : Parcelable.Creator<Playlist?> {
            override fun createFromParcel(source: Parcel): Playlist {
                return Playlist(source)
            }
            override fun newArray(size: Int): Array<Playlist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

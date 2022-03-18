/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import player.phonograph.R
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.util.PlaylistsUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

open class BasePlaylist : AbsPlaylist {

    constructor(id: Long, name: String?) : super(id, name)
    constructor() : super()

    open fun getSongs(context: Context): List<Song> {
        // todo
        return PlaylistSongLoader.getPlaylistSongList(context, id)
    }

    open fun containsSong(context: Context, songId: Long): Boolean {
        // todo
        return PlaylistsUtil.doesPlaylistContain(context, id, songId)
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
    }
    protected constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<BasePlaylist?> = object : Parcelable.Creator<BasePlaylist?> {
            override fun createFromParcel(source: Parcel): BasePlaylist {
                return BasePlaylist(source)
            }
            override fun newArray(size: Int): Array<BasePlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

abstract class AbsPlaylist : Parcelable {
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

    open val type: Int
        get() = 0

    open val iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val playlist = other as BasePlaylist
        return if (id != playlist.id) false else name == playlist.name
    }

    override fun hashCode(): Int = 31 * id.toInt() + name.hashCode()
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
}

/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.PlaylistType
import player.phonograph.model.Song
import java.lang.IllegalStateException

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

sealed class Playlist : Parcelable {

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

    abstract val type: Int
    abstract val iconRes: Int

    abstract fun getSongs(context: Context): List<Song>
    abstract fun containsSong(context: Context, songId: Long): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val playlist = other as Playlist
        return if (id != playlist.id) false else name == playlist.name
    }

    override fun hashCode(): Int = 31 * id.toInt() + name.hashCode()
    override fun toString(): String = "Playlist{id=$id, name='$name'}"

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeLong(id)
        dest.writeString(name)
    }

    constructor(parcel: Parcel) {
        parcel.readInt()
        id = parcel.readLong()
        name = parcel.readString() ?: ""
    }

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Playlist?> = object : Parcelable.Creator<Playlist?> {
            override fun createFromParcel(source: Parcel): Playlist {
                return when (source.readInt()) {
                    PlaylistType.FILE -> { FilePlaylist(source) }
                    PlaylistType.ABS_SMART -> { throw IllegalStateException("Instantiating abstract type of playlist") }
                    PlaylistType.FAVORITE -> { FavoriteSongsPlaylist(source) }
                    PlaylistType.LAST_ADDED -> { LastAddedPlaylist(source) }
                    PlaylistType.HISTORY -> { HistoryPlaylist(source) }
                    PlaylistType.MY_TOP_TRACK -> { MyTopTracksPlaylist(source) }
                    PlaylistType.RANDOM -> { ShuffleAllPlaylist(source) }
                    else -> { throw IllegalStateException("Unknown type of playlist") }
                }
            }
            override fun newArray(size: Int): Array<Playlist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

interface EditablePlaylist : ResettablePlaylist {
    fun removeSong(context: Context, song: Song)
    fun removeSongs(context: Context, songs: List<Song>) {
        for (song in songs) { removeSong(context, song) }
    }
    fun appendSong(context: Context, song: Song)
    fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) { appendSong(context, song) }
    }
    fun moveSong(context: Context, song: Song, from: Int, to: Int)
//    fun insert(context: Context, song: Song, pos: Int)
//    and more todo
}

interface ResettablePlaylist {
    fun clear(context: Context)
}

interface GeneratedPlaylist

/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import util.phonograph.playlist.LegacyPlaylistsUtil
import player.phonograph.R
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import util.phonograph.playlist.PlaylistsManager
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilePlaylist : Playlist, EditablePlaylist {

    var associatedFilePath: String

    constructor(id: Long, name: String?, path: String) : super(id, name) {
        associatedFilePath = path
    }
    constructor() : super() {
        associatedFilePath = "-"
    }

    override fun getSongs(context: Context): List<Song> =
        PlaylistSongLoader.getPlaylistSongList(context, id)

    override fun containsSong(context: Context, songId: Long): Boolean =
        PlaylistsUtil.doesPlaylistContain(context, id, songId)

    override val type: Int
        get() = PlaylistType.FILE

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    override fun removeSong(context: Context, song: Song) =
        LegacyPlaylistsUtil.removeFromPlaylist(context, song, id)

    override fun appendSongs(context: Context, songs: List<Song>) {
        CoroutineScope(Dispatchers.Default).launch {
            PlaylistsManager.appendPlaylist(context, songs, this@FilePlaylist)
        }
    }
    override fun appendSong(context: Context, song: Song) = appendSongs(context, listOf(song))

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) {
        LegacyPlaylistsUtil.moveItem(context, id, from, to)
    }

    override fun clear(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            PlaylistsManager.deletePlaylistWithGuide(context, listOf(this@FilePlaylist))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val otherPlaylist = other as FilePlaylist
        return super.equals(other) && otherPlaylist.associatedFilePath == associatedFilePath
    }
    override fun hashCode(): Int =
        super.hashCode() * 31 + associatedFilePath.hashCode()
    override fun toString(): String =
        "Playlist{id=$id, name='$name', path='$associatedFilePath'}"

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(associatedFilePath)
    }

    constructor(parcel: Parcel) : super(parcel) {
        associatedFilePath = parcel.readString() ?: ""
    }

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<FilePlaylist?> = object : Parcelable.Creator<FilePlaylist?> {
            override fun createFromParcel(source: Parcel): FilePlaylist {
                return FilePlaylist(source)
            }
            override fun newArray(size: Int): Array<FilePlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

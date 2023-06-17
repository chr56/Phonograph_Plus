/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.model.Song
import util.phonograph.playlist.PlaylistsManager
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FilePlaylist : Playlist, EditablePlaylist {

    val associatedFilePath: String

    val dateAdded: Long
    val dateModified: Long

    constructor(id: Long, name: String?, path: String, dateAdded: Long, dateModified: Long) : super(id, name) {
        this.associatedFilePath = path
        this.dateAdded = dateAdded
        this.dateModified = dateModified
    }

    override fun getSongs(context: Context): List<Song> =
        PlaylistSongLoader.getPlaylistSongList(context, id)

    override fun containsSong(context: Context, songId: Long): Boolean =
        PlaylistsManagement.doesPlaylistContain(context, id, songId)

    override val type: Int
        get() = PlaylistType.FILE

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    override fun removeSong(context: Context, song: Song) = runBlocking {
        removeFromPlaylistViaMediastore(context, song, id)
        Unit
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        CoroutineScope(Dispatchers.Default).launch {
            PlaylistsManager.appendPlaylist(context, songs, this@FilePlaylist)
        }
    }

    override fun appendSong(context: Context, song: Song) = appendSongs(context, listOf(song))

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) {
        runBlocking {
            moveItemViaMediastore(context, id, from, to)
        }
    }

    override fun clear(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            PlaylistsManager.deletePlaylistWithGuide(context, listOf(this@FilePlaylist))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilePlaylist) return false
        if (!super.equals(other)) return false

        if (associatedFilePath != other.associatedFilePath) return false
        if (dateAdded != other.dateAdded) return false
        if (dateModified != other.dateModified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + associatedFilePath.hashCode()
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + dateModified.hashCode()
        return result
    }

    override fun toString(): String {
        return "FilePlaylist(associatedFilePath='$associatedFilePath', dateAdded=$dateAdded, dateModified=$dateModified)"
    }


    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(associatedFilePath)
        dest.writeLong(dateAdded)
        dest.writeLong(dateModified)
    }

    constructor(parcel: Parcel) : super(parcel) {
        associatedFilePath = parcel.readString() ?: ""
        dateAdded = parcel.readLong()
        dateModified = parcel.readLong()
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

        val EMPTY_PLAYLIST = FilePlaylist(id = -1, name = "N/A", path = "-", dateAdded = -1, dateModified = -1)
    }
}

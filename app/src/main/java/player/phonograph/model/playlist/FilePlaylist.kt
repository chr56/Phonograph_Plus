/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.repo.mediastore.playlist.FilePlaylistImpl
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

abstract class FilePlaylist : Playlist, EditablePlaylist {

    val associatedFilePath: String

    val dateAdded: Long
    val dateModified: Long

    constructor(id: Long, name: String?, path: String, dateAdded: Long, dateModified: Long) : super(id, name) {
        this.associatedFilePath = path
        this.dateAdded = dateAdded
        this.dateModified = dateModified
    }

    override val type: Int
        get() = PlaylistType.FILE

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    abstract val mediastoreUri: Uri

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
                return FilePlaylistImpl(source)
            }

            override fun newArray(size: Int): Array<FilePlaylist?> {
                return arrayOfNulls(size)
            }
        }

    }
}

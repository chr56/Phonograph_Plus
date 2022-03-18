/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.R
import player.phonograph.loader.LastAddedLoader
import player.phonograph.loader.TopAndRecentlyPlayedTracksLoader
import player.phonograph.provider.FavoriteSongsStore
import player.phonograph.provider.HistoryStore
import player.phonograph.provider.SongPlayCountStore
import player.phonograph.util.MediaStoreUtil

interface EditablePlaylist : ResettablePlaylist {
//    fun move()
//    fun remove()
//    fun add()
//    fun todo
}

interface ResettablePlaylist {
    fun clear(context: Context)
}

interface GeneratedPlaylist

class FilePlaylist : BasePlaylist, EditablePlaylist {

    var associatedFilePath: String

    constructor(id: Long, name: String?, path: String) : super(id, name) {
        associatedFilePath = path
    }
    constructor() : super() {
        associatedFilePath = ""
    }

    override val type: Int
        get() = 1

    override fun clear(context: Context) {
        TODO()
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

abstract class AutoPlaylist : BasePlaylist {
    constructor() : super()
    constructor(id: Long, name: String?) : super(id, name)
    override val type: Int
        get() = 2
    constructor(parcel: Parcel) : super(parcel)
}

class FavoriteSongsPlaylist : AutoPlaylist, EditablePlaylist {

    constructor(context: Context) : super(
        "favorites".hashCode() * 31L + R.drawable.ic_favorite_border_white_24dp,
        context.getString(R.string.favorites)
    )

    override var iconRes: Int = R.drawable.ic_favorite_border_white_24dp

    override fun getSongs(context: Context): List<Song> {
        return FavoriteSongsStore.instance.getAllSongs(context)
    }
    override fun clear(context: Context) {
        FavoriteSongsStore.instance.clear()
    }

    override fun toString(): String = "FavoritePlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<FavoriteSongsPlaylist?> = object : Parcelable.Creator<FavoriteSongsPlaylist?> {
            override fun createFromParcel(source: Parcel): FavoriteSongsPlaylist {
                return FavoriteSongsPlaylist(source)
            }
            override fun newArray(size: Int): Array<FavoriteSongsPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

class LastAddedPlaylist : AutoPlaylist {
    constructor(context: Context) : super(
        "last_added".hashCode() * 31L + R.drawable.ic_library_add_white_24dp,
        context.getString(R.string.last_added)
    )

    override var iconRes: Int = R.drawable.ic_library_add_white_24dp

    override fun getSongs(context: Context): List<Song> {
        return LastAddedLoader.getLastAddedSongs(context)
    }

    override fun toString(): String = "LastAddedPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<LastAddedPlaylist?> = object : Parcelable.Creator<LastAddedPlaylist?> {
            override fun createFromParcel(source: Parcel): LastAddedPlaylist {
                return LastAddedPlaylist(source)
            }
            override fun newArray(size: Int): Array<LastAddedPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

class HistoryPlaylist : AutoPlaylist, ResettablePlaylist {
    constructor(context: Context) : super(
        "recently_played".hashCode() * 31L + R.drawable.ic_access_time_white_24dp,
        context.getString(R.string.history)
    )

    override var iconRes: Int = R.drawable.ic_access_time_white_24dp

    override fun getSongs(context: Context): List<Song> {
        return TopAndRecentlyPlayedTracksLoader.getRecentlyPlayedTracks(context)
    }
    override fun clear(context: Context) {
        HistoryStore.getInstance(context).clear()
    }

    override fun toString(): String = "HistoryPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<HistoryPlaylist?> = object : Parcelable.Creator<HistoryPlaylist?> {
            override fun createFromParcel(source: Parcel): HistoryPlaylist {
                return HistoryPlaylist(source)
            }
            override fun newArray(size: Int): Array<HistoryPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

class MyTopTracksPlaylist : AutoPlaylist, ResettablePlaylist {
    constructor(context: Context) : super(
        "top_tracks".hashCode() * 31L + R.drawable.ic_trending_up_white_24dp,
        context.getString(R.string.my_top_tracks)
    )

    override var iconRes: Int = R.drawable.ic_trending_up_white_24dp

    override fun getSongs(context: Context): List<Song> {
        return TopAndRecentlyPlayedTracksLoader.getTopTracks(context)
    }
    override fun clear(context: Context) {
        SongPlayCountStore.getInstance(context).clear()
    }

    override fun toString(): String = "MyTopTracksPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<MyTopTracksPlaylist?> = object : Parcelable.Creator<MyTopTracksPlaylist?> {
            override fun createFromParcel(source: Parcel): MyTopTracksPlaylist {
                return MyTopTracksPlaylist(source)
            }
            override fun newArray(size: Int): Array<MyTopTracksPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

class ShuffleAllPlaylist : AutoPlaylist {
    constructor(context: Context) : super(
        "shuffle_all".hashCode() * 31L + R.drawable.ic_shuffle_white_24dp,
        context.getString(R.string.action_shuffle_all)
    )

    override val iconRes: Int = R.drawable.ic_shuffle_white_24dp

    override fun getSongs(context: Context): List<Song> {
        return MediaStoreUtil.getAllSongs(context) as List<Song>
    }

    override fun toString(): String = "ShuffleAllPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<ShuffleAllPlaylist?> = object : Parcelable.Creator<ShuffleAllPlaylist?> {
            override fun createFromParcel(source: Parcel): ShuffleAllPlaylist {
                return ShuffleAllPlaylist(source)
            }
            override fun newArray(size: Int): Array<ShuffleAllPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

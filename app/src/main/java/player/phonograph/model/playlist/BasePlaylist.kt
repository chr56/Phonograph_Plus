/*
 *  Copyright (c) 2022~2023 chr_56 & Karim Abou Zeid (kabouzeid)
 */

package player.phonograph.model.playlist

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersHolder
import player.phonograph.model.Displayable
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

sealed class Playlist : Parcelable, Displayable {

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
        name = "N/A"
    }

    abstract val type: Int
    abstract val iconRes: Int

    abstract suspend fun getSongs(context: Context): List<Song>
    abstract suspend fun containsSong(context: Context, songId: Long): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val playlist = other as Playlist
        return if (id != playlist.id) false else name == playlist.name
    }

    override fun hashCode(): Int = 31 * id.toInt() + name.hashCode()
    override fun toString(): String = "Playlist{id=$id, name='$name'}"

    suspend fun infoString(context: Context): String {
        val songs = getSongs(context)
        val duration = songs.totalDuration()
        return buildInfoString(
            songCountString(context, songs.size),
            getReadableDurationString(duration)
        )
    }

    override fun getItemID(): Long = id
    override fun getDisplayTitle(context: Context): CharSequence = name
    override fun getDescription(context: Context): CharSequence? = null

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
                    PlaylistType.FILE         -> GlobalContext.get().get { ParametersHolder(mutableListOf(source)) }
                    PlaylistType.ABS_SMART    -> throw IllegalStateException("Instantiating abstract type of playlist")
                    PlaylistType.FAVORITE     -> SmartPlaylist.favoriteSongsPlaylist
                    PlaylistType.LAST_ADDED   -> SmartPlaylist.lastAddedPlaylist
                    PlaylistType.HISTORY      -> SmartPlaylist.historyPlaylist
                    PlaylistType.MY_TOP_TRACK -> SmartPlaylist.myTopTracksPlaylist
                    PlaylistType.RANDOM       -> SmartPlaylist.shuffleAllPlaylist
                    else                      -> throw IllegalStateException("Unknown type of playlist")
                }
            }

            override fun newArray(size: Int): Array<Playlist?> {
                return arrayOfNulls(size)
            }
        }
    }
}
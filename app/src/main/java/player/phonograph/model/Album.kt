package player.phonograph.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import java.lang.Exception
import java.util.ArrayList
import player.phonograph.App.Companion.instance
import player.phonograph.helper.menu.SongsMenuHelper.handleMenuClick
import player.phonograph.interfaces.Displayable
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class Album : Parcelable, Displayable {

    val id: Long
    @JvmField val songs: List<Song>

    constructor(id: Long, songs: List<Song>) {
        this.id = id
        this.songs = songs
    }
    constructor() {
        songs = ArrayList()
        this.id = -1
    }

    fun getSongCount(): Int = songs.size

    val title: String
        get() = safeGetFirstSong().albumName!!
    val artistId: Long
        get() = safeGetFirstSong().artistId
    val artistName: String
        get() = safeGetFirstSong().artistName!!
    val year: Int
        get() = safeGetFirstSong().year
    val dateModified: Long
        get() = safeGetFirstSong().dateModified

    fun safeGetFirstSong(): Song = if (songs.isNullOrEmpty()) Song.EMPTY_SONG else songs[0]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Album
        return songs == that.songs
    }

    override fun hashCode(): Int = songs.hashCode()
    override fun toString(): String = "Album{name=$title,id=$id,artist=$artistName}"

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(songs)
        dest.writeLong(id)
    }

    constructor(parcel: Parcel) {
        songs = parcel.createTypedArrayList(Song.CREATOR) ?: throw Exception("Fail to recreate Album from song")
        id = parcel.readLong()
    }

    override fun getItemID(): Long = id

    override fun getDisplayTitle(): CharSequence = title

    override fun getDescription(): CharSequence = MusicUtil.buildInfoString(artistName, MusicUtil.getSongCountString(instance, songs.size))

    override fun getPic(): Uri? = null // todo

    override fun getSortOrderReference(): String = title // todo

    override fun menuRes(): Int = 0 // todo

    override fun menuHandler(): ((AppCompatActivity, Displayable, Int) -> Boolean)? = null // todo album menu action

    @Suppress("UNCHECKED_CAST")
    override fun multiMenuHandler(): (AppCompatActivity, List<Displayable>, Int) -> Boolean =
        { appCompatActivity: AppCompatActivity?, list: List<Displayable>?, integer: Int? ->
            handleMenuClick(
                appCompatActivity!!, MusicUtil.getAlbumSongList(list as List<Album>?), integer!!
            )
        } // todo more variety

    override fun clickHandler(): (FragmentActivity, Displayable, List<Displayable>?) -> Unit {
        return { fragmentActivity: FragmentActivity?, displayable: Displayable, _: List<Displayable>? ->
            NavigationUtil.goToAlbum(
                fragmentActivity!!, (displayable as Album).id
            ) // todo animate
        }
    }

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Album> = object : Parcelable.Creator<Album> {
            override fun createFromParcel(source: Parcel): Album {
                return Album(source)
            }

            override fun newArray(size: Int): Array<Album?> {
                return arrayOfNulls(size)
            }
        }
    }
}

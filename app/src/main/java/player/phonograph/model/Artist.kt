package player.phonograph.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import player.phonograph.App.Companion.instance
import player.phonograph.helper.menu.SongsMenuHelper.handleMenuClick
import player.phonograph.interfaces.Displayable
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class Artist : Parcelable, Displayable {
    @JvmField val albums: List<Album>?

    constructor(albums: List<Album>?) {
        this.albums = albums
    }

    constructor() {
        albums = ArrayList()
    }

    val name: String
        get() {
            val name = safeGetFirstAlbum().artistName
            return if (MusicUtil.isArtistNameUnknown(name)) {
                UNKNOWN_ARTIST_DISPLAY_NAME
            } else name
        }
    val id: Long
        get() = safeGetFirstAlbum().artistId
    val songs: List<Song>
        get() {
            val songs: MutableList<Song> = ArrayList()
            for (album in albums!!) {
                songs.addAll(album.songs!!)
            }
            return songs
        }
    val songCount: Int
        get() {
            var songCount = 0
            for (album in albums!!) {
                songCount += album.getSongCount()
            }
            return songCount
        }
    val albumCount: Int
        get() = albums?.size ?: -1

    fun safeGetFirstAlbum(): Album {
        return if (albums!!.isEmpty()) Album() else albums[0]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val artist = other as Artist
        return if (albums != null) albums == artist.albums else artist.albums == null
    }

    override fun hashCode(): Int = albums?.hashCode() ?: 0

    override fun toString(): String = "Artist{name=$name,albums=$albums}"

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeTypedList(albums)

    protected constructor(parcel: Parcel) {
        albums = parcel.createTypedArrayList(Album.CREATOR)
    }

    override fun getItemID(): Long = id

    override fun getDisplayTitle(): CharSequence = name

    override fun getDescription(): CharSequence = MusicUtil.getArtistInfoString(instance, this)

    override fun getPic(): Uri? = null // todo

    override fun getSortOrderReference(): String = name // todo

    override fun menuRes(): Int = 0 // todo

    override fun menuHandler(): ((AppCompatActivity, Displayable, Int) -> Boolean)? = null // todo artist menu action

    @Suppress("UNCHECKED_CAST")
    override fun multiMenuHandler(): ((AppCompatActivity, List<Displayable>, Int) -> Boolean) =
        { appCompatActivity: AppCompatActivity?, list: List<Displayable>?, integer: Int? ->
            handleMenuClick(
                appCompatActivity!!, MusicUtil.getArtistSongList(list as List<Artist>?), integer!!
            )
        } // todo more variety

    override fun clickHandler(): (FragmentActivity, Displayable, List<Displayable>?) -> Unit =
        { fragmentActivity: FragmentActivity?, displayable: Displayable, queue: List<Displayable>? ->
            NavigationUtil.goToArtist(
                fragmentActivity!!, (displayable as Artist).id, null
            ) // todo animate
        }

    companion object {
        const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"

        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Artist> = object : Parcelable.Creator<Artist> {
            override fun createFromParcel(source: Parcel): Artist {
                return Artist(source)
            }

            override fun newArray(size: Int): Array<Artist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

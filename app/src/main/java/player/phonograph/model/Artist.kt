package player.phonograph.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import player.phonograph.App
import player.phonograph.R
import player.phonograph.helper.menu.onMultiSongMenuItemClick
import player.phonograph.interfaces.Displayable
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class Artist : Parcelable, Displayable {

    val id: Long
    val name: String
    @JvmField val albums: List<Album>

    constructor(id: Long, name: String?, albums: List<Album>) {
        this.albums = albums
        this.id = id
        this.name = name ?: UNKNOWN_ARTIST_DISPLAY_NAME
    }

    constructor() {
        this.id = -1
        this.name = UNKNOWN_ARTIST_DISPLAY_NAME
        this.albums = ArrayList()
    }

    val albumCount: Int get() = albums.size

    val songs: List<Song>
        get() = albums.flatMap { it.songs }
    val songCount: Int
        get() = albums.fold(0) { i: Int, album: Album -> i + album.songCount }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val artist = other as Artist
        return albums == artist.albums
    }

    override fun hashCode(): Int = albums.hashCode()

    override fun toString(): String = "Artist{name=$name,id =$id,albums=$albums}"

    override fun getItemID(): Long = id

    override fun getDisplayTitle(): CharSequence = name

    override fun getDescription(): CharSequence = MusicUtil.getArtistInfoString(App.instance, this)

    override fun getPic(): Uri? = null // todo

    override fun getSortOrderReference(): String = name // todo

    override fun menuRes(): Int = 0 // todo

    override fun menuHandler(): ((AppCompatActivity, Displayable, Int) -> Boolean)? = null // todo artist menu action

    @Suppress("UNCHECKED_CAST")
    override fun multiMenuHandler(): ((AppCompatActivity, List<Displayable>, Int) -> Boolean) =
        { appCompatActivity: AppCompatActivity?, list: List<Displayable>?, integer: Int? ->
            onMultiSongMenuItemClick(
                appCompatActivity!!, MusicUtil.getArtistSongList(list as List<Artist>), integer!!
            )
        } // todo more variety

    override fun clickHandler(): (FragmentActivity, Displayable, List<Displayable>?, image: ImageView?) -> Unit =
        { fragmentActivity: FragmentActivity?, displayable: Displayable, _: List<Displayable>?, image: ImageView? ->
            if (image != null) {
                NavigationUtil.goToArtist(
                    fragmentActivity!!, (displayable as Artist).id, Pair(image, App.instance.resources.getString(R.string.transition_artist_image))
                )
            } else {
                NavigationUtil.goToArtist(
                    fragmentActivity!!, (displayable as Artist).id
                )
            }
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

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeTypedList(albums)
    }

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString() ?: UNKNOWN_ARTIST_DISPLAY_NAME
        albums = parcel.createTypedArrayList(Album.CREATOR) ?: ArrayList()
    }
}

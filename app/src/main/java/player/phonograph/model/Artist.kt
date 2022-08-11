package player.phonograph.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.App
import player.phonograph.interfaces.Displayable
import player.phonograph.util.MusicUtil

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

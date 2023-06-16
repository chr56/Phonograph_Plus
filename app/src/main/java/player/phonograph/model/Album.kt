package player.phonograph.model

import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class Album : Parcelable, Displayable {

    val id: Long
    val title: String

    @JvmField
    val songs: List<Song>

    constructor(id: Long, title: String?, songs: List<Song>) {
        this.id = id
        this.title = title ?: "UNKNOWN"
        this.songs = songs
    }

    constructor() {
        this.id = -1
        this.title = "UNKNOWN"
        this.songs = ArrayList()
    }

    val songCount: Int
        get() = songs.size

    val artistId: Long
        get() = safeGetFirstSong().artistId
    val artistName: String
        get() = safeGetFirstSong().let {
            if (!it.albumArtistName.isNullOrEmpty()) it.albumArtistName
            else if (!it.artistName.isNullOrEmpty()) it.artistName
            else "UNKNOWN"
        }
    val year: Int
        get() = safeGetFirstSong().year
    val dateModified: Long
        get() = safeGetFirstSong().dateModified

    fun safeGetFirstSong(): Song = if (songs.isEmpty()) Song.EMPTY_SONG else songs[0]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Album
        return songs == that.songs
    }

    override fun hashCode(): Int = songs.hashCode()
    override fun toString(): String = "Album{name=$title,id=$id,artist=$artistName}"

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = title

    override fun getDescription(context: Context): CharSequence = infoString(context)

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

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(songs)
        dest.writeLong(id)
        dest.writeString(title)
    }

    constructor(parcel: Parcel) {
        songs = parcel.createTypedArrayList(Song.CREATOR) ?: throw Exception(
            "Fail to recreate Album from song"
        )
        id = parcel.readLong()
        title = parcel.readString() ?: "UNKNOWN"
    }
}

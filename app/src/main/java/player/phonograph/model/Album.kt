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
    val songCount: Int

    constructor(id: Long, title: String?, songCount: Int) {
        this.id = id
        this.title = title ?: "UNKNOWN"
        this.songCount = songCount
    }

    constructor() {
        this.id = -1
        this.title = "UNKNOWN"
        this.songCount = -1
    }


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

    fun safeGetFirstSong(): Song = Song.EMPTY_SONG //todo

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Album) return false

        if (id != other.id) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

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
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeInt(songCount)
    }

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        title = parcel.readString() ?: "UNKNOWN"
        songCount = parcel.readInt()
    }
}

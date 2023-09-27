package player.phonograph.model

import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class Artist : Parcelable, Displayable {

    val id: Long
    val name: String
    @JvmField
    val albumCount: Int
    @JvmField
    val songCount: Int

    constructor(id: Long, name: String?, albumCount: Int, songCount: Int) {
        this.id = id
        this.name = name ?: UNKNOWN_ARTIST_DISPLAY_NAME
        this.albumCount = albumCount
        this.songCount = songCount
    }

    constructor() {
        this.id = -1
        this.name = UNKNOWN_ARTIST_DISPLAY_NAME
        this.albumCount = -1
        this.songCount = -1
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Artist) return false

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode() * 17 + id.toInt()

    override fun toString(): String = "Artist{name=$name,id =$id,albumsCount=$albumCount}"

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = name

    override fun getDescription(context: Context): CharSequence = infoString(context)

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
        dest.writeInt(albumCount)
        dest.writeInt(songCount)
    }

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString() ?: UNKNOWN_ARTIST_DISPLAY_NAME
        albumCount = parcel.readInt()
        songCount = parcel.readInt()
    }
}

package player.phonograph.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.util.MusicUtil.getGenreInfoString

class Genre : Parcelable, Displayable {

    @JvmField val id: Long
    @JvmField val name: String?
    @JvmField val songCount: Int

    constructor(id: Long, name: String?, songCount: Int) {
        this.id = id
        this.name = name
        this.songCount = songCount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val genre = other as Genre
        if (id != genre.id) return false
        return if (name != genre.name) false else songCount == genre.songCount
    }

    override fun hashCode(): Int = name.hashCode() + id.toInt() * 31 + songCount * 31
    override fun toString(): String = "Genre{id=$id, name='$name', songCount=$songCount'}"

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeInt(songCount)
    }
    protected constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString()!!
        songCount = parcel.readInt()
    }

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = name ?: "UNKNOWN GENRE $id"

    override fun getDescription(context: Context): CharSequence = getGenreInfoString(context, this)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Genre> = object : Parcelable.Creator<Genre> {
            override fun createFromParcel(source: Parcel): Genre {
                return Genre(source)
            }

            override fun newArray(size: Int): Array<Genre?> {
                return arrayOfNulls(size)
            }
        }
    }
}

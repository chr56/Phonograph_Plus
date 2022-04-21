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

    override fun getDisplayTitle(): CharSequence = name ?: "UNKNOWN GENRE $id"

    override fun getDescription(): CharSequence = MusicUtil.getGenreInfoString(instance, this)

    override fun getPic(): Uri? = null

    override fun getSortOrderReference(): String? = null

    override fun menuRes(): Int = 0

    override fun menuHandler(): ((AppCompatActivity, Displayable, Int) -> Boolean)? = null

    @Suppress("UNCHECKED_CAST")
    override fun multiMenuHandler(): (AppCompatActivity, List<Displayable>, Int) -> Boolean =
        { appCompatActivity: AppCompatActivity?, list: List<Displayable>?, integer: Int? ->
            handleMenuClick(appCompatActivity!!, MusicUtil.getGenreSongList(list as List<Genre>?), integer!!)
            true
        }

    override fun clickHandler(): (FragmentActivity, Displayable, List<Displayable>?) -> Unit =
        { fragmentActivity: FragmentActivity?, displayable: Displayable?, list: List<Displayable>? ->
            NavigationUtil.goToGenre(
                fragmentActivity!!, displayable as Genre?
            )
        }

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

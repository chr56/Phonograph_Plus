package player.phonograph.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.util.menu.onSongMenuItemClick
import player.phonograph.util.menu.onMultiSongMenuItemClick
import player.phonograph.interfaces.Displayable
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.MusicUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class Song : Parcelable, Displayable {
    @JvmField
    val id: Long
    @JvmField
    val title: String
    @JvmField
    val trackNumber: Int
    @JvmField
    val year: Int
    @JvmField
    val duration: Long
    @JvmField
    val data: String
    @JvmField
    val dateAdded: Long
    @JvmField
    val dateModified: Long
    @JvmField
    val albumId: Long
    @JvmField
    val albumName: String?
    @JvmField
    val artistId: Long
    @JvmField
    val artistName: String?

    constructor(
        id: Long,
        title: String,
        trackNumber: Int,
        year: Int,
        duration: Long,
        data: String,
        dateAdded: Long,
        dateModified: Long,
        albumId: Long,
        albumName: String?,
        artistId: Long,
        artistName: String?
    ) {
        this.id = id
        this.title = title
        this.trackNumber = trackNumber
        this.year = year
        this.duration = duration
        this.data = data
        this.dateAdded = dateAdded
        this.dateModified = dateModified
        this.albumId = albumId
        this.albumName = albumName
        this.artistId = artistId
        this.artistName = artistName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val song = o as Song
        if (id != song.id) return false
        if (trackNumber != song.trackNumber) return false
        if (year != song.year) return false
        if (duration != song.duration) return false
        if (dateAdded != song.dateAdded) return false
        if (dateModified != song.dateModified) return false
        if (albumId != song.albumId) return false
        if (artistId != song.artistId) return false
        if (title != song.title) return false
        if (data != song.data) return false
        if (if (albumName != null) albumName != song.albumName else song.albumName != null) return false
        return if (artistName != null) artistName == song.artistName else song.artistName == null
    }

    override fun hashCode(): Int {
        var result = id.toInt()
        result = 31 * result + title.hashCode()
        result = 31 * result + trackNumber
        result = 31 * result + year
        result = 31 * result + (duration xor (duration ushr 32)).toInt()
        result = 31 * result + data.hashCode()
        result = 31 * result + (dateModified xor (dateModified ushr 32)).toInt()
        result = 31 * result + (dateAdded xor (dateAdded ushr 32)).toInt()
        result = 31 * result + albumId.toInt()
        result = 31 * result + (albumName?.hashCode() ?: 0)
        result = 31 * result + artistId.toInt()
        result = 31 * result + (artistName?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Song{id=$id, title='$title', trackNumber=$trackNumber, year=$year, duration=$duration, data='$data', dateModified=$dateModified, dataAdded=$dateAdded, albumId=$albumId, albumName='$albumName', artistId=$artistId, artistName='$artistName'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeInt(trackNumber)
        dest.writeInt(year)
        dest.writeLong(duration)
        dest.writeString(data)
        dest.writeLong(dateAdded)
        dest.writeLong(dateModified)
        dest.writeLong(albumId)
        dest.writeString(albumName)
        dest.writeLong(artistId)
        dest.writeString(artistName)
    }

    protected constructor(parcel: Parcel) {
        id = parcel.readLong()
        title = parcel.readString()!!
        trackNumber = parcel.readInt()
        year = parcel.readInt()
        duration = parcel.readLong()
        data = parcel.readString()!!
        dateAdded = parcel.readLong()
        dateModified = parcel.readLong()
        albumId = parcel.readLong()
        albumName = parcel.readString()
        artistId = parcel.readLong()
        artistName = parcel.readString()
    }

    override fun getItemID(): Long = id

    override fun getDisplayTitle(): CharSequence = title

    override fun getDescription(): CharSequence? = MusicUtil.getSongInfoString(this)

    override fun getPic(): Uri? {
        return null // todo
    }

    override fun getSortOrderReference(): String? = title // todo

    override fun menuRes(): Int = R.menu.menu_item_song_short

    override fun menuHandler(): ((AppCompatActivity, Displayable, Int) -> Boolean)? {
        return { appCompatActivity: AppCompatActivity?, displayable: Displayable?, integer: Int? ->
            onSongMenuItemClick(
                appCompatActivity!!, (displayable as Song?)!!, integer!!
            )
        }
    }

    override fun multiMenuHandler(): ((AppCompatActivity, List<Displayable>, Int) -> Boolean)? {
        return { appCompatActivity: AppCompatActivity?, list: List<Displayable>?, integer: Int? ->
            onMultiSongMenuItemClick(
                appCompatActivity!!, (list as List<Song>), integer!!
            )
        }
    }

    override fun clickHandler(): (FragmentActivity, Displayable, List<Displayable>?, image: ImageView?) -> Unit {
        return { _: FragmentActivity?, displayable: Displayable?, queue: List<Displayable>?, image: ImageView? ->
            queue?.let { MusicPlayerRemote.openQueue(it as List<Song>, queue.indexOf(displayable), true) }
        }
    }

    companion object {
        @JvmField
        val EMPTY_SONG = Song(
            id = -1,
            title = "",
            trackNumber = -1,
            year = -1,
            duration = -1,
            data = "",
            dateAdded = -1,
            dateModified = -1,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = ""
        )

        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<Song> = object : Parcelable.Creator<Song> {
            override fun createFromParcel(source: Parcel): Song {
                return Song(source)
            }
            override fun newArray(size: Int): Array<Song?> {
                return arrayOfNulls(size)
            }
        }
    }
}

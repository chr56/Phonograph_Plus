package player.phonograph.model.smartplaylist

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.provider.FavoriteSongsStore

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class FavoriteSongsPlaylist : AbsSmartPlaylist {
    @DrawableRes
    override var iconRes: Int = R.drawable.ic_favorite_border_white_24dp

    constructor(context: Context) : super(context.getString(R.string.favorites), R.drawable.ic_favorite_border_white_24dp)

    override fun getSongs(context: Context?): MutableList<Song> {
        return FavoriteSongsStore.instance.getAllSongs(context ?: App.instance).toMutableList()
    }
    override fun clear(context: Context?) {
        FavoriteSongsStore.instance.clear()
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + iconRes
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (super.equals(other)) {
            if (javaClass != other.javaClass) {
                return false
            }
            val another = other as FavoriteSongsPlaylist
            return iconRes == another.iconRes
        }
        return false
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(iconRes)
    }

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FavoriteSongsPlaylist?> = object : Parcelable.Creator<FavoriteSongsPlaylist?> {
            override fun createFromParcel(source: Parcel): FavoriteSongsPlaylist {
                return FavoriteSongsPlaylist(source)
            }
            override fun newArray(size: Int): Array<FavoriteSongsPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}

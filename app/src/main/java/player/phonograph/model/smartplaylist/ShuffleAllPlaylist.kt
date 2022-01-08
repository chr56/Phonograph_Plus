package player.phonograph.model.smartplaylist

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.R
import player.phonograph.loader.SongLoader.getAllSongs
import player.phonograph.model.Song

class ShuffleAllPlaylist : AbsSmartPlaylist {

    constructor(context: Context) :
        super(context.getString(R.string.action_shuffle_all), R.drawable.ic_shuffle_white_24dp)

    constructor(`in`: Parcel?) : super(`in`)

    override fun getSongs(context: Context): List<Song?> {
        return getAllSongs(context)
    }

    override fun clear(context: Context) {
        // Shuffle all is not a real "Smart Playlist"
    }

    @Keep
    override fun describeContents(): Int = 0

    companion object {
        @Keep
        @JvmField
        var CREATOR: Parcelable.Creator<ShuffleAllPlaylist> =
            object : Parcelable.Creator<ShuffleAllPlaylist> {
                override fun createFromParcel(source: Parcel): ShuffleAllPlaylist {
                    return ShuffleAllPlaylist(source)
                }

                override fun newArray(size: Int): Array<ShuffleAllPlaylist?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

package player.phonograph.model.smartplaylist

import android.content.Context
import android.os.Parcel
import androidx.annotation.DrawableRes
import player.phonograph.R
import player.phonograph.model.AbsCustomPlaylist
import kotlin.math.abs

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsSmartPlaylist : AbsCustomPlaylist {

    override var iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    constructor(name: String, iconRes: Int) :
        super(-abs(31 * name.hashCode() + iconRes * name.hashCode() * 31 * 31).toLong(), name) {
        this.iconRes = iconRes
    }

    abstract fun clear(context: Context?)

    override fun hashCode(): Int = super.hashCode() * 32 + iconRes
    override fun equals(other: Any?): Boolean {
        if (super.equals(other)) {
            if (javaClass != other.javaClass) {
                return false
            }
            val another = other as AbsSmartPlaylist
            return iconRes == another.iconRes
        }
        return false
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(iconRes)
    }
    protected constructor(parcel: Parcel) : super(parcel) {
        iconRes = parcel.readInt()
    }
}

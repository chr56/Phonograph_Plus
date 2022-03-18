package player.phonograph.model

import android.content.Context
import android.os.Parcel

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsCustomPlaylist : BasePlaylist {
    constructor()
    constructor(id: Long, name: String?) : super(id, name)
    constructor(parcel: Parcel) : super(parcel)

    abstract fun getSongs(context: Context?): MutableList<Song>
}

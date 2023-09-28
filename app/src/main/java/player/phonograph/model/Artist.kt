package player.phonograph.model

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
) : Parcelable, Displayable {

    constructor() : this(-1, UNKNOWN_ARTIST_DISPLAY_NAME, -1, -1)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Artist) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (songCount != other.songCount) return false
        if (albumCount != other.albumCount) return false

        return true
    }

    override fun hashCode(): Int = id.toInt() * 101 + name.hashCode()

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = name

    override fun getDescription(context: Context): CharSequence = infoString(context)

    companion object {
        const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"
    }
}

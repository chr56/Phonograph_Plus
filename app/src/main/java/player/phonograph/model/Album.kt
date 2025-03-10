package player.phonograph.model

import player.phonograph.model.Artist.Companion.UNKNOWN_ARTIST_DISPLAY_NAME
import player.phonograph.util.text.infoString
import player.phonograph.util.text.songCountString
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Parcelize
data class Album(
    val id: Long,
    val title: String = UNKNOWN_ALBUM_DISPLAY_NAME,
    val songCount: Int = -1,
    val artistId: Long = -1,
    val artistName: String? = UNKNOWN_ARTIST_DISPLAY_NAME,
    val year: Int = 0,
    val dateModified: Long = 0,
) : Parcelable, Displayable {


    constructor() : this(-1, UNKNOWN_ALBUM_DISPLAY_NAME)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Album) return false

        if (id != other.id) return false
        if (title != other.title) return false
        if (songCount != other.songCount) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode() * 101 + title.hashCode()

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = title

    override fun getDescription(context: Context): CharSequence = infoString(context)
    override fun getSecondaryText(context: Context): CharSequence? = artistName
    override fun getTertiaryText(context: Context): CharSequence = songCountString(context, songCount)

    override fun defaultSortOrderReference(): String = title

    companion object {
        const val UNKNOWN_ALBUM_DISPLAY_NAME = "Unnamed Album"
    }
}

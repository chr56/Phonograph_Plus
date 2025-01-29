package player.phonograph.model

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

@Parcelize
data class Song(
    @JvmField
    val id: Long,
    @JvmField
    val title: String,
    @JvmField
    val trackNumber: Int,
    @JvmField
    val year: Int,
    @JvmField
    val duration: Long,
    @JvmField
    val data: String,
    @JvmField
    val dateAdded: Long,
    @JvmField
    val dateModified: Long,
    @JvmField
    val albumId: Long,
    @JvmField
    val albumName: String?,
    @JvmField
    val artistId: Long,
    @JvmField
    val artistName: String?,
    @JvmField
    val albumArtistName: String?,
    @JvmField
    val composer: String?,
) : Parcelable, Displayable {

    private constructor() : this(
        id = -1,
        title = "",
        trackNumber = -1,
        year = -1,
        duration = -1,
        data = "",
        dateAdded = -1,
        dateModified = -1,
        albumId = -1,
        albumName = null,
        artistId = -1,
        artistName = null,
        albumArtistName = null,
        composer = null,
    )

    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = title

    override fun getDescription(context: Context): CharSequence = infoString()
    override fun getSecondaryText(context: Context): CharSequence? = albumName
    override fun getTertiaryText(context: Context): CharSequence? = artistName

    override fun defaultSortOrderReference(): String = title

    companion object {

        @JvmStatic
        fun deleted(title: String, path: String) = Song(
            id = -1,
            title = title,
            trackNumber = -1,
            year = -1,
            duration = -1,
            data = path,
            dateAdded = -1,
            dateModified = -1,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = "",
            albumArtistName = null,
            composer = null,
        )
    }
}
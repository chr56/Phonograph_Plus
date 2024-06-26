/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.model.Displayable
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    @JvmField val name: String,
    @JvmField val location: PlaylistLocation,
    @JvmField val dateAdded: Long = -1,
    @JvmField val dateModified: Long = -1,
    @JvmField val size: Int = -1,
    @JvmField val iconRes: Int = R.drawable.ic_queue_music_white_24dp,
) : Parcelable, Displayable {

    @IgnoredOnParcel
    @JvmField
    val id: Long = location.id()

    override fun getItemID(): Long = id
    override fun getDisplayTitle(context: Context): CharSequence = name
    override fun getDescription(context: Context): CharSequence = location.text(context)

    fun isVirtual(): Boolean = location is VirtualPlaylistLocation

    /**
     * @return actual path of this playlist if this playlist is actually file file playlist (null if not)
     */
    fun path() = (location as? FilePlaylistLocation)?.path

    /**
     * @return MediaStore id if available
     */
    fun mediaStoreId(): Long? = (location as? FilePlaylistLocation)?.mediastoreId

}


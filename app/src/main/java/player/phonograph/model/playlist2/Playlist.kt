/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist2

import player.phonograph.R
import player.phonograph.model.Displayable
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val location: PlaylistLocation,
    @JvmField val dateAdded: Long = -1,
    @JvmField val dateModified: Long = -1,
    @JvmField val size: Int = -1,
    @JvmField val iconRes: Int = R.drawable.ic_queue_music_white_24dp,
) : Parcelable, Displayable {


    override fun getItemID(): Long = id
    override fun getDisplayTitle(context: Context): CharSequence = name
    override fun getDescription(context: Context): CharSequence = location.text(context)

    fun isVirtual(): Boolean = location is VirtualPlaylistLocation

    companion object {
        val EMPTY_PLAYLIST = Playlist(0, "EMPTY PLAYLIST", FilePlaylistLocation("/"))
    }
}


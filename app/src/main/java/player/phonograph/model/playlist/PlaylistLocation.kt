/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface PlaylistLocation : Parcelable, Comparable<PlaylistLocation> {
    fun text(context: Context): CharSequence
}

@Parcelize
data class VirtualPlaylistLocation(@PlaylistType val type: Int) : PlaylistLocation {
    override fun text(context: Context): CharSequence = playlistTypeName(context.resources, type)
    override fun toString(): String = "Virtual(type: $type)"
    override fun compareTo(other: PlaylistLocation): Int = 1
}

@Parcelize
data class FilePlaylistLocation(
    val path: String,
    val storageVolume: String,
    val mediastoreId: Long,
) : PlaylistLocation {
    override fun text(context: Context): CharSequence = path
    override fun toString(): String = path
    override fun compareTo(other: PlaylistLocation): Int =
        if (other is FilePlaylistLocation) {
            other.path.compareTo(path)
        } else {
            -1
        }
}
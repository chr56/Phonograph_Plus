/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import lib.storage.textparser.ExternalFilePathParser
import player.phonograph.util.produceSectionedId
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface PlaylistLocation : Parcelable, Comparable<PlaylistLocation> {
    fun id(): Long
    fun text(context: Context): CharSequence
}

@Parcelize
sealed class VirtualPlaylistLocation(@PlaylistType val type: Int) : PlaylistLocation {
    override fun id(): Long = produceSectionedId(type.toLong(), SECTION_VIRTUAL)
    override fun text(context: Context): CharSequence = playlistTypeName(context.resources, type)
    override fun compareTo(other: PlaylistLocation): Int = 1

    data object Favorite : VirtualPlaylistLocation(PLAYLIST_TYPE_FAVORITE)
    data object LastAdded : VirtualPlaylistLocation(PLAYLIST_TYPE_LAST_ADDED)
    data object History : VirtualPlaylistLocation(PLAYLIST_TYPE_HISTORY)
    data object MyTopTrack : VirtualPlaylistLocation(PLAYLIST_TYPE_MY_TOP_TRACK)
    data object Random : VirtualPlaylistLocation(PLAYLIST_TYPE_RANDOM)
}


@Parcelize
data class DatabasePlaylistLocation(val databaseId: Long) : PlaylistLocation {
    override fun id(): Long = produceSectionedId(databaseId, SECTION_DATABASE)
    override fun text(context: Context): CharSequence = "#$databaseId"
    override fun toString(): String = "Database(id: $databaseId)"
    override fun compareTo(other: PlaylistLocation): Int =
        when (other) {
            is DatabasePlaylistLocation -> other.databaseId.compareTo(databaseId)
            is FilePlaylistLocation     -> 0
            else                        -> -1
        }
}

@Parcelize
data class FilePlaylistLocation(
    val path: String,
    val storageVolume: String,
    val mediastoreId: Long,
) : PlaylistLocation {
    override fun id(): Long = produceSectionedId(mediastoreId, SECTION_MEDIASTORE)
    override fun text(context: Context): CharSequence = ExternalFilePathParser.bashPath(path) ?: path
    override fun toString(): String = path
    override fun compareTo(other: PlaylistLocation): Int =
        if (other is FilePlaylistLocation) {
            other.path.compareTo(path)
        } else {
            -1
        }
}

private const val SECTION_MEDIASTORE = 0
private const val SECTION_DATABASE = 1 shl 1
private const val SECTION_VIRTUAL = 1 shl 2
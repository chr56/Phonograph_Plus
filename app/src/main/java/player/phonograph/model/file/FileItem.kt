/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.file

import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef

class FileItem(
    val name: String,
    val location: Location,
    val dateAdded: Long = -1,
    val dateModified: Long = -1,
    val size: Long = -1,
    val content: Content,
) {

    sealed interface Content

    object MediaContent: Content
    class SongContent(val song: Song) : Content
    class PlaylistContent(val playlist: Playlist) : Content
    class FolderContent(var count: Int) : Content

    val isFolder: Boolean get() = content is FolderContent
    val isFile: Boolean get() = content !is FolderContent

    //region Object methods

    // only location matters
    override fun toString(): String = location.toString()
    override fun hashCode(): Int = location.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileItem) return false
        if (location != other.location) return false
        return true
    }

    //endregion

    class SortedComparator(private val sortMode: SortMode) : Comparator<FileItem> {
        override fun compare(a: FileItem?, b: FileItem?): Int {
            if (a == null || b == null) return 0
            return if ((a.isFolder) xor (b.isFolder)) {
                if (a.isFolder) -1 else 1
            } else {
                when (sortMode.sortRef) {
                    SortRef.MODIFIED_DATE -> a.dateModified.compareTo(b.dateModified)
                    SortRef.ADDED_DATE    -> a.dateAdded.compareTo(b.dateAdded)
                    SortRef.SIZE          -> {
                        if (a.isFile && b.isFolder) a.size.compareTo(b.size)
                        else a.name.compareTo(b.name)
                    }

                    else                  -> a.name.compareTo(b.name)
                }.let {
                    if (sortMode.revert) -it else it
                }
            }
        }

    }
}
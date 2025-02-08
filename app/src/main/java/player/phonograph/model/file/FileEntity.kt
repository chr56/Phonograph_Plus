/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

import player.phonograph.App
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting

/**
 * Presenting a file
 */
sealed class FileEntity(
    val location: Location,
    name: String? = null,
    val dateAdded: Long = -1,
    val dateModified: Long = -1,
) : Comparable<FileEntity> {

    companion object {
        private var _currentSortRef: SortMode = SortMode(SortRef.DISPLAY_NAME)
        val currentSortRef: SortMode get() = _currentSortRef
        fun updateSortRef() {
            val preference = Setting(App.instance).Composites[Keys.fileSortMode]
            _currentSortRef = preference.data
        }
    }

    override fun compareTo(other: FileEntity): Int {
        return if ((this is Folder) xor (other is Folder)) {
            if (this is Folder) -1 else 1
        } else {
            when (currentSortRef.sortRef) {
                SortRef.MODIFIED_DATE -> dateModified.compareTo(other.dateModified)
                SortRef.ADDED_DATE    -> dateAdded.compareTo(other.dateAdded)
                SortRef.SIZE          -> {
                    if (this is File && other is File) size.compareTo(other.size)
                    else name.compareTo(other.name)
                }

                else                  -> name.compareTo(other.name)
            }.let {
                if (currentSortRef.revert) -it else it
            }
        }
    }

    val name: String = name ?: location.basePath.takeLastWhile { it != '/' }

    class File(
        location: Location,
        name: String?,
        val id: Long = -1,
        val size: Long = -1,
        dateAdded: Long = -1,
        dateModified: Long = -1,
    ) : FileEntity(location, name, dateAdded, dateModified)

    class Folder(
        location: Location,
        name: String?,
        dateAdded: Long = -1,
        dateModified: Long = -1,
        @JvmSynthetic
        var songCount: Int = -1
    ) : FileEntity(location, name, dateAdded, dateModified)

    // only location matters

    override fun hashCode(): Int = location.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileEntity) return false

        if (location != other.location) return false

        return true
    }
}
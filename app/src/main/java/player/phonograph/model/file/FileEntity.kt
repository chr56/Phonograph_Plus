/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

/**
 * Presenting a file
 */
sealed class FileEntity(
    val location: Location,
    name: String? = null,
    val dateAdded: Long = -1,
    val dateModified: Long = -1,
) {

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
        var songCount: Int = -1,
    ) : FileEntity(location, name, dateAdded, dateModified)

    // only location matters

    override fun hashCode(): Int = location.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileEntity) return false

        if (location != other.location) return false

        return true
    }

    override fun toString(): String = location.toString()
}
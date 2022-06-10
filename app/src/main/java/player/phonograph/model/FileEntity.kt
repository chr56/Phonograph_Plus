/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import java.io.File
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil

/**
 * Presenting a file
 */
sealed class FileEntity : Comparable<FileEntity> {
    abstract val path: Location

    override fun compareTo(other: FileEntity): Int {
        return if (isFolder xor other.isFolder) {
            if (isFolder) -1 else 1
        } else {
            name.compareTo(other.name)
        }
    }

    class File(override val path: Location) : FileEntity() {
        val linkedSong: Song get() = MediaStoreUtil.getSong(App.instance, File(path.absolutePath)) ?: Song.EMPTY_SONG
        override val isFolder: Boolean = false
    }

    class Folder(override val path: Location) : FileEntity() {
        override val isFolder: Boolean = true
    }

    val name: String get() = path.basePath.takeLastWhile { it != '/' }
    abstract val isFolder: Boolean

    override fun hashCode(): Int = path.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileEntity) return false

        if (path != other.path) return false

        return true
    }
}

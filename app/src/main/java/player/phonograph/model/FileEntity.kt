/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil
import java.io.File

/**
 * Presenting a file
 */
sealed class FileEntity {
    abstract val path: Location

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
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
    abstract val location: Location

    override fun compareTo(other: FileEntity): Int {
        return if ((this is Folder) xor (other is Folder)) {
            if (this is Folder) -1 else 1
        } else {
            name.compareTo(other.name)
        }
    }

    class File(override val location: Location, private val mSong: Song? = null) : FileEntity() {
        val linkedSong: Song get() =
            mSong ?: MediaStoreUtil.getSong(App.instance, File(location.absolutePath)) ?: Song.EMPTY_SONG
    }

    class Folder(override val location: Location) : FileEntity()

    val name: String by lazy(LazyThreadSafetyMode.NONE) { location.basePath.takeLastWhile { it != '/' } }

    override fun hashCode(): Int = location.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileEntity) return false

        if (location != other.location) return false

        return true
    }
}

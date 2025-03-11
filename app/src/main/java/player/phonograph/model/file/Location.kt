/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

import lib.storage.extension.rootDirectory
import android.os.Environment
import android.os.storage.StorageVolume

/**
 * Presenting a path
 * @param basePath the path without prefix likes /storage/emulated/0 or /storage/69F4-242C,
 *  **starting with '/', ending without '/'**
 * @param storageVolume StorageVolume where file locate
 */
class Location(val basePath: String, val storageVolume: StorageVolume) {

    val absolutePath: String
        get() {
            val prefix = storageVolume.rootDirectory()?.path ?: Environment.getExternalStorageDirectory().absolutePath
            return "$prefix$basePath"
        }

    /**
     *  null if no parent (already be top / root of this volume)
     */
    val parent: Location?
        get() {
            if (basePath == "/") return null // root
            val parentPath = basePath.dropLastWhile { it != '/' }.removeSuffix("/")
            return changeTo(parentPath)
        }

    /**
     * another base path on same volume
     */
    fun changeTo(basePath: String): Location = Location(basePath.ifBlank { "/" }, storageVolume)

    override fun hashCode(): Int = storageVolume.hashCode() * 31 + basePath.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        if (basePath != other.basePath) return false
        if (storageVolume != other.storageVolume) return false

        return true
    }
    override fun toString(): String = "${storageVolume.uuid}:$basePath"

    val sqlPattern get() = "%${absolutePath}%"
}

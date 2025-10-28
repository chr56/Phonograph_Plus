/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.explorer

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.model.file.Location
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.io.File


object Locations {
    private const val TAG = "Locations"

    fun from(path: String, context: Context): Location =
        from(File(path), context)

    fun from(file: File, context: Context): Location =
        from(file, context.getSystemService<StorageManager>()!!)

    fun from(path: String, storageManager: StorageManager): Location =
        from(File(path), storageManager)

    fun from(file: File, storageManager: StorageManager): Location =
        from(file.absolutePath, file.getStorageVolume(storageManager))

    fun from(absolutePath: String, storageVolume: StorageVolume): Location =
        ActualLocation(absolutePath = absolutePath.ifBlank { "/" }, storageVolume = storageVolume)

    /**
     * get parent of current [location]
     * @return parent Location, null if it is root
     */
    fun parent(location: Location, context: Context): Location? =
        parent(location, context.getSystemService<StorageManager>()!!)

    /**
     * get parent of current [location]
     * @return parent Location, null if it is root
     */
    fun parent(location: Location, storageManager: StorageManager): Location? {
        if (location.isRoot) return null
        val file = File(location.absolutePath)
        val parent = file.parentFile ?: return null
        val storageVolume = parent.getStorageVolume(storageManager)
        return ActualLocation(parent.absolutePath, storageVolume)
    }

    private fun File.getStorageVolume(storageManager: StorageManager): StorageVolume {
        val volume = storageManager.getStorageVolume(this)
        return if (volume != null) {
            volume
        } else {
            Log.e(TAG, "can't find storage volume for file $path")
            storageManager.primaryStorageVolume
        }
    }

    private class ActualLocation(
        override val absolutePath: String,
        storageVolume: StorageVolume,
    ) : Location {

        override val volumeUUID: String = storageVolume.uuid ?: ""

        override val volumeName: String = storageVolume.getDescription(App.instance)

        override val volumeRootPath: String = storageVolume.rootDirectory()?.path ?: "/"

        override val basePath: String
            get() = absolutePath.substringAfter(volumeRootPath)

        override val isRoot: Boolean
            get() = absolutePath == volumeRootPath

        override fun toString(): String = absolutePath
        override fun hashCode(): Int = volumeUUID.hashCode() * 31 + absolutePath.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Location) return false

            if (absolutePath != other.absolutePath) return false
            if (volumeUUID != other.volumeUUID) return false

            return true
        }
    }
}

/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.explorer

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.model.file.MediaPath
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.io.File


object MediaPaths {
    private const val TAG = "MediaPaths"

    fun from(path: String, context: Context): MediaPath =
        from(File(path), context)

    fun from(file: File, context: Context): MediaPath =
        from(file, context.getSystemService<StorageManager>()!!)

    fun from(path: String, storageManager: StorageManager): MediaPath =
        from(File(path), storageManager)

    fun from(file: File, storageManager: StorageManager): MediaPath =
        from(file.canonicalPath.ifBlank { "/" }, file.getStorageVolume(storageManager))

    fun from(canonicalPath: String, storageVolume: StorageVolume, mediastoreId: Long = -1): MediaPath =
        ActualMediaPath(
            path = canonicalPath,
            volume = ActualVolume.from(App.instance, storageVolume),
            mediastoreId = mediastoreId,
        )

    /**
     * get parent of current [mediaPath]
     * @return parent [MediaPath], null if it is root
     */
    fun parent(mediaPath: MediaPath, context: Context): MediaPath? =
        parent(mediaPath, context.getSystemService<StorageManager>()!!)

    /**
     * get parent of current [mediaPath]
     * @return parent [MediaPath], null if it is root
     */
    fun parent(mediaPath: MediaPath, storageManager: StorageManager): MediaPath? {
        if (mediaPath.isRoot) return null
        val file = File(mediaPath.path)
        val parent = file.parentFile ?: return null
        val storageVolume = parent.getStorageVolume(storageManager)
        return ActualMediaPath(parent.absolutePath, ActualVolume.from(App.instance, storageVolume))
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

    private class ActualMediaPath(
        override val path: String,
        override val volume: MediaPath.Volume,
        override val mediastoreId: Long = -1,
    ) : MediaPath {

        override val volumeRoot: String get() = volume.root

        override val basePath: String get() = path.substringAfter(volume.root)

        override val isRoot: Boolean get() = path == volume.root

        override fun toString(): String = path
        override fun hashCode(): Int = path.hashCode() * 31 + volume.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaPath) return false

            if (path != other.path) return false
            if (volume.uuid != other.volume.uuid) return false

            return true
        }
    }

    private class ActualVolume private constructor(
        override val uuid: String,
        override val name: String,
        override val isPrimary: Boolean,
        override val root: String,
    ) : MediaPath.Volume {

        override fun toString(): String = "StorageVolume(${uuid}, $root)"
        override fun hashCode(): Int = uuid.hashCode() * 31 + root.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ActualVolume) return false

            if (uuid != other.uuid) return false
            if (root != other.root) return false
            return true
        }

        companion object {
            fun from(context: Context, storageVolume: StorageVolume): ActualVolume =
                ActualVolume(
                    storageVolume.uuid ?: "",
                    storageVolume.getDescription(context),
                    storageVolume.isPrimary,
                    storageVolume.rootDirectory()?.path ?: "/",
                )
        }

    }
}

/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.explorer

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.model.file.MediaPath
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.core.content.getSystemService
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.io.File


object MediaPaths {
    private const val TAG = "MediaPaths"

    fun startDirectory(context: Context): MediaPath {
        val path = Setting(context.applicationContext)[Keys.startDirectoryPath].data
        return from(path, context)
    }

    fun from(prefix: String, segments: List<String>, context: Context): MediaPath {
        val basePath = segments.joinToString(prefix = "/", separator = "/")
        val path = "$prefix$basePath"
        return from(path, context)
    }

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

    // Discuss: maybe storage volume could be nested?
    // jut mount a volume at another, currently we know that all mounted typically under /storage/ without nesting.
    /**
     * Get current [StorageVolume] from a [root] path.
     * (We presume that they could not be nested.)
     */
    fun volumeOf(context: Context, root: MediaPath): StorageVolume {
        val storageManager = context.getSystemService<StorageManager>()!!
        val rootVolume = storageManager.getStorageVolume(File(root.path))
        return rootVolume ?: storageManager.primaryStorageVolume
    }

    /**
     * get all [StorageVolume]
     */
    fun volumes(context: Context): List<StorageVolume> {
        val storageManager = context.getSystemService<StorageManager>()
        val volumes = storageManager?.storageVolumes
            ?.filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
            ?: emptyList()
        return volumes
    }

    private class ActualMediaPath(
        override val path: String,
        override val volume: MediaPath.Volume,
        override val mediastoreId: Long = -1,
    ) : MediaPath {

        override val volumeRoot: String get() = volume.root

        override val basePath: String get() = path.substringAfter(volume.root)

        override val basePathSegments: List<String> get() = basePath.split("/").filter { it.isNotEmpty() }

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

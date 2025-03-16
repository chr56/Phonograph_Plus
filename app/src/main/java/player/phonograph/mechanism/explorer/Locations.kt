/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.explorer

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.model.file.Location
import player.phonograph.model.file.defaultStartDirectory
import androidx.core.content.getSystemService
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.io.File


object Locations {
    private const val TAG = "Locations"

    val default: Location get() = from(defaultStartDirectory.absolutePath)

    fun from(basePath: String, storageVolume: StorageVolume): Location {
        return ActualLocation(basePath.ifBlank { "/" }, storageVolume)
    }

    fun from(file: File, context: Context = App.instance): Location {
        val storageManager = context.getSystemService<StorageManager>()!!

        val storageVolume = file.getStorageVolume(storageManager)
        val basePath = file.getBasePath(
            storageVolume.rootDirectory() ?: throw IllegalStateException("unavailable for $storageManager")
        )

        return from(basePath, storageVolume)
    }

    fun from(path: String, context: Context = App.instance): Location = from(File(path), context)

    private fun File.getStorageVolume(storageManager: StorageManager): StorageVolume {
        val volume = storageManager.getStorageVolume(this)
        return if (volume != null) {
            volume
        } else {
            Log.e(TAG, "can't find storage volume for file $path")
            storageManager.primaryStorageVolume
        }
    }

    private fun File.getBasePath(root: File): String {
        return path.substringAfter(root.path)
    }

    private class ActualLocation(
        override val basePath: String,
        override val storageVolume: StorageVolume,
    ) : Location {

        override val absolutePath: String
            get() {
                val prefix = storageVolume.rootDirectory()?.path ?: Environment.getExternalStorageDirectory().absolutePath
                return "$prefix$basePath"
            }

        override val parent: Location?
            get() {
                if (basePath == "/") return null // root
                val parentPath = basePath.dropLastWhile { it != '/' }.removeSuffix("/")
                return ActualLocation(parentPath.ifBlank { "/" }, storageVolume)
            }

        override fun toString(): String = "${storageVolume.uuid}:$basePath"
        override fun hashCode(): Int = storageVolume.hashCode() * 31 + basePath.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Location) return false

            if (basePath != other.basePath) return false
            if (storageVolume != other.storageVolume) return false

            return true
        }

    }
}

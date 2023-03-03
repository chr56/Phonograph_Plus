/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

import lib.phonograph.storage.externalStoragePath
import lib.phonograph.storage.root
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.util.FileUtil.defaultStartDirectory
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import java.io.File

/**
 * Presenting a path
 * @param basePath the path without prefix likes /storage/emulated/0 or /storage/69F4-242C,
 *  **starting with '/', ending without '/'**
 * @param storageVolume StorageVolume where file locate
 */
class Location private constructor(val basePath: String, val storageVolume: StorageVolume) {

    val absolutePath: String
        get() {
            val prefix = storageVolume.root()?.path ?: externalStoragePath
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
    fun changeTo(basePath: String): Location = from(basePath, storageVolume)

    companion object {
        private const val TAG = "Location"

        fun from(basePath: String, storageVolume: StorageVolume): Location {
            return Location(basePath.ifBlank { "/" }, storageVolume)
        }

        fun from(file: File) = fromAbsolutePath(file.absolutePath)

        /**
         * @param path absolute path
         */
        fun fromAbsolutePath(path: String, context: Context = App.instance): Location {
            val file = File(path)
            val storageManager = context.getSystemService<StorageManager>()!!

            val storageVolume = file.getStorageVolume(storageManager)
            val basePath = file.getBasePath(storageVolume.root() ?: throw IllegalStateException("unavailable for $storageManager"))
            // path.substringAfter(storageVolume.root()?.path ?: file.getBasePath(context))

            if (DEBUG) Log.w(TAG, "Location Created! path = $path, storageVolume = $storageVolume(${storageVolume.root()})")
            return from(basePath, storageVolume)
        }

        val HOME: Location get() = fromAbsolutePath(defaultStartDirectory.absolutePath)

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
    }

    init {
        if (basePath.isBlank()) {
            Log.e(TAG, "base path is null!")
        }
    }

    override fun hashCode(): Int = storageVolume.hashCode() * 31 + basePath.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        if (basePath != other.basePath) return false
        if (storageVolume != other.storageVolume) return false

        return true
    }
}

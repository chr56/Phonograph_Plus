/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import lib.phonograph.storage.externalStoragePath
import lib.phonograph.storage.getBasePath
import lib.phonograph.storage.getStorageId
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.settings.Setting

/**
 * Presenting a path
 * @param basePath the path without prefix likes /storage/emulated/0 or /storage/69F4-242C,
 *  **starting with '/', ending without '/'**
 * @param storageVolume the location of Storage, such as Internal(`emulated/0`) or physical storage devices (`69F4-242C`)
 */
class Location(val basePath: String, val storageVolume: String?) {

    val absolutePath: String
        get() {
            val prefix = if (storageVolume != null) "/storage/$storageVolume" else externalStoragePath
            return "$prefix$basePath"
        }
    val parent: Location get() {
        val parentPath = basePath.dropLastWhile { it != '/' }.removeSuffix("/")
        return Location(parentPath, storageVolume)
    }

    companion object {

        /**
         * @param path absolute path
         */
        fun fromAbsolutePath(path: String, context: Context = App.instance): Location {
            val f = File(path)
            if (DEBUG) Log.w("Location", "From /${f.getBasePath(context)} @ ${f.getStorageId(context)}")
            return Location("/${f.getBasePath(context)}", getStorageVolume(path))
        }
        @SuppressLint("SdCardPath")
        private fun getStorageVolume(absolutePath: String): String? =
            when {
                absolutePath.startsWith("/sdcard/") -> null
                absolutePath.startsWith("/storage/emulated/") -> {
                    val s = absolutePath.substringAfter("/storage/").split('/')
                    "${s[0]}/${s[1]}"
                }
                else -> absolutePath.substringAfter("/storage/").substringBefore('/')
            }

        val HOME: Location = fromAbsolutePath(Setting.defaultStartDirectory.absolutePath)
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

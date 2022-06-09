/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import android.content.Context
import android.util.Log
import lib.phonograph.storage.externalStoragePath
import lib.phonograph.storage.getBasePath
import lib.phonograph.storage.getStorageId
import player.phonograph.App
import player.phonograph.settings.Setting
import java.io.File

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

    companion object {

        /**
         * @param path absolute path
         */
        fun fromAbsolutePath(path: String, context: Context = App.instance): Location {
            val f = File(path)
            Log.d("Location", "From ${f.getBasePath(context)} @ ${f.getStorageId(context)}")
            return Location(f.getStorageId(context), f.getBasePath(context))
        }

        val HOME: Location = fromAbsolutePath(Setting.defaultStartDirectory.absolutePath)
    }
}
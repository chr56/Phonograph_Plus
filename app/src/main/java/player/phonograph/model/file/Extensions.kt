/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("Util")

package player.phonograph.model.file

import player.phonograph.App
import android.os.Environment
import java.io.File

val defaultStartDirectory: File
    get() {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return if (musicDir != null && musicDir.exists() && musicDir.isDirectory) {
            musicDir
        } else {
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage.exists() && externalStorage.isDirectory) {
                externalStorage
            } else {
                App.instance.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File("/") // root
            }
        }
    }
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.file

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment.DIRECTORY_MUSIC
import android.os.Environment.getDataDirectory
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Environment.getStorageDirectory
import java.io.File


private val DirectoryExternalStorageRoot: File get() = getExternalStorageDirectory()

private val DirectoryMusic: File get() = getExternalStoragePublicDirectory(DIRECTORY_MUSIC)


val DefaultStartDirectory: String
    get() = lookupDirectory(DirectoryMusic)
        ?: lookupDirectory(DirectoryExternalStorageRoot)
        ?: if (SDK_INT >= VERSION_CODES.R) getStorageDirectory().absolutePath else getDataDirectory().absolutePath

private fun lookupDirectory(file: File?): String? =
    if (file != null && file.exists() && file.isDirectory) file.absolutePath else null
/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.file

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Environment
import java.io.File

val defaultStartDirectory: File
    get() = lookupDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
        ?: lookupDirectory(Environment.getExternalStorageDirectory())
        ?: if (SDK_INT >= VERSION_CODES.R) Environment.getStorageDirectory() else Environment.getDataDirectory()

private fun lookupDirectory(file: File?): File? =
    if (file != null && file.exists() && file.isDirectory) file else null
/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.storage

import android.os.Build
import android.os.storage.StorageVolume
import java.io.File

/**
 * @return null if unavailable (for example, unmounted or unsupported)
 */
fun StorageVolume.root(): File? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.directory
    } else {
        try {
            // this.javaClass.getField("mPath")
            this.javaClass.getMethod("getPathFile").invoke(this) as File
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

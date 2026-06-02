/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.file

import android.annotation.SuppressLint
import android.os.Build
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * force move file
 */
fun moveFile(from: File, to: File) {
    require(from.exists()) { "${from.path} doesn't exits!" }
    @SuppressLint("ObsoleteSdkInt")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } else {
        if (from.canWrite()) {
            if (to.exists()) {
                to.delete().also { require(it) { "Can't delete ${to.path}" } }
            }
            from.renameTo(to).also { require(it) { "Restore ${from.path} failed!" } }
        }
    }
}

/**
 * create the file or delete it and create new one if exists
 */
fun File.createOrOverride(recursive: Boolean = false): File {
    if (recursive && parentFile != null) {
        val parentFile = parentFile!!
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
    }
    if (exists()) delete()
    createNewFile()
    return this
}
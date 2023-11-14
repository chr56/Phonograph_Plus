/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util.file

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * create the file or delete it and create new one if exists
 */
fun File.createOrOverrideFile(): File {
    if (exists()) delete()
    createNewFile()
    return this
}

fun File.createOrOverrideFileRecursive(): File {
    val parentFile = parentFile
    if (parentFile != null) {
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
    }

    if (exists()) delete()
    createNewFile()
    return this
}

fun moveFile(from: File, to: File) {
    require(from.exists()) { "${from.path} doesn't exits!" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
 * save [content] to a file from document uri ([dest])
 */
fun saveToFile(dest: Uri, content: String, resolver: ContentResolver) {
    resolver.openFileDescriptor(dest, "wt")?.use { descriptor ->
        FileOutputStream(descriptor.fileDescriptor).use { stream ->
            stream.bufferedWriter().use {
                it.write(content)
                it.flush()
            }
        }
    }
}
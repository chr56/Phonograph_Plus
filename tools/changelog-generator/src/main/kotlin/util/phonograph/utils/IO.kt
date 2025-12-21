/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.utils

import java.io.BufferedWriter
import java.io.File

fun checkFile(file: File, override: Boolean) {
    if (file.exists()) {
        require(file.isFile) { "${file.path} is not a file!" }
        if (override) file.delete()
    } else {
        file.createNewFile()
    }
}

fun writeToFile(string: String, path: String, override: Boolean = true) {
    val file = File(path)
    writeToFile(string, file, override)
}

fun writeToFile(data: String, file: File, override: Boolean = true) {
    checkFile(file, override)
    file.bufferedWriter().use {
        it.write(data)
    }
}

private fun File.bufferedWriter(): BufferedWriter =
    outputStream().writer(Charsets.UTF_8).buffered(4096)
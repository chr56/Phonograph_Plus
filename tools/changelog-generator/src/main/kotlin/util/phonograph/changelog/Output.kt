/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import java.io.BufferedWriter
import java.io.File


fun exportGitHubReleaseMarkDown(string: String, path: String) {
    val file = File(path)
    if (file.exists() && file.isFile) {
        file.delete()
    }
    exportGitHubReleaseMarkDown(string, file)
}

fun exportGitHubReleaseMarkDown(string: String, file: File) {
    file.bufferedWriter().use {
        it.write(string)
    }
}

private fun File.bufferedWriter(): BufferedWriter =
    outputStream().writer(Charsets.UTF_8).buffered(4096)
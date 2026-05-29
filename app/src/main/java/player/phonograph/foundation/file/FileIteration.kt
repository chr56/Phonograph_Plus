/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.file

import kotlinx.coroutines.yield
import java.io.File
import java.io.FileFilter

suspend fun listPaths(
    path: String,
    filter: FileFilter?,
    recursive: Boolean = false,
): Array<String> {
    val directory = File(path)
    val files = if (directory.isDirectory) {
        if (recursive) {
            listFilesDeep(directory, filter)
        } else {
            listFiles(directory, filter)
        }
    } else {
        listOf(directory)
    }
    yield()
    val paths = files.map { file -> safeGetCanonicalPath(file) }.toTypedArray()
    return paths
}

private fun listFiles(directory: File, fileFilter: FileFilter?): List<File> {
    val files: Array<File>? = directory.listFiles(fileFilter)
    return if (files.isNullOrEmpty()) emptyList() else files.toList()
}

private suspend fun listFilesDeep(directory: File, fileFilter: FileFilter?): List<File> {
    val result = mutableListOf<File>()
    val files = directory.listFiles(fileFilter) ?: return emptyList()
    yield()
    for (file in files) {
        if (file.isDirectory) {
            result.addAll(listFilesDeep(file, fileFilter))
        } else {
            result.add(file)
        }
    }
    return if (result.isEmpty()) emptyList() else result
}
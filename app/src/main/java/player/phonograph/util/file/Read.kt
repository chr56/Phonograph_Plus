/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.file

import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.text.DecimalFormat


fun safeGetCanonicalPath(file: File): String = try {
    file.canonicalPath
} catch (e: IOException) {
    e.printStackTrace()
    file.absolutePath
}

fun safeGetCanonicalFile(file: File): File = try {
    file.canonicalFile
} catch (e: IOException) {
    e.printStackTrace()
    file.absoluteFile
}

fun stripExtension(str: String): String {
    val pos = str.lastIndexOf('.')
    return if (pos == -1) str else str.substring(0, pos)
}

fun getReadableFileSize(size: Long): String {
    if (size <= 0) return "$size B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups =
        (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        size / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

val audioFileFilter: FileFilter
    get() = FileFilter { file: File ->
        !file.isHidden && (
                file.isDirectory || file.mimeTypeIs("audio/*") || file.mimeTypeIs("application/ogg")
                )
    }

suspend fun listPaths(
    path: String,
    filter: FileFilter? = audioFileFilter,
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
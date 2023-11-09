/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.file

import kotlin.math.log10
import kotlin.math.pow
import java.io.File
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


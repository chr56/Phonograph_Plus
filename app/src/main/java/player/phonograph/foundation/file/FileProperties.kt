/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.file

import kotlin.math.log10
import kotlin.math.pow
import java.io.File
import java.io.IOException
import java.text.DecimalFormat


fun stripExtension(str: String): String {
    val pos = str.lastIndexOf('.')
    return if (pos == -1) str else str.substring(0, pos)
}

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

fun readableFileSize(size: Long): String {
    if (size <= 0) return "$size B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.##").format(
        size / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

fun readableFileSizeInMB(sizeInBytes: Long): String {
    val sizeInKB = sizeInBytes / 1024
    val sizeInMB = sizeInKB / 1024
    val remainderInMB = (sizeInKB % 1024) / 1024F

    val readableFileSizeInMB = "%.2f".format(sizeInMB + remainderInMB)

    return "$readableFileSizeInMB MB ($sizeInKB KB)"
}


/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

fun String?.bracketedIfAny(): String {
    return if (!isNullOrEmpty()) "($this)" else ""
}

fun getFileSizeString(sizeInBytes: Long): String {

    val sizeInKB = sizeInBytes / 1024
    val sizeInMB = sizeInKB / 1024
    val remainderInMB = (sizeInKB % 1024) / 1024F

    val readableFileSizeInMB = "%.2f".format(sizeInMB + remainderInMB)

    return "$readableFileSizeInMB MB ($sizeInKB KB)"
}
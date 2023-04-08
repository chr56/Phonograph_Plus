/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

fun makeSectionName(reference: String?): String {
    if (reference.isNullOrBlank()) return ""
    var str = reference.trim { it <= ' ' }.lowercase()
    str = when {
        str.startsWith("the ") -> str.substring(4)
        str.startsWith("a ") -> str.substring(2)
        else -> str
    }
    return if (str.isEmpty()) "" else str[0].uppercase()
}
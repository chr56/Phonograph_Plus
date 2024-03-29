/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.text

fun splitMultiTag(source: String): Collection<String> {
    if (source.isEmpty()) return emptySet()
    return source.trim(Char::isWhitespace).split(";", " / ", " & ", "ft. ").map { it.trimStart() }
}
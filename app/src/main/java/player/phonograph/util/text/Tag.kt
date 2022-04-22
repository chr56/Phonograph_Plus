/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.text

fun splitMultiTag(source: String): Array<String>? {

    if (source.isEmpty()) return null
    val output: MutableList<String> = arrayListOf()

    source.trim().split(";", "/", "&").forEach {
        output.add(it.trim())
    }

    return output.toTypedArray()
}
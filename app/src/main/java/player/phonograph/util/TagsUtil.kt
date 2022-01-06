/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

object TagsUtil {
    @JvmStatic
    fun parse(source: String): Array<String>? {

        if (source.isEmpty()) return null
        val output: MutableList<String> = arrayListOf()

        source.trim().split(";", "/", "&").forEach {
            output.add(it.trim())
        }

        return output.toTypedArray()
    }
}

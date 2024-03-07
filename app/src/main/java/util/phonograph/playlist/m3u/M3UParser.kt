/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.m3u

import java.io.InputStream
import java.nio.charset.StandardCharsets

object M3UParser {

    fun readForPaths(inputStream: InputStream): List<String> {
        return inputStream.reader(StandardCharsets.UTF_8).use { reader ->
            val pathRegex = Regex("(/.+)*/?.+")
            reader.readLines().filterNot { it.startsWith('#') }.filter { line -> pathRegex.matches(line) }
        }
    }

}
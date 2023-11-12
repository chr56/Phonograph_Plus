/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.releasenote

import util.phonograph.yamlParser
import kotlinx.serialization.decodeFromString
import java.io.File

fun parseReleaseNoteYaml(file: File): ReleaseNote {
    val text = file.readText()
    val releaseNote = yamlParser.decodeFromString<ReleaseNote>(text)
    println(releaseNote)
    return releaseNote
}
/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.releasenote

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.decodeFromString
import java.io.File

private val toml = Toml(
    TomlInputConfig(ignoreUnknownNames = true)
)


fun parseReleaseNoteToml(file: File): ReleaseNote {
    val text = file.readText()
    val releaseNote = toml.decodeFromString<ReleaseNote>(text)
    println(releaseNote)
    return releaseNote
}
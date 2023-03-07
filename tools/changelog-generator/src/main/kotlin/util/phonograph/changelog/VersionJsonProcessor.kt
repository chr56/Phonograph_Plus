/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.VersionJson
import util.phonograph.format.VersionJsonItem
import util.phonograph.parser
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File


private fun ReleaseNoteModel.versionJsonItem(channel: String): VersionJsonItem = VersionJsonItem(
    channel = channel,
    versionName = version,
    versionCode = versionCode,
    date = time,
    link = listOf(
        VersionJsonItem.Link(
            name = "Github Release",
            url = "https://github.com/chr56/Phonograph_Plus/releases/tag/v$version"
        )
    ),
    releaseNote = VersionJsonItem.ReleaseNote(
        zh = generateHTMLNoteMinify(note, "zh"),
        en = generateHTMLNoteMinify(note, "en"),
    )
)

//todo
fun generateVersionJsonItem(model: ReleaseNoteModel, channel: String): String {
    val item = generateVersionJsonItemImpl(model, channel)
    return parser.encodeToString(item)
}

internal fun generateVersionJsonItemImpl(model: ReleaseNoteModel, channel: String): JsonObject {
    val versionJsonItem = model.versionJsonItem(channel)
    return parser.encodeToJsonElement(versionJsonItem) as JsonObject
}


fun parseVersionJson(path: String): VersionJson = parseVersionJson(File(path))

fun parseVersionJson(file: File): VersionJson {
    val raw = file.readText()
    return parser.decodeFromString(raw)
}

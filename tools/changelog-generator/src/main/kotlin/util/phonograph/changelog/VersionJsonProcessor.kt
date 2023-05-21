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


private fun ReleaseNoteModel.versionJsonItem(): VersionJsonItem = VersionJsonItem(
    channel = channel?.name ?: "NA",
    versionName = version,
    versionCode = versionCode,
    date = time,
    link = listOf(
        VersionJsonItem.Link(
            name = "Github Release",
            url = downloadUrl()
        )
    ),
    releaseNote = VersionJsonItem.ReleaseNote(
        zh = generateHTMLNoteMinify(note, "zh"),
        en = generateHTMLNoteMinify(note, "en"),
    )
)

private const val LINK = "https://github.com/chr56/Phonograph_Plus/releases/tag/"

private fun ReleaseNoteModel.downloadUrl(): String = "$LINK${tag(this)}"

fun tag(model: ReleaseNoteModel): String = when (model.channel) {
    ReleaseChannel.PREVIEW -> "preview_${model.version}"
    ReleaseChannel.STABLE  -> "v${model.version}"
    ReleaseChannel.LTS     -> "v${model.version}"
    else                   -> throw IllegalStateException("can not process tag for channel ${model.channel?.name}")
}


fun parseVersionJson(path: String): VersionJson = parseVersionJson(File(path))

fun parseVersionJson(file: File): VersionJson {
    val raw = file.readText()
    return parser.decodeFromString(raw)
}

const val MAX_CHANNEL_ITEM = 3

fun updateVersionJson(versionJson: VersionJson, releaseNoteModel: ReleaseNoteModel): VersionJson {
    // new item
    val newVersionJsonItem = releaseNoteModel.versionJsonItem()

    // with old items
    val allItems = mutableListOf(newVersionJsonItem).also { it.addAll(versionJson.versions) }

    // process
    val items =
        allItems
            .distinctBy { it.date } // distinct by time
            .groupBy { it.channel } // group by channel
            .flatMap { (_, g) ->
                g.sortedByDescending { it.date }.take(MAX_CHANNEL_ITEM) // keep every channel up to [MAX_CHANNEL_ITEM]
            }.sortedByDescending { it.date } // sort by time

    // create new
    val result = VersionJson(items)

    return result
}

fun serializeVersionJson(versionJson: VersionJson): String = parser.encodeToString(versionJson)

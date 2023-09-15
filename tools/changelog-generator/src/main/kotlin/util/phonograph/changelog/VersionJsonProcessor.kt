/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.VersionJson
import util.phonograph.format.VersionJsonItem
import util.phonograph.parser
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.ReleaseNote
import kotlinx.serialization.encodeToString
import java.io.File


private fun ReleaseNote.versionJsonItem(): VersionJsonItem = VersionJsonItem(
    channel = channel.name,
    versionName = version,
    versionCode = versionCode,
    date = timestamp,
    link = listOf(
        VersionJsonItem.Link(
            name = "Github Release",
            url = downloadUrl()
        )
    ),
    releaseNote = VersionJsonItem.ReleaseNote(
        zh = generateHTMLNoteMinify(this, Language.ZH),
        en = generateHTMLNoteMinify(this, Language.EN),
    )
)

private const val LINK = "https://github.com/chr56/Phonograph_Plus/releases/tag/"

private fun ReleaseNote.downloadUrl(): String = "$LINK$tag"


fun parseVersionJson(path: String): VersionJson = parseVersionJson(File(path))

fun parseVersionJson(file: File): VersionJson {
    val raw = file.readText()
    return parser.decodeFromString(raw)
}

const val MAX_CHANNEL_ITEM = 3

fun updateVersionJson(versionJson: VersionJson, ReleaseNote: ReleaseNote): VersionJson {
    // new item
    val newVersionJsonItem = ReleaseNote.versionJsonItem()

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

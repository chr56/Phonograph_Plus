/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

import util.phonograph.changelog.ReleaseNoteModel
import util.phonograph.changelog.generateHTMLNoteMinify
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

private val parser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

private fun ReleaseNoteModel.versionJsonItem(channel: String): VersionJsonItem = VersionJsonItem(
    channel = channel,
    versionName = version,
    versionCode = versionCode,
    date = time,
    link = VersionJsonItem.Link(
        name = "Github Release",
        url = "https://github.com/chr56/Phonograph_Plus/releases/tag/v$version"
    ),
    releaseNote = VersionJsonItem.ReleaseNote(
        zh = generateHTMLNoteMinify(note, "zh"),
        en = generateHTMLNoteMinify(note, "en"),
    )
)

//todo
fun generateVersionJson(model: ReleaseNoteModel, channel: String): String {
    val item = generateVersionItem(model, channel)
    return parser.encodeToString(item)
}

internal fun generateVersionItem(model: ReleaseNoteModel, channel: String): JsonObject {
    val versionJsonItem = model.versionJsonItem(channel)
    return parser.encodeToJsonElement(versionJsonItem) as JsonObject
}

internal const val CHANNEL = "channel"
internal const val VERSION_NAME = "versionName"
internal const val VERSION_CODE = "versionCode"
internal const val DATE = "date"
internal const val LINK = "link"
internal const val LINK_NAME = "name"
internal const val LINK_URI = "uri"
internal const val RELEASE_NOTE = "releaseNote"
internal const val ZH_CN = "zh-cn"
internal const val EN = "en"

@Serializable
class VersionJsonItem(
    @SerialName(CHANNEL) val channel: String,
    @SerialName(VERSION_NAME) val versionName: String,
    @SerialName(VERSION_CODE) val versionCode: Int,
    @SerialName(DATE) val date: Long,
    @SerialName(LINK) val link: Link,
    @SerialName(RELEASE_NOTE) val releaseNote: ReleaseNote,
) {
    @Serializable
    class Link(
        @SerialName(LINK_NAME) val name: String,
        @SerialName(LINK_URI) val url: String,
    )
    @Serializable
    class ReleaseNote(
        @SerialName(ZH_CN) val zh: String,
        @SerialName(EN) val en: String,
    )
}

@Serializable
class VersionJson(
    @SerialName("versions")
    val versions: List<VersionJsonItem>,
)
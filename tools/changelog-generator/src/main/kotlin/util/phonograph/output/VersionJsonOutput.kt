/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.changelog.generateHTMLNoteMinify
import util.phonograph.jsonParser
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.ReleaseNote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.Writer



private const val MAX_CHANNEL_ITEM = 3
private const val GITHUB_LINK = "https://github.com/chr56/Phonograph_Plus/releases/tag/"

class VersionJsonOutput(versionJsonFile: File, private val releaseNote: ReleaseNote) : OutputFormat {

    private val versionJson: VersionJson = jsonParser.decodeFromString(versionJsonFile.readText())

    private fun ReleaseNote.toVersionJsonItem(): VersionJsonItem =
        VersionJsonItem(
            channel = channel.name,
            versionName = version,
            versionCode = versionCode,
            date = timestamp,
            link = listOf(
                VersionJsonItem.Link(
                    name = "Github Release",
                    url = "$GITHUB_LINK$tag"
                )
            ),
            releaseNote = VersionJsonItem.ReleaseNote(
                zh = generateHTMLNoteMinify(this, Language.ZH),
                en = generateHTMLNoteMinify(this, Language.EN),
            )
        )

    private fun updateVersionJson(oldVersionJson: VersionJson): VersionJson {
        // new item
        val newVersionJsonItem = releaseNote.toVersionJsonItem()

        // with old items
        val allItems = mutableListOf(newVersionJsonItem).also { it.addAll(oldVersionJson.versions) }

        // process
        val items =
            allItems
                .distinctBy { it.date } // distinct by time
                .groupBy { it.channel } // group by channel
                .flatMap { (_, g) ->
                    g.sortedByDescending { it.date }
                        .take(MAX_CHANNEL_ITEM) // keep every channel up to [MAX_CHANNEL_ITEM]
                }.sortedByDescending { it.date } // sort by time

        // create new
        return VersionJson(items)
    }


    override fun write(target: Writer) {
        val updatedVersionJson = updateVersionJson(versionJson)
        val string = jsonParser.encodeToString(updatedVersionJson)
        target.write(string)
    }


}


private const val CHANNEL = "channel"
private const val VERSION_NAME = "versionName"
private const val VERSION_CODE = "versionCode"
private const val DATE = "date"
private const val LINK = "link"
private const val LINK_NAME = "name"
private const val LINK_URI = "uri"
private const val RELEASE_NOTE = "releaseNote"
private const val ZH_CN = "zh-cn"
private const val EN = "en"

@Serializable
private class VersionJsonItem(
    @SerialName(CHANNEL) val channel: String,
    @SerialName(VERSION_NAME) val versionName: String,
    @SerialName(VERSION_CODE) val versionCode: Int,
    @SerialName(DATE) val date: Long,
    @SerialName(LINK) val link: List<Link>,
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
private class VersionJson(
    @SerialName("versions")
    val versions: List<VersionJsonItem>,
)
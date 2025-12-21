/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.html.generateHTMLNoteMinify
import util.phonograph.model.Language
import util.phonograph.model.OutputFormat
import util.phonograph.model.ReleaseMetadata
import util.phonograph.model.TargetVariant
import util.phonograph.model.VersionJson
import util.phonograph.model.constants.DOWNLOAD_LINK_GITHUB_HOME_LABEL
import util.phonograph.model.constants.DOWNLOAD_LINK_GITHUB_LEGACY_LABEL
import util.phonograph.model.constants.DOWNLOAD_LINK_GITHUB_MODERN_LABEL
import util.phonograph.model.constants.downloadLink
import util.phonograph.model.constants.releaseLink
import util.phonograph.utils.jsonParser
import java.io.File
import java.io.Writer

class VersionJsonOutput(versionJsonFile: File, private val metadata: ReleaseMetadata) : OutputFormat {

    companion object{
        const val MAX_JSON_ITEM_COUNT = 3
    }

    private val versionJson: VersionJson = jsonParser.decodeFromString(versionJsonFile.readText())

    private fun ReleaseMetadata.toVersionJsonItem(): VersionJson.Item =
        VersionJson.Item(
            channel = channel.favorName.lowercase(),
            versionName = version,
            versionCode = versionCode,
            date = timestamp,
            link = links(),
            releaseNote = VersionJson.Item.ReleaseNote(
                zh = generateHTMLNoteMinify(this, Language.ZH),
                en = generateHTMLNoteMinify(this, Language.EN),
            )
        )

    private fun ReleaseMetadata.links(): List<VersionJson.Item.Link> {
        return listOf(
            VersionJson.Item.Link(
                name = DOWNLOAD_LINK_GITHUB_HOME_LABEL,
                url = releaseLink(tag)
            ),
            VersionJson.Item.Link(
                name = DOWNLOAD_LINK_GITHUB_MODERN_LABEL,
                url = downloadLink(tag, version, variant(TargetVariant.MODERN))
            ),
            VersionJson.Item.Link(
                name = DOWNLOAD_LINK_GITHUB_LEGACY_LABEL,
                url = downloadLink(tag, version, variant(TargetVariant.LEGACY))
            )
        )
    }

    private fun updateVersionJson(oldVersionJson: VersionJson): VersionJson {
        // new item
        val newVersionJsonItem = metadata.toVersionJsonItem()

        // with old items
        val allItems = mutableListOf(newVersionJsonItem).also { it.addAll(oldVersionJson.versions) }

        // process
        val items =
            allItems
                .distinctBy { it.date } // distinct by time
                .groupBy { it.channel } // group by channel
                .flatMap { (_, g) ->
                    g.sortedByDescending { it.date }
                        .take(MAX_JSON_ITEM_COUNT) // keep every channel up to [MAX_CHANNEL_ITEM]
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